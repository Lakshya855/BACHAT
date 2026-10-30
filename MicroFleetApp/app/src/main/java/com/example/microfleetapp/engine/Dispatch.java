package com.example.microfleetapp.engine;

import com.example.microfleetapp.engine.Types.AcoResult;
import com.example.microfleetapp.engine.Types.Batch;
import com.example.microfleetapp.engine.Types.Cap;
import com.example.microfleetapp.engine.Types.Eval;
import com.example.microfleetapp.engine.Types.Ev;
import com.example.microfleetapp.engine.Types.Ord;
import com.example.microfleetapp.engine.Types.Params;
import com.example.microfleetapp.engine.Types.RiderState;
import com.example.microfleetapp.engine.Types.RouteRec;
import com.example.microfleetapp.engine.Types.Stop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The full MicroFleet decision pipeline for one dispatch round:
 * orders (already fuzzy-scored) -> GA pooling -> ACO routing -> RL rider assignment.
 */
public final class Dispatch {

    private Dispatch() { }

    public interface Progress {
        void stage(String message);
    }

    /** A rider as the engine needs to know it. */
    public static final class RiderIn {
        public final int ref;          // caller's own index for this rider
        public final String name;
        public final int node;
        public final double availMin;  // minutes until the rider is free
        public final int workload;     // batches currently queued
        public final double capKg;
        public final int capPk;

        public RiderIn(int ref, String name, int node, double availMin, int workload, double capKg, int capPk) {
            this.ref = ref;
            this.name = name;
            this.node = node;
            this.availMin = availMin;
            this.workload = workload;
            this.capKg = capKg;
            this.capPk = capPk;
        }
    }

    /** What the RL policy considered for one rider. */
    public static final class RiderOption {
        public int riderPos;       // position in the engine's rider list
        public String status;      // ok | capacity | topk
        public double km, finishMin, lateMin, reward, prob = -1;
        public boolean chosen;
    }

    public static final class PlannedBatch {
        public Batch batch;
        public int riderPos = -1;          // -1 = not assigned this round
        public String deferReason;
        public Eval route;                 // final ACO route (null when not assigned)
        public AcoResult aco;
        public double soloKm;              // what these orders would cost as separate trips
        public double deferProb = -1;
        public final List<RiderOption> options = new ArrayList<RiderOption>();
    }

    public static final class Plan {
        public final List<Ord> orders = new ArrayList<Ord>();
        public final List<RiderIn> riders = new ArrayList<RiderIn>();
        public final List<PlannedBatch> batches = new ArrayList<PlannedBatch>();
        public double pooledKm, baselineKm, gaCost, gaSinglesCost, onTimePct;
        public int trips, orderCount, assignedOrders, deferredOrders, lateCount, deliveries;
    }

    private static String cacheKey(Batch b, int ri, RiderState rs, double st, Params p) {
        return b.id + "|" + ri + "|" + rs.node + "|" + String.format(Locale.ROOT, "%.3f", st) + "|" + p.ants + "|"
                + p.iters + "|" + Rng.num(p.alpha) + "|" + Rng.num(p.beta) + "|" + Rng.num(p.rho);
    }

    /** ACO route function with a cache so each (batch, rider state) is only solved once. */
    private static Rl.RouteFn acoRoute(final Params p, final int seed) {
        final Map<String, AcoResult> cache = new HashMap<String, AcoResult>();
        return new Rl.RouteFn() {
            public Eval route(List<Ord> os, Batch b, int ri, RiderState rs, double nowT) {
                double st = Math.max(rs.avail, nowT);
                String key = cacheKey(b, ri, rs, st, p);
                AcoResult res = cache.get(key);
                if (res == null) {
                    res = Aco.run(os, b, rs.cap, rs.node, st, p, (int) (Rng.hashStr(key) + seed));
                    if (res == null) { // ACO could not construct a route (should not happen): fall back
                        List<Ev> seq = Core.cheapestSeq(os, b.idxs, rs.node, st);
                        res = new AcoResult();
                        res.detail = Core.evalSeq(os, rs.node, st, seq, rs.cap, true);
                        res.fallback = true;
                    }
                    cache.put(key, res);
                }
                Eval copy = shallowCopy(res.detail);
                copy.aco = res;
                return copy;
            }
        };
    }

    private static Eval shallowCopy(Eval e) {
        Eval c = new Eval();
        c.finish = e.finish;
        c.dist = e.dist;
        c.late = e.late;
        c.lateW = e.lateW;
        c.duration = e.duration;
        c.firstArr = e.firstArr;
        c.startT = e.startT;
        c.viol = e.viol;
        c.endNode = e.endNode;
        c.startNode = e.startNode;
        c.stops = e.stops;
        return c;
    }

