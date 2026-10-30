package com.example.microfleetapp.data;

import com.example.microfleetapp.engine.Scenario;

/** Fixed catalogue of the pilot: partner shops and the delivery riders. */
public final class Catalog {

    private Catalog() { }

    public static final class Merchant {
        public final int id;
        public final String name, type, defaultPkg;
        public final int node, priority;

        Merchant(int id, String name, String type, int node, int priority, String defaultPkg) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.node = node;
            this.priority = priority;
            this.defaultPkg = defaultPkg;
        }
    }

    public static final class Rider {
        public final int id;
        public final String name;
        public final int startNode, capPk;
        public final double capKg;
        public final int color;

        Rider(int id, String name, int startNode, double capKg, int capPk, int color) {
            this.id = id;
            this.name = name;
            this.startNode = startNode;
            this.capKg = capKg;
            this.capPk = capPk;
            this.color = color;
        }
    }

    /** Same nine shop types and locations as the project's simulation. */
    public static final Merchant[] MERCHANTS = {
            new Merchant(0, "Bandra West Pharmacy", "Pharmacy", 3, 1, "MEDICAL"),
            new Merchant(1, "Bandra Stn Bakery", "Bakery", 2, 2, "PERISHABLE"),
            new Merchant(2, "Khar Florist", "Florist", 5, 2, "FRAGILE"),
            new Merchant(3, "Santacruz Boutique", "Boutique", 6, 3, "PARCEL"),
            new Merchant(4, "Dadar Grocery", "Grocery", 0, 2, "PERISHABLE"),
            new Merchant(5, "Vile Parle Stationery", "Stationery", 7, 1, "DOCUMENT"),
            new Merchant(6, "Andheri East Electronics", "Electronics", 11, 2, "FRAGILE"),
            new Merchant(7, "Juhu Circle Cafe", "Cafe", 9, 2, "PERISHABLE"),
            new Merchant(8, "Mahim Sweet Shop", "Sweet shop", 1, 3, "PERISHABLE")};

    public static final int[] RIDER_COLORS = {0xFFFDE047, 0xFFF4F2FF, 0xFF22D3EE, 0xFFFB7185, 0xFFA3E635};

    public static final Rider[] RIDERS = buildRiders();

    private static Rider[] buildRiders() {
        Scenario.RiderSpec[] s = Scenario.CLASSIC_RIDERS;
        Rider[] r = new Rider[s.length];
        for (int i = 0; i < s.length; i++) {
            r[i] = new Rider(i, s[i].name, s[i].node, s[i].kg, s[i].pk, RIDER_COLORS[i % RIDER_COLORS.length]);
        }
        return r;
    }

    public static final String[] PACKAGE_TYPES = {"MEDICAL", "PERISHABLE", "FRAGILE", "DOCUMENT", "PARCEL"};
    public static final String[] CUSTOMER_TYPES = {"STANDARD", "PREMIUM", "VIP"};

    public static Merchant merchantOfType(String type) {
        for (Merchant m : MERCHANTS) {
            if (m.type.equals(type)) {
                return m;
            }
        }
        return MERCHANTS[0];
    }

    public static String pretty(String enumName) {
        if (enumName == null || enumName.isEmpty()) {
            return "";
        }
        String s = enumName.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
