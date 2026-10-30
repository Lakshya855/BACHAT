package com.example.microfleetapp.engine;

import com.example.microfleetapp.engine.Types.Batch;
import com.example.microfleetapp.engine.Types.BatchInfo;
import com.example.microfleetapp.engine.Types.Cap;
import com.example.microfleetapp.engine.Types.Ord;
import com.example.microfleetapp.engine.Types.Params;
import com.example.microfleetapp.engine.Types.RiderState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Memetic Genetic Algorithm that decides WHICH ORDERS RIDE TOGETHER. A chromosome is a partition
 * of the order pool into batches; fitness is total distance + weighted lateness + a fixed cost per
 * trip + capacity penalties (lower is better).
 */
public final class Ga {

    public static final class Result {
        public List<List<Integer>> best;
        public double bestCost, singlesCost;
        public int generations;
    }

    private static final class Ind {
        List<List<Integer>> gs;
        double f;

        Ind(List<List<Integer>> gs) {
            this.gs = gs;
        }
    }

    /**
     * The original simulator drew tournament parents from the STARTING population every
     * generation. The app breeds from the current population (the textbook GA). Only the
     * parity test against the simulator switches this on.
     */
    public static boolean LEGACY_PARENT_POOL = false;

    private final Core.Ctx ctx;
    private final List<Ord> os;
    private final Cap maxCap;
    private final Params gp;
    private final Rng rng;
    private final int n, maxSz;

    private Ga(Core.Ctx ctx, Cap maxCap, Params gp, int seed) {
        this.ctx = ctx;
        this.os = ctx.os;
        this.maxCap = maxCap;
        this.gp = gp;
        this.rng = new Rng(seed);
        this.n = os.size();
        this.maxSz = gp.maxBatch;
    }

    public static Result run(Core.Ctx ctx, Cap maxCap, Params gp, int seed) {
        return new Ga(ctx, maxCap, gp, seed).execute();
    }

    private double cost(List<List<Integer>> gs) {
        double s = 0;
        for (List<Integer> a : gs) {
            s += Core.batchInfo(ctx, a, maxCap, maxSz).cost;
        }
        return s;
    }

    private static List<List<Integer>> clone(List<List<Integer>> gs) {
        List<List<Integer>> out = new ArrayList<List<Integer>>(gs.size());
        for (List<Integer> a : gs) {
            out.add(new ArrayList<Integer>(a));
        }
        return out;
    }

    /** Sorts groups, drops empty ones and splits any group larger than the batch limit. */
    private List<List<Integer>> repair(List<List<Integer>> gs) {
        List<List<Integer>> out = new ArrayList<List<Integer>>();
        for (List<Integer> a : gs) {
            if (a.isEmpty()) {
                continue;
            }
            Collections.sort(a);
            if (a.size() <= maxSz) {
                out.add(a);
            } else {
                List<Integer> s = rng.shuffle(new ArrayList<Integer>(a));
                while (!s.isEmpty()) {
                    int take = Math.min(maxSz, s.size());
                    List<Integer> part = new ArrayList<Integer>(s.subList(0, take));
                    s.subList(0, take).clear();
                    Collections.sort(part);
                    out.add(part);
                }
            }
        }
        return out;
    }

    private Ind randomInd() {
        int base = Math.max(1, (int) Math.ceil((double) n / maxSz));
        int k = base + (int) Math.floor(rng.next() * (n - (int) Math.ceil((double) n / maxSz) + 1));
        List<List<Integer>> gs = new ArrayList<List<Integer>>();
        for (int i = 0; i < k; i++) {
            gs.add(new ArrayList<Integer>());
        }
        for (int i = 0; i < n; i++) {
            gs.get((int) Math.floor(rng.next() * k)).add(i);
        }
        return new Ind(repair(gs));
    }

    private double meanOf(List<Double> v) {
        double s = 0;
        for (double x : v) {
            s += x;
        }
        return s / v.size();
    }

    private int nearestGroup(List<List<Integer>> g, int o, int skip) {
        int bi = -1;
        double bd = 1e9;
        for (int k = 0; k < g.size(); k++) {
            List<Integer> a = g.get(k);
            if (k == skip || a.isEmpty()) {
                continue;
            }
            List<Double> ds = new ArrayList<Double>();
            for (int i : a) {
                ds.add(Road.T[os.get(o).pickup][os.get(i).pickup] + Road.T[os.get(o).drop][os.get(i).drop]);
            }
            double d = meanOf(ds);
            if (d < bd) {
                bd = d;
                bi = k;
            }
        }
        return bi;
    }