    /** Distance if every order were delivered separately by its nearest capable rider. */
    public static double baselineKm(List<Ord> os, List<RiderIn> riders) {
        double km = 0;
        for (Ord o : os) {
            double bd = Double.POSITIVE_INFINITY;
            boolean found = false;
            for (RiderIn r : riders) {
                if (o.kg > r.capKg || o.pk > r.capPk) {
                    continue;
                }
                double d = Road.D[r.node][o.pickup];
                if (d < bd) {
                    bd = d;
                    found = true;
                }
            }
            if (!found) {
                for (RiderIn r : riders) {
                    bd = Math.min(bd, Road.D[r.node][o.pickup]);
                }
            }
            km += bd + Road.D[o.pickup][o.drop];
        }
        return km;
    }

    public static Plan plan(List<Ord> orders, List<RiderIn> riders, Params p, double[] rlWeights, int seed, Progress progress) {
        Plan plan = new Plan();
        plan.orders.addAll(orders);
        plan.riders.addAll(riders);
        plan.orderCount = orders.size();
        if (orders.isEmpty() || riders.isEmpty()) {
            return plan;
        }
        Core.Ctx ctx = new Core.Ctx(orders);
        List<Cap> caps = new ArrayList<Cap>();
        List<RiderState> rs = new ArrayList<RiderState>();
        for (int i = 0; i < riders.size(); i++) {
            RiderIn r = riders.get(i);
            Cap c = new Cap(r.capKg, r.capPk);
            caps.add(c);
            rs.add(new RiderState(i, r.node, r.availMin, r.workload, c));
        }
        Cap maxCap = Core.maxCapOf(caps);

        if (progress != null) {
            progress.stage("GA: pooling " + orders.size() + " orders into shared trips");
        }
        Ga.Result ga = Ga.run(ctx, maxCap, p, seed * 101 + 5);
        plan.gaCost = ga.bestCost;
        plan.gaSinglesCost = ga.singlesCost;
        List<Batch> batches = Ga.makeBatches(ctx, rs, ga.best, maxCap, p.maxBatch);

        if (progress != null) {
            progress.stage("ACO + RL: routing " + batches.size() + " trips and choosing riders");
        }
        Rl.RouteFn route = acoRoute(p, seed);
        Rng rng = new Rng(seed * 17 + (int) Rng.hashStr("rl"));
        for (Batch b : batches) {
            PlannedBatch pb = new PlannedBatch();
            pb.batch = b;
            pb.soloKm = soloKm(orders, b, riders);
            List<Rl.Cand> cands = Rl.buildCandidates(orders, b, rs, p.maxCand, route, 0);
            List<Rl.Cand> valid = Rl.validOf(cands);
            Rl.Choice ch = Rl.choose("rl", valid, rlWeights, rng, true);
            for (Rl.Cand c : cands) {
                RiderOption op = new RiderOption();
                op.riderPos = c.i;
                op.status = c.status;
                if (c.route != null) {
                    op.km = c.route.dist;
                    op.finishMin = c.route.finish;
                    op.lateMin = c.route.late;
                    op.reward = c.reward;
                }
                pb.options.add(op);
            }
            if (ch.probs != null) {
                for (int k = 0; k < valid.size(); k++) {
                    pb.options.get(valid.get(k).i).prob = ch.probs[k];
                }
                pb.deferProb = ch.probs[valid.size()];
            }
            if (ch.pick >= 0) {
                Rl.Cand c = cands.get(ch.pick);
                RiderState r = rs.get(ch.pick);
                pb.riderPos = ch.pick;
                pb.route = c.route;
                pb.aco = c.route.aco;
                pb.options.get(ch.pick).chosen = true;
                RouteRec rec = new RouteRec();
                rec.batch = b;
                rec.detail = c.route;
                rec.startT = Math.max(r.avail, 0);
                rec.startNode = r.node;
                r.routes.add(rec);
                r.avail = c.route.finish;
                r.node = c.route.endNode;
                r.count++;
                r.km += c.route.dist;
            } else if (b.eligible.isEmpty()) {
                pb.deferReason = "No on-duty rider has enough capacity for this trip";
            } else {
                pb.deferReason = "The RL policy held this trip back (riders too busy right now)";
            }
            plan.batches.add(pb);
        }

        plan.baselineKm = baselineKm(orders, riders);
        int lateN = 0, deliv = 0;
        for (PlannedBatch pb : plan.batches) {
            if (pb.riderPos < 0) {
                plan.deferredOrders += pb.batch.idxs.size();
                continue;
            }
            plan.trips++;
            plan.assignedOrders += pb.batch.idxs.size();
            plan.pooledKm += pb.route.dist;
            for (Stop s : pb.route.stops) {
                if (s.ev.k == 'D') {
                    deliv++;
                    if (s.late > 0.01) {
                        lateN++;
                    }
                }
            }
        }
        plan.deliveries = deliv;
        plan.lateCount = lateN;
        plan.onTimePct = deliv > 0 ? 100.0 * (deliv - lateN) / deliv : 0;
        return plan;
    }

    private static double soloKm(List<Ord> os, Batch b, List<RiderIn> riders) {
        List<Ord> mine = new ArrayList<Ord>();
        for (int i : b.idxs) {
            mine.add(os.get(i));
        }
        return baselineKm(mine, riders);
    }
}
