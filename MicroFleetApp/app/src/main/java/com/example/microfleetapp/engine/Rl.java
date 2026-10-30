package com.example.microfleetapp.engine;

import com.example.microfleetapp.engine.Types.Batch;
import com.example.microfleetapp.engine.Types.Cap;
import com.example.microfleetapp.engine.Types.Eval;
import com.example.microfleetapp.engine.Types.Ev;
import com.example.microfleetapp.engine.Types.Ord;
import com.example.microfleetapp.engine.Types.RiderState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Reinforcement learning for rider assignment: a masked softmax policy over the candidate riders
 * (plus a "defer" action), trained with REINFORCE and a moving-average baseline on randomly
 * generated dispatch episodes. State features per (batch, rider): reach time, route km, weighted
 * lateness, rider backlog, rider workload and finish time.
 */
public final class Rl {

    public static final int NF = 6;

    private Rl() { }

    public interface RouteFn {
        Eval route(List<Ord> os, Batch b, int ri, RiderState r, double nowT);
    }

    public static final class Cand {
        public int i;
        public String status; // ok | capacity | topk
        public Eval route;
        public double[] feats;
        public double reward;
    }

    public static final class Choice {
        public int pick = -1;
        public double[] probs;
        public int a = -1;
    }

    public static double rewardOf(Eval r, RiderState rs, double nowT) {
        return 10 - 0.35 * r.dist - 0.30 * r.lateW - 0.05 * (r.finish - nowT) - 1.0 * rs.count;
    }

    public static double[] featsOf(Eval r, RiderState rs, double nowT) {
        double startT = Math.max(rs.avail, nowT);
        return new double[]{(r.firstArr - startT) / 30, r.dist / 15, r.lateW / 30,
                Math.max(0, rs.avail - nowT) / 60, rs.count / 3.0, (r.finish - nowT) / 90};
    }

    /** Quick route used while training: cheapest insertion instead of a full ACO run. */
    public static final RouteFn FAST_ROUTE = new RouteFn() {
        public Eval route(List<Ord> os, Batch b, int ri, RiderState rs, double nowT) {
            double st = Math.max(rs.avail, nowT);
            List<Ev> seq = Core.cheapestSeq(os, b.idxs, rs.node, st);
            return Core.evalSeq(os, rs.node, st, seq, rs.cap, true);
        }
    };

    public static List<Cand> buildCandidates(final List<Ord> os, final Batch b, final List<RiderState> rs, int K,
                                             RouteFn routeFn, double nowT) {
        List<Cand> out = new ArrayList<Cand>();
        for (int i = 0; i < rs.size(); i++) {
            Cand c = new Cand();
            c.i = i;
            RiderState r = rs.get(i);
            c.status = (b.kg > r.cap.kg || b.pk > r.cap.pk) ? "capacity" : "ok";
            out.add(c);
        }
        final double now = nowT;
        List<Cand> ok = new ArrayList<Cand>();
        for (Cand c : out) {
            if (c.status.equals("ok")) {
                ok.add(c);
            }
        }
        final List<RiderState> fr = rs;
        Collections.sort(ok, new Comparator<Cand>() {
            private double proxy(Cand c) {
                double m = Double.POSITIVE_INFINITY;
                for (int o : b.idxs) {
                    m = Math.min(m, Road.T[fr.get(c.i).node][os.get(o).pickup]);
                }
                return m + Math.max(0, fr.get(c.i).avail - now);
            }

            public int compare(Cand a, Cand c) {
                double d = proxy(a) - proxy(c);
                return d < 0 ? -1 : d > 0 ? 1 : 0;
            }
        });
        for (int idx = 0; idx < ok.size(); idx++) {
            if (idx >= K) {
                ok.get(idx).status = "topk";
            }
        }
        for (Cand c : out) {
            if (!c.status.equals("ok")) {
                continue;
            }
            RiderState r = rs.get(c.i);
            c.route = routeFn.route(os, b, c.i, r, nowT);
            c.feats = featsOf(c.route, r, nowT);
            c.reward = rewardOf(c.route, r, nowT);
        }
        return out;
    }

    public static List<Cand> validOf(List<Cand> cands) {
        List<Cand> v = new ArrayList<Cand>();
        for (Cand c : cands) {
            if (c.status.equals("ok")) {
                v.add(c);
            }
        }
        return v;
    }