    private Ind tour(List<Ind> pop) {
        Ind b = null;
        for (int i = 0; i < 3; i++) {
            Ind c = pop.get((int) Math.floor(rng.next() * pop.size()));
            if (b == null || c.f < b.f) {
                b = c;
            }
        }
        return b;
    }

    private List<List<Integer>> crossover(List<List<Integer>> a, List<List<Integer>> b) {
        int nGroups = 1 + (rng.next() < .5 ? 1 : 0);
        List<Integer> keys = new ArrayList<Integer>();
        for (int i = 0; i < a.size(); i++) {
            keys.add(i);
        }
        rng.shuffle(keys);
        int take = Math.min(nGroups, a.size());
        List<List<Integer>> child = new ArrayList<List<Integer>>();
        java.util.HashSet<Integer> taken = new java.util.HashSet<Integer>();
        for (int i = 0; i < take; i++) {
            List<Integer> g = new ArrayList<Integer>(a.get(keys.get(i)));
            child.add(g);
            taken.addAll(g);
        }
        for (List<Integer> g : b) {
            List<Integer> rest = new ArrayList<Integer>();
            for (int i : g) {
                if (!taken.contains(i)) {
                    rest.add(i);
                }
            }
            if (!rest.isEmpty()) {
                child.add(rest);
                taken.addAll(rest);
            }
        }
        return child;
    }

    private int distinctOther(int len, int not) {
        int j;
        do {
            j = (int) Math.floor(rng.next() * len);
        } while (j == not);
        return j;
    }

    private List<List<Integer>> mutate(List<List<Integer>> gs) {
        List<List<Integer>> g = clone(gs);
        double r = rng.next();
        if (r < .4 || g.size() == 1) {
            int src = (int) Math.floor(rng.next() * g.size());
            List<Integer> a = g.get(src);
            int o = a.remove((int) Math.floor(rng.next() * a.size()));
            if (g.size() == 1 || rng.next() < .2) {
                List<Integer> single = new ArrayList<Integer>();
                single.add(o);
                g.add(single);
            } else {
                int t = rng.next() < .5 ? nearestGroup(g, o, src) : -1;
                if (t < 0) {
                    t = distinctOther(g.size(), src);
                }
                g.get(t).add(o);
            }
        } else if (r < .62) {
            int i = (int) Math.floor(rng.next() * g.size());
            int j = distinctOther(g.size(), i);
            List<Integer> merged = new ArrayList<Integer>(g.get(i));
            merged.addAll(g.get(j));
            g.set(i, merged);
            g.remove(j);
        } else if (r < .82) {
            List<Integer> cand = new ArrayList<Integer>();
            for (int k = 0; k < g.size(); k++) {
                if (g.get(k).size() >= 2) {
                    cand.add(k);
                }
            }
            if (!cand.isEmpty()) {
                int k = cand.get((int) Math.floor(rng.next() * cand.size()));
                List<Integer> a = rng.shuffle(new ArrayList<Integer>(g.get(k)));
                int cut = 1 + (int) Math.floor(rng.next() * (a.size() - 1));
                g.set(k, new ArrayList<Integer>(a.subList(0, cut)));
                g.add(new ArrayList<Integer>(a.subList(cut, a.size())));
            }
        } else {
            int i = (int) Math.floor(rng.next() * g.size());
            int j = distinctOther(g.size(), i);
            int x = (int) Math.floor(rng.next() * g.get(i).size());
            int y = (int) Math.floor(rng.next() * g.get(j).size());
            int tmp = g.get(i).get(x);
            g.get(i).set(x, g.get(j).get(y));
            g.get(j).set(y, tmp);
        }
        return repair(g);
    }

    /** Memetic step: try moving one random order to its best group (cheap local search). */
    private List<List<Integer>> improve(List<List<Integer>> gs) {
        List<List<Integer>> g = clone(gs);
        int src = (int) Math.floor(rng.next() * g.size());
        if (g.get(src).isEmpty()) {
            return gs;
        }
        int o = g.get(src).get((int) Math.floor(rng.next() * g.get(src).size()));
        double base = cost(g);
        List<List<Integer>> best = null;
        double bc = base;
        for (int t = -1; t < g.size(); t++) {
            if (t == src) {
                continue;
            }
            List<List<Integer>> h = clone(g);
            List<Integer> filtered = new ArrayList<Integer>();
            for (int x : h.get(src)) {
                if (x != o) {
                    filtered.add(x);
                }
            }
            h.set(src, filtered);
            if (t == -1) {
                if (g.get(src).size() == 1) {
                    continue;
                }
                List<Integer> single = new ArrayList<Integer>();
                single.add(o);
                h.add(single);
            } else {
                if (h.get(t).size() >= maxSz) {
                    continue;
                }
                h.get(t).add(o);
            }
            List<List<Integer>> hh = repair(h);
            double c = cost(hh);
            if (c < bc - 1e-9) {
                bc = c;
                best = hh;
            }
        }
        return best != null ? best : gs;
    }

