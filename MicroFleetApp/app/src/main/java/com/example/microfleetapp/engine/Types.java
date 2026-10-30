package com.example.microfleetapp.engine;

import java.util.ArrayList;
import java.util.List;

/** Plain data classes used by the intelligent-system engines. */
public final class Types {

    private Types() { }

    /** Weight of each package type in the priority score. */
    public static double pkgWeight(String pkg) {
        switch (pkg) {
            case "MEDICAL": return 1.0;
            case "PERISHABLE": return 0.8;
            case "FRAGILE": return 0.6;
            case "DOCUMENT": return 0.5;
            default: return 0.3; // PARCEL
        }
    }

    public static double custWeight(String cust) {
        switch (cust) {
            case "VIP": return 1.0;
            case "PREMIUM": return 0.6;
            default: return 0.2; // STANDARD
        }
    }

    public static double merchantWeight(int mp) {
        return mp == 1 ? 1.0 : mp == 2 ? 0.6 : 0.3;
    }

    /** One delivery order as seen by the engines. All times are minutes relative to "now". */
    public static final class Ord {
        public final int i;          // index in the engine's order list
        public final int appId;      // id of the order in the app (0 for synthetic orders)
        public final int num;        // 1-based display number
        public final String merchant;
        public final int pickup, drop;
        public final String pkg, cust;
        public final double kg;
        public final int pk, mp;
        public final double deadline, waited, ready;
        public final double direct, slack, prio, u;
        public final String ulabel;
        public final Fuzzy.Result fz;

        public Ord(int i, int appId, String merchant, int pickup, int drop, String pkg, double kg, int pk,
                   String cust, int mp, double deadline, double waited, double ready, int fuzzyRes) {
            this.i = i;
            this.appId = appId;
            this.num = i + 1;
            this.merchant = merchant;
            this.pickup = pickup;
            this.drop = drop;
            this.pkg = pkg;
            this.kg = kg;
            this.pk = pk;
            this.cust = cust;
            this.mp = mp;
            this.deadline = deadline;
            this.waited = waited;
            this.ready = ready;
            this.direct = Road.T[pickup][drop];
            this.slack = deadline - ready - direct - Core.SERVICE;
            this.prio = 0.5 * custWeight(cust) + 0.2 * merchantWeight(mp) + 0.3 * pkgWeight(pkg);
            this.fz = Fuzzy.urgency(slack, waited, prio, fuzzyRes);
            this.u = fz.urgency;
            this.ulabel = Fuzzy.label(u);
        }
    }

    /** A route event: pickup ('P') or drop-off ('D') of order index o. */
    public static final class Ev {
        public final char k;
        public final int o;

        public Ev(char k, int o) {
            this.k = k;
            this.o = o;
        }
    }

    public static final class Cap {
        public final double kg;
        public final int pk;

        public Cap(double kg, int pk) {
            this.kg = kg;
            this.pk = pk;
        }
    }

    public static final class Stop {
        public Ev ev;
        public int node;
        public double arr, wait, depart, late, load;
        public int pk;
    }

    /** Result of simulating a pickup/delivery sequence. */
    public static final class Eval {
        public double finish, dist, late, lateW, duration, firstArr, startT;
        public int viol, endNode, startNode;
        public List<Stop> stops;
        public AcoResult aco; // set when the route was produced by ACO
    }

    public static final class BatchInfo {
        public List<Ev> seq;
        public double dist, lateW, late, kg, capPen, cost, finish;
        public int pk;
    }

    /** A group of orders delivered together. */
    public static final class Batch {
        public List<Integer> idxs;
        public double kg, umax, uavg, dmin;
        public int pk;
        public BatchInfo info;
        public List<Ev> seq;
        public List<Integer> eligible = new ArrayList<Integer>();
        public String id;
        public int k;
    }

    public static final class RouteRec {
        public Batch batch;
        public Eval detail;
        public double startT;
        public int startNode;
    }

    /** Rider as seen by the RL agent. */
    public static final class RiderState {
        public final int i;
        public int node;
        public double avail;
        public int count;
        public final Cap cap;
        public double km;
        public final List<RouteRec> routes = new ArrayList<RouteRec>();

        public RiderState(int i, int node, double avail, int count, Cap cap) {
            this.i = i;
            this.node = node;
            this.avail = avail;
            this.count = count;
            this.cap = cap;
        }
    }

    public static final class AcoResult {
        public List<Ev> evs;          // event list incl. rider origin at index 0 (o = -1)
        public int[] evNode;
        public List<Integer> bestSeq; // indices into evs
        public double bestCost, greedyCost, greedyDist, bestDist;
        public Eval detail, gDetail;
        public int startNode;
        public double startT;
        public boolean fallback;
    }

    public static final class Params {
        public int gaPop = 36, gaGens = 60, maxBatch = 3;
        public double gaPc = .85, gaPm = .35;
        public int ants = 14, iters = 40, maxCand = 3;
        public double alpha = 1, beta = 2.5, rho = .25;
        public int episodes = 900;
        public double lr = .02;
    }
}