    /** Softmax over valid riders; the last entry is the probability of deferring the batch. */
    public static double[] policyProbs(double[] w, List<Cand> valid) {
        double[] z = new double[valid.size() + 1];
        for (int v = 0; v < valid.size(); v++) {
            double s = 0;
            for (int k = 0; k < NF; k++) {
                s += w[k] * valid.get(v).feats[k];
            }
            z[v] = s;
        }
        z[valid.size()] = w[NF];
        double mx = z[0];
        for (double q : z) {
            mx = Math.max(mx, q);
        }
        double[] e = new double[z.length];
        double t = 0;
        for (int i = 0; i < z.length; i++) {
            e[i] = Math.exp(z[i] - mx);
        }
        for (double q : e) {
            t += q;
        }
        for (int i = 0; i < e.length; i++) {
            e[i] = e[i] / t;
        }
        return e;
    }

    public static Choice choose(String policy, List<Cand> valid, double[] w, Rng rng, boolean greedy) {
        Choice ch = new Choice();
        if (valid.isEmpty()) {
            return ch;
        }
        if (policy.equals("rl")) {
            double[] p = policyProbs(w, valid);
            int a;
            if (greedy) {
                a = 0;
                for (int i = 1; i < p.length; i++) {
                    if (p[i] > p[a]) {
                        a = i;
                    }
                }
            } else {
                double r = rng.next();
                int i = 0;
                for (; i < p.length - 1; i++) {
                    r -= p[i];
                    if (r <= 0) {
                        break;
                    }
                }
                a = i;
            }
            ch.probs = p;
            ch.a = a;
            ch.pick = a < valid.size() ? valid.get(a).i : -1;
            return ch;
        }
        if (policy.equals("nearest")) {
            Cand a = valid.get(0);
            for (int i = 1; i < valid.size(); i++) {
                Cand c = valid.get(i);
                if (c.feats[0] < a.feats[0]) {
                    a = c;
                }
            }
            ch.pick = a.i;
            return ch;
        }
        if (policy.equals("least")) {
            Cand a = valid.get(0);
            for (int i = 1; i < valid.size(); i++) {
                Cand c = valid.get(i);
                double kc = c.feats[3] + c.feats[4] * 0.01 + c.feats[0] * 0.001;
                double ka = a.feats[3] + a.feats[4] * 0.01 + a.feats[0] * 0.001;
                if (kc < ka) {
                    a = c;
                }
            }
            ch.pick = a.i;
            return ch;
        }
        ch.pick = valid.get(rng.nextInt(valid.size())).i;
        return ch;
    }

    // ------------------------------------------------------------------ training

    private static final class Episode {
        List<Ord> os;
        List<Types.Cap> caps;
        List<Integer> nodes;
        List<Batch> batches;
    }

    private static Episode randomEpisode(int seed) {
        Rng rng = new Rng(seed);
        int nO = 6 + rng.nextInt(4);
        Episode ep = new Episode();
        ep.os = new ArrayList<Ord>();
        List<Scenario.Spec> specs = Scenario.genOrderSpecs(rng, nO);
        for (int i = 0; i < specs.size(); i++) {
            ep.os.add(Scenario.buildOrder(specs.get(i), i, 41));
        }
        ep.caps = new ArrayList<Cap>();
        ep.nodes = new ArrayList<Integer>();
        for (Scenario.RiderSpec r : Scenario.genRiderSpecs(rng, 4)) {
            ep.caps.add(new Cap(r.kg, r.pk));
            ep.nodes.add(r.node);
        }
        List<Integer> ord = new ArrayList<Integer>();
        for (int i = 0; i < ep.os.size(); i++) {
            ord.add(i);
        }
        rng.shuffle(ord);
        ep.batches = new ArrayList<Batch>();
        while (!ord.isEmpty()) {
            int k = Math.min(ord.size(), 1 + rng.nextInt(3));
            List<Integer> idxs = new ArrayList<Integer>(ord.subList(0, k));
            ord.subList(0, k).clear();
            Batch b = new Batch();
            b.idxs = idxs;
            double kg = 0, umax = 0;
            int pk = 0;
            boolean first = true;
            for (int i : idxs) {
                kg += ep.os.get(i).kg;
                pk += ep.os.get(i).pk;
                umax = first ? ep.os.get(i).u : Math.max(umax, ep.os.get(i).u);
                first = false;
            }
            b.kg = kg;
            b.pk = pk;
            b.umax = umax;
            ep.batches.add(b);
        }
        Collections.sort(ep.batches, new Comparator<Batch>() {
            public int compare(Batch a, Batch b) {
                double d = b.umax - a.umax;
                return d < 0 ? -1 : d > 0 ? 1 : 0;
            }
        });
        return ep;
    }

    private static final class Step {
        double[][] feats;
        double[] probs;
        int a;
        double reward;
    }

    private static final class EpResult {
        double total, dist, lateW;
        int assigned, nb;
        List<Step> steps = new ArrayList<Step>();
    }

