package com.example.microfleetapp.engine;

import com.example.microfleetapp.engine.Types.AcoResult;
import com.example.microfleetapp.engine.Types.Batch;
import com.example.microfleetapp.engine.Types.Cap;
import com.example.microfleetapp.engine.Types.Eval;
import com.example.microfleetapp.engine.Types.Ev;
import com.example.microfleetapp.engine.Types.Ord;
import com.example.microfleetapp.engine.Types.Params;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Ant Colony Optimisation for the pickup-and-delivery route of ONE batch: which stop comes next?
 * Ants build routes using pheromone (tau) and a heuristic (eta = nearer + more urgent), respecting
 * "pickup before its drop-off" and rider capacity. Good routes deposit more pheromone.
 */
public final class Aco {

    private static final class Built {
        List<Integer> seq;
        double cost, dur, lateW, dist;
    }

    private final List<Ord> os;
    private final Batch b;
    private final Cap cap;
    private final int startNode;
    private final double startT;
    private final Params ap;
    private final Rng rng;
    private final int m, n;
    private final int[] evO, evNode;
    private final char[] evK;

    private Aco(List<Ord> os, Batch b, Cap cap, int startNode, double startT, Params ap, int seed) {
        this.os = os;
        this.b = b;
        this.cap = cap;
        this.startNode = startNode;
        this.startT = startT;
        this.ap = ap;
        this.rng = new Rng(seed);
        this.m = b.idxs.size();
        this.n = 1 + 2 * m;
        evO = new int[n];
        evNode = new int[n];
        evK = new char[n];
        evO[0] = -1;
        evNode[0] = startNode;
        evK[0] = 'O';
        for (int q = 0; q < m; q++) {
            int oi = b.idxs.get(q);
            evO[1 + 2 * q] = oi;
            evK[1 + 2 * q] = 'P';
            evNode[1 + 2 * q] = os.get(oi).pickup;
            evO[2 + 2 * q] = oi;
            evK[2 + 2 * q] = 'D';
            evNode[2 + 2 * q] = os.get(oi).drop;
        }
    }

    public static AcoResult run(List<Ord> os, Batch b, Cap cap, int startNode, double startT, Params ap, int seed) {
        return new Aco(os, b, cap, startNode, startT, ap, seed).execute();
    }

    private List<Ev> evsOf(List<Integer> seq) {
        List<Ev> out = new ArrayList<Ev>(seq.size());
        for (int j : seq) {
            out.add(new Ev(evK[j], evO[j]));
        }
        return out;
    }

    private Built construct(double[] tau, boolean greedy) {
        boolean[] vis = new boolean[n];
        vis[0] = true;
        int cur = 0;
        double t = startT, w = 0;
        int pk = 0;
        List<Integer> seq = new ArrayList<Integer>();
        for (int step = 0; step < 2 * m; step++) {
            int cnt = 0;
            int[] js = new int[n];
            double[] taus = new double[n], etas = new double[n], trs = new double[n], waits = new double[n];
            for (int j = 1; j < n; j++) {
                if (vis[j]) {
                    continue;
                }
                boolean isP = (j % 2 == 1);
                if (!isP && !vis[j - 1]) {
                    continue;
                }
                Ord o = os.get(evO[j]);
                if (isP && (w + o.kg > cap.kg + 1e-9 || pk + o.pk > cap.pk)) {
                    continue;
                }
                double tr = Road.T[evNode[cur]][evNode[j]];
                double wait = isP ? Math.max(0, o.ready - (t + tr)) : 0;
                js[cnt] = j;
                taus[cnt] = tau[cur * n + j];
                etas[cnt] = 1 / (1 + tr + wait) * (1 + o.u);
                trs[cnt] = tr;
                waits[cnt] = wait;
                cnt++;
            }
            if (cnt == 0) {
                return null;
            }
            int pick;
            if (greedy) {
                int a = 0;
                for (int c = 1; c < cnt; c++) {
                    if (!(trs[a] + waits[a] <= trs[c] + waits[c])) {
                        a = c;
                    }
                }
                pick = js[a];
            } else {
                double[] wts = new double[cnt];
                double tot = 0;
                for (int c = 0; c < cnt; c++) {
                    wts[c] = Math.pow(taus[c], ap.alpha) * Math.pow(etas[c], ap.beta);
                }
                for (int c = 0; c < cnt; c++) {
                    tot += wts[c];
                }
                double r = rng.next() * tot;
                int idx = 0;
                for (; idx < cnt - 1; idx++) {
                    r -= wts[idx];
                    if (r <= 0) {
                        break;
                    }
                }
                pick = js[idx];
            }
            Ord o = os.get(evO[pick]);
            t += Road.T[evNode[cur]][evNode[pick]];
            if (pick % 2 == 1) {
                t = Math.max(t, o.ready);
                w += o.kg;
                pk += o.pk;
            } else {
                w -= o.kg;
                pk -= o.pk;
            }
            t += Core.SERVICE;
            vis[pick] = true;
            seq.add(pick);
            cur = pick;
        }
        Eval r = Core.evalSeq(os, startNode, startT, evsOf(seq), cap, false);
        Built out = new Built();
        out.seq = seq;
        out.cost = r.duration + Core.LAMBDA * r.lateW;
        out.dur = r.duration;
        out.lateW = r.lateW;
        out.dist = r.dist;
        return out;
    }

    private AcoResult execute() {
        double[] tau = new double[n * n];
        Arrays.fill(tau, 1.0);
        Built greedy = construct(tau, true);
        if (greedy == null) {
            return null;
        }
        double Q = greedy.cost;
        Built best = greedy;
        for (int it = 0; it < ap.iters; it++) {
            List<Built> ants = new ArrayList<Built>();
            for (int a = 0; a < ap.ants; a++) {
                Built r = construct(tau, false);
                if (r != null) {
                    ants.add(r);
                }
            }
            Collections.sort(ants, new Comparator<Built>() {
                public int compare(Built x, Built y) {
                    double d = x.cost - y.cost;
                    return d < 0 ? -1 : d > 0 ? 1 : 0;
                }
            });
            Built ib = ants.isEmpty() ? best : ants.get(0);
            if (ib.cost < best.cost) {
                best = ib;
            }
            for (int i = 0; i < tau.length; i++) {
                tau[i] = Rng.clamp(tau[i] * (1 - ap.rho), 0.02, 30);
            }
            deposit(tau, ib.seq, Q / ib.cost);
            deposit(tau, best.seq, 2 * Q / best.cost);
        }
        AcoResult res = new AcoResult();
        res.evs = new ArrayList<Ev>();
        for (int j = 0; j < n; j++) {
            res.evs.add(new Ev(evK[j], evO[j]));
        }
        res.evNode = evNode;
        res.bestSeq = best.seq;
        res.bestCost = best.cost;
        res.greedyCost = greedy.cost;
        res.greedyDist = greedy.dist;
        res.bestDist = best.dist;
        res.detail = Core.evalSeq(os, startNode, startT, evsOf(best.seq), cap, true);
        res.gDetail = Core.evalSeq(os, startNode, startT, evsOf(greedy.seq), cap, true);
        res.startNode = startNode;
        res.startT = startT;
        return res;
    }

    private void deposit(double[] tau, List<Integer> sq, double amt) {
        int prev = 0;
        for (int j : sq) {
            tau[prev * n + j] = Math.min(30, tau[prev * n + j] + amt);
            prev = j;
        }
    }
}
