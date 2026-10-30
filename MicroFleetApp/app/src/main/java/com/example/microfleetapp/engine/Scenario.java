package com.example.microfleetapp.engine;

import com.example.microfleetapp.engine.Types.Ord;

import java.util.ArrayList;
import java.util.List;

/** Merchant catalogue, random order generator (used to train the RL agent) and the classic example. */
public final class Scenario {

    private Scenario() { }

    public static final String[] M_TYPE = {"Pharmacy", "Bakery", "Florist", "Boutique", "Grocery",
            "Stationery", "Electronics", "Cafe", "Sweet shop"};
    public static final int[] M_NODE = {3, 2, 5, 6, 0, 7, 11, 9, 1};
    public static final String[][] M_PKG = {{"MEDICAL"}, {"PERISHABLE"}, {"FRAGILE"}, {"PARCEL"},
            {"PERISHABLE", "PARCEL"}, {"DOCUMENT"}, {"FRAGILE", "PARCEL"}, {"PERISHABLE"}, {"PERISHABLE"}};
    public static final String[] RIDER_NAMES = {"Aarav", "Rohan", "Vikram", "Sana", "Meera"};

    private static final String[] CUSTS = {"STANDARD", "STANDARD", "PREMIUM", "VIP"};

    public static int merchantIndex(String type) {
        for (int i = 0; i < M_TYPE.length; i++) {
            if (M_TYPE[i].equals(type)) {
                return i;
            }
        }
        return 0;
    }

    /** Order description before fuzzy scoring. Times are minutes relative to "now". */
    public static final class Spec {
        public String merchant, pkg, cust;
        public int drop, pk, mp;
        public double kg, deadline, waited, ready;

        public Spec(String merchant, int drop, String pkg, double kg, int pk, String cust, int mp,
                    double deadline, double waited, double ready) {
            this.merchant = merchant;
            this.drop = drop;
            this.pkg = pkg;
            this.kg = kg;
            this.pk = pk;
            this.cust = cust;
            this.mp = mp;
            this.deadline = deadline;
            this.waited = waited;
            this.ready = ready;
        }
    }

    public static final class RiderSpec {
        public final String name;
        public final int node, pk;
        public final double kg;

        public RiderSpec(String name, int node, double kg, int pk) {
            this.name = name;
            this.node = node;
            this.kg = kg;
            this.pk = pk;
        }
    }

    /** The worked example used throughout the project report: 10 orders, 4 riders. */
    public static final Spec[] CLASSIC_ORDERS = {
            new Spec("Pharmacy", 10, "MEDICAL", 1.2, 1, "VIP", 1, 45, 12, 4),
            new Spec("Bakery", 8, "PERISHABLE", 4, 2, "PREMIUM", 2, 60, 8, 6),
            new Spec("Florist", 9, "FRAGILE", 2.5, 1, "STANDARD", 2, 75, 5, 8),
            new Spec("Boutique", 12, "PARCEL", 3, 1, "STANDARD", 3, 110, 3, 5),
            new Spec("Grocery", 17, "PERISHABLE", 8, 3, "STANDARD", 2, 55, 15, 3),
            new Spec("Stationery", 13, "DOCUMENT", 0.4, 1, "PREMIUM", 1, 40, 20, 2),
            new Spec("Bakery", 6, "PERISHABLE", 3, 2, "STANDARD", 2, 70, 10, 7),
            new Spec("Pharmacy", 14, "MEDICAL", 0.8, 1, "STANDARD", 1, 65, 6, 3),
            new Spec("Florist", 15, "FRAGILE", 2, 1, "STANDARD", 3, 120, 2, 10),
            new Spec("Grocery", 1, "PARCEL", 6, 2, "STANDARD", 3, 90, 4, 5)};

    public static final RiderSpec[] CLASSIC_RIDERS = {
            new RiderSpec("Aarav", 2, 25, 10), new RiderSpec("Rohan", 7, 20, 8),
            new RiderSpec("Vikram", 0, 30, 15), new RiderSpec("Sana", 11, 12, 6)};

    public static List<Spec> genOrderSpecs(Rng rng, int n) {
        List<Spec> list = new ArrayList<Spec>();
        for (int i = 0; i < n; i++) {
            int mi = rng.nextInt(M_TYPE.length);
            int mnode = M_NODE[mi];
            int drop;
            do {
                drop = rng.nextInt(Road.N);
            } while (drop == mnode);
            String[] pkgs = M_PKG[mi];
            String pkg = pkgs[rng.nextInt(pkgs.length)];
            int ready = 2 + rng.nextInt(10);
            double deadline = Math.round(ready + Road.T[mnode][drop] + Core.SERVICE + 28 + rng.next() * 90);
            double kg = Math.round((0.4 + rng.next() * 7) * 10) / 10.0;
            int pk = 1 + rng.nextInt(3);
            String cust = CUSTS[rng.nextInt(4)];
            int mp = 1 + rng.nextInt(3);
            double waited = rng.nextInt(24);
            list.add(new Spec(M_TYPE[mi], drop, pkg, kg, pk, cust, mp, deadline, waited, ready));
        }
        return list;
    }

    public static List<RiderSpec> genRiderSpecs(Rng rng, int n) {
        List<RiderSpec> out = new ArrayList<RiderSpec>();
        for (int i = 0; i < n; i++) {
            int node = rng.nextInt(Road.N);
            double kg = Math.round(12 + rng.next() * 18);
            int pk = 6 + rng.nextInt(9);
            out.add(new RiderSpec(RIDER_NAMES[i], node, kg, pk));
        }
        return out;
    }

    public static Ord buildOrder(Spec s, int i, int fuzzyRes) {
        int mi = merchantIndex(s.merchant);
        return new Ord(i, 0, s.merchant, M_NODE[mi], s.drop, s.pkg, s.kg, s.pk, s.cust, s.mp,
                s.deadline, s.waited, s.ready, fuzzyRes);
    }
}
