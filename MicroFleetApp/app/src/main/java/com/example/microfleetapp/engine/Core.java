package com.example.microfleetapp.engine;

import com.example.microfleetapp.engine.Types.Batch;
import com.example.microfleetapp.engine.Types.BatchInfo;
import com.example.microfleetapp.engine.Types.Cap;
import com.example.microfleetapp.engine.Types.Eval;
import com.example.microfleetapp.engine.Types.Ev;
import com.example.microfleetapp.engine.Types.Ord;
import com.example.microfleetapp.engine.Types.Stop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Route evaluation shared by the GA, ACO and RL stages. */
public final class Core {

    public static final double SERVICE = 2, LAMBDA = 3, TRIP_COST = 3.0, LATE_KM = 0.35;

    private Core() { }

    /** Orders of one dispatch round plus a cache of batch evaluations. */
    public static final class Ctx {
        public final List<Ord> os;
        final Map<String, BatchInfo> memo = new HashMap<String, BatchInfo>();

        public Ctx(List<Ord> os) {
            this.os = os;
        }
    }

    public static double deferPenalty(Batch b) {
        return 20 + 20 * b.umax;
    }

    public static int seqNode(List<Ord> os, Ev e) {
        return e.k == 'P' ? os.get(e.o).pickup : os.get(e.o).drop;
    }

    /** Simulates riding the event sequence; time in minutes, distance in km. */
    public static Eval evalSeq(List<Ord> os, int startNode, double startT, List<Ev> seq, Cap cap, boolean detail) {
        double t = startT, dist = 0, w = 0, late = 0, lateW = 0, firstArr = startT;
        int cur = startNode, pk = 0, viol = 0;
        List<Stop> stops = detail ? new ArrayList<Stop>() : null;
        for (int s = 0; s < seq.size(); s++) {
            Ev ev = seq.get(s);
            Ord o = os.get(ev.o);
            int node = ev.k == 'P' ? o.pickup : o.drop;
            dist += Road.D[cur][node];
            t += Road.T[cur][node];
            double arr = t;
            if (s == 0) {
                firstArr = arr;
            }
            double wait = 0, l = 0;
            if (ev.k == 'P') {
                wait = Math.max(0, o.ready - t);
                t += wait;
                w += o.kg;
                pk += o.pk;
            } else {
                l = Math.max(0, arr - o.deadline);
                late += l;
                lateW += (1 + o.u) * l;
                w -= o.kg;
                pk -= o.pk;
            }
            if (cap != null && (w > cap.kg + 1e-9 || pk > cap.pk)) {
                viol++;
            }
            t += SERVICE;
            if (detail) {
                Stop st = new Stop();
                st.ev = ev;
                st.node = node;
                st.arr = arr;
                st.wait = wait;
                st.depart = t;
                st.late = l;
                st.load = w;
                st.pk = pk;
                stops.add(st);
            }
            cur = node;
        }
        Eval r = new Eval();
        r.finish = t;
        r.dist = dist;
        r.late = late;
        r.lateW = lateW;
        r.viol = viol;
        r.duration = t - startT;
        r.endNode = cur;
        r.firstArr = firstArr;
        r.stops = stops;
        r.startNode = startNode;
        r.startT = startT;
        return r;
    }

    /** startNode == null means "start at the first stop". */
    static double quickCost(List<Ord> os, List<Ev> seq, Integer startNode, double startT) {
        int sn = startNode == null ? seqNode(os, seq.get(0)) : startNode;
        Eval r = evalSeq(os, sn, startT, seq, null, false);
        return r.dist + LATE_KM * r.lateW;
    }

    /** Cheapest-insertion heuristic: most urgent orders first, each at its best position. */
    public static List<Ev> cheapestSeq(final List<Ord> os, List<Integer> idxs, Integer startNode, double startT) {
        List<Integer> ord = new ArrayList<Integer>(idxs);
        Collections.sort(ord, new Comparator<Integer>() {
            public int compare(Integer a, Integer b) {
                double d = os.get(b).u - os.get(a).u;
                if (d != 0) {
                    return d < 0 ? -1 : 1;
                }
                return a - b;
            }
        });
        List<Ev> seq = new ArrayList<Ev>();
        for (int o : ord) {
            List<Ev> bestC = null;
            double bestQ = 0;
            for (int i = 0; i <= seq.size(); i++) {
                for (int j = i; j <= seq.size(); j++) {
                    List<Ev> c = new ArrayList<Ev>(seq);
                    c.add(i, new Ev('P', o));
                    c.add(j + 1, new Ev('D', o));
                    double q = quickCost(os, c, startNode, startT);
                    if (bestC == null || q < bestQ) {
                        bestQ = q;
                        bestC = c;
                    }
                }
            }
            seq = bestC;
        }
        return seq;
    }

    public static Cap maxCapOf(List<Cap> caps) {
        double kg = 0;
        int pk = 0;
        for (Cap c : caps) {
            kg = Math.max(kg, c.kg);
            pk = Math.max(pk, c.pk);
        }
        return new Cap(kg, pk);
    }

    public static BatchInfo batchInfo(Ctx ctx, List<Integer> idxs, Cap maxCap, int maxSz) {
        StringBuilder kb = new StringBuilder();
        for (int i = 0; i < idxs.size(); i++) {
            if (i > 0) {
                kb.append(',');
            }
            kb.append(idxs.get(i));
        }
        String key = kb.toString();
        BatchInfo hit = ctx.memo.get(key);
        if (hit != null) {
            return hit;
        }
        List<Ord> os = ctx.os;
        List<Ev> seq = cheapestSeq(os, idxs, null, 5);
        Eval r = evalSeq(os, seqNode(os, seq.get(0)), 5, seq, null, false);
        double kg = 0;
        int pk = 0;
        for (int i : idxs) {
            kg += os.get(i).kg;
            pk += os.get(i).pk;
        }
        double capPen = (kg > maxCap.kg ? 60 + 10 * (kg - maxCap.kg) : 0)
                + (pk > maxCap.pk ? 60 + 10 * (pk - maxCap.pk) : 0)
                + (idxs.size() > maxSz ? 80 * (idxs.size() - maxSz) : 0);
        BatchInfo info = new BatchInfo();
        info.seq = seq;
        info.dist = r.dist;
        info.lateW = r.lateW;
        info.late = r.late;
        info.kg = kg;
        info.pk = pk;
        info.capPen = capPen;
        info.cost = r.dist + LATE_KM * r.lateW + TRIP_COST + capPen;
        info.finish = r.finish;
        ctx.memo.put(key, info);
        return info;
    }

    public static double mean(double[] a) {
        if (a.length == 0) {
            return 0;
        }
        double s = 0;
        for (double v : a) {
            s += v;
        }
        return s / a.length;
    }

    public static double std(double[] a) {
        double m = mean(a), s = 0;
        for (double v : a) {
            s += (v - m) * (v - m);
        }
        return a.length == 0 ? 0 : Math.sqrt(s / a.length);
    }
}