    private static EpResult playEpisode(Episode ep, String policy, double[] w, Rng sampleRng, boolean greedy, boolean record) {
        List<RiderState> rs = new ArrayList<RiderState>();
        for (int i = 0; i < ep.caps.size(); i++) {
            rs.add(new RiderState(i, ep.nodes.get(i), 0, 0, ep.caps.get(i)));
        }
        EpResult res = new EpResult();
        for (Batch b : ep.batches) {
            List<Cand> cands = buildCandidates(ep.os, b, rs, 3, FAST_ROUTE, 0);
            List<Cand> valid = validOf(cands);
            Choice ch = choose(policy, valid, w, sampleRng, greedy);
            double reward = 0;
            if (valid.isEmpty()) {
                reward = 0;
            } else if (ch.pick < 0) {
                reward = -Core.deferPenalty(b);
            } else {
                Cand c = cands.get(ch.pick);
                reward = c.reward;
                RiderState r = rs.get(ch.pick);
                res.dist += c.route.dist;
                res.lateW += c.route.lateW;
                res.assigned++;
                r.avail = c.route.finish;
                r.node = c.route.endNode;
                r.count++;
            }
            res.total += reward;
            if (record && !valid.isEmpty()) {
                Step st = new Step();
                st.feats = new double[valid.size()][];
                for (int v = 0; v < valid.size(); v++) {
                    st.feats[v] = valid.get(v).feats;
                }
                st.probs = ch.probs;
                st.a = ch.a;
                st.reward = reward;
                res.steps.add(st);
            }
        }
        res.nb = ep.batches.size();
        return res;
    }

    /** Trains the policy from scratch; returns the NF+1 weights (last one = "defer" bias). */
    public static double[] train(int seed, int episodes, double lr) {
        double[] w = new double[NF + 1];
        double[] base = new double[16];
        Rng rng = new Rng(seed * 31 + 7);
        for (int e = 0; e < episodes; e++) {
            Episode ep = randomEpisode(100000 + seed * 10000 + e);
            EpResult res = playEpisode(ep, "rl", w, rng, false, true);
            int T_ = res.steps.size();
            double[] G = new double[T_];
            double acc = 0;
            for (int t = T_ - 1; t >= 0; t--) {
                acc += res.steps.get(t).reward;
                G[t] = acc;
            }
            double[] grad = new double[NF + 1];
            for (int t = 0; t < T_; t++) {
                Step st = res.steps.get(t);
                double adv = G[t] - base[t];
                base[t] = 0.97 * base[t] + 0.03 * G[t];
                int V = st.feats.length;
                for (int v = 0; v < V; v++) {
                    double coef = ((st.a == v ? 1 : 0) - st.probs[v]) * adv;
                    for (int k = 0; k < NF; k++) {
                        grad[k] += coef * st.feats[v][k];
                    }
                }
                grad[NF] += ((st.a == V ? 1 : 0) - st.probs[V]) * adv;
            }
            double gn = 0;
            for (double g : grad) {
                gn += g * g;
            }
            gn = Math.sqrt(gn);
            double sc = gn > 8 ? 8 / gn : 1;
            for (int k = 0; k <= NF; k++) {
                w[k] += lr * sc * grad[k];
            }
        }
        return w;
    }

    public static final class PolicyRow {
        public String name, key;
        public double mean, std, dist, late, asg;
    }

    /** Compares the learned policy with simple rules on {@code count} unseen random episodes. */
    public static List<PolicyRow> evaluatePolicies(double[] w, int count, int seed0) {
        String[][] defs = {{"RL policy", "rl"}, {"Nearest rider", "nearest"}, {"Least-loaded rider", "least"},
                {"Random feasible", "random"}};
        List<PolicyRow> rows = new ArrayList<PolicyRow>();
        for (String[] d : defs) {
            double[] tot = new double[count], dist = new double[count], late = new double[count], asg = new double[count];
            for (int s = 0; s < count; s++) {
                Episode ep = randomEpisode(seed0 + s);
                EpResult r = playEpisode(ep, d[1], w, new Rng(seed0 * 5 + s), true, false);
                tot[s] = r.total;
                dist[s] = r.dist;
                late[s] = r.lateW;
                asg[s] = 100.0 * r.assigned / r.nb;
            }
            PolicyRow row = new PolicyRow();
            row.name = d[0];
            row.key = d[1];
            row.mean = Core.mean(tot);
            row.std = Core.std(tot);
            row.dist = Core.mean(dist);
            row.late = Core.mean(late);
            row.asg = Core.mean(asg);
            rows.add(row);
        }
        return rows;
    }
}