    private static final Comparator<Ind> BY_FITNESS = new Comparator<Ind>() {
        public int compare(Ind a, Ind b) {
            double d = a.f - b.f;
            return d < 0 ? -1 : d > 0 ? 1 : 0;
        }
    };

    private Result execute() {
        List<List<Integer>> singles = new ArrayList<List<Integer>>();
        for (int i = 0; i < n; i++) {
            List<Integer> one = new ArrayList<Integer>();
            one.add(i);
            singles.add(one);
        }
        List<Ind> pop = new ArrayList<Ind>();
        pop.add(new Ind(singles)); // one "no pooling" individual; everything else starts random
        while (pop.size() < gp.gaPop) {
            pop.add(randomInd());
        }
        for (Ind p : pop) {
            p.f = cost(p.gs);
        }
        List<Ind> cur = pop;
        for (int g = 0; g < gp.gaGens; g++) {
            Collections.sort(cur, BY_FITNESS);
            List<Ind> next = new ArrayList<Ind>();
            for (int i = 0; i < 2 && i < cur.size(); i++) {
                Ind e = new Ind(clone(cur.get(i).gs));
                e.f = cur.get(i).f;
                next.add(e);
            }
            for (int im = 0; im < 3 && next.size() < gp.gaPop; im++) { // random immigrants keep diversity
                Ind ind = randomInd();
                ind.f = cost(ind.gs);
                next.add(ind);
            }
            while (next.size() < gp.gaPop) {
                List<Ind> parents = LEGACY_PARENT_POOL ? pop : cur;
                Ind A = tour(parents), B = tour(parents);
                List<List<Integer>> child = clone(A.gs);
                if (rng.next() < gp.gaPc) {
                    child = repair(crossover(A.gs, B.gs));
                }
                if (rng.next() < gp.gaPm) {
                    child = mutate(child);
                }
                if (rng.next() < 0.25) {
                    List<List<Integer>> imp = improve(child);
                    if (imp != child) {
                        child = imp;
                    }
                }
                Ind ind = new Ind(child);
                ind.f = cost(child);
                next.add(ind);
            }
            cur = next;
        }
        Collections.sort(cur, BY_FITNESS);
        Result res = new Result();
        res.best = cur.get(0).gs;
        res.bestCost = cur.get(0).f;
        res.singlesCost = cost(singles);
        res.generations = gp.gaGens;
        return res;
    }

    /** Turns the GA's groups into batches (sorted most urgent first) with their cheapest sequence. */
    public static List<Batch> makeBatches(Core.Ctx ctx, List<RiderState> riders, List<List<Integer>> gs, Cap maxCap, int maxSz) {
        final List<Ord> os = ctx.os;
        List<Batch> batches = new ArrayList<Batch>();
        for (List<Integer> idxs : gs) {
            BatchInfo info = Core.batchInfo(ctx, idxs, maxCap, maxSz);
            Batch b = new Batch();
            b.idxs = new ArrayList<Integer>(idxs);
            b.kg = info.kg;
            b.pk = info.pk;
            double umax = 0, usum = 0, dmin = Double.POSITIVE_INFINITY;
            boolean first = true;
            for (int i : idxs) {
                double u = os.get(i).u;
                umax = first ? u : Math.max(umax, u);
                usum += u;
                dmin = Math.min(dmin, os.get(i).deadline);
                first = false;
            }
            b.umax = umax;
            b.uavg = usum / idxs.size();
            b.dmin = dmin;
            b.info = info;
            b.seq = info.seq;
            for (RiderState r : riders) {
                if (info.kg <= r.cap.kg && info.pk <= r.cap.pk) {
                    b.eligible.add(r.i);
                }
            }
            batches.add(b);
        }
        Collections.sort(batches, new Comparator<Batch>() {
            public int compare(Batch a, Batch b) {
                double d = b.umax - a.umax;
                if (d != 0) {
                    return d < 0 ? -1 : 1;
                }
                double e = a.dmin - b.dmin;
                return e < 0 ? -1 : e > 0 ? 1 : 0;
            }
        });
        for (int i = 0; i < batches.size(); i++) {
            batches.get(i).id = "B" + (i + 1);
            batches.get(i).k = i;
        }
        return batches;
    }
}
