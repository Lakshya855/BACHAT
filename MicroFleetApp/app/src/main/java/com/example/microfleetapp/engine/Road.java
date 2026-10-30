package com.example.microfleetapp.engine;

/**
 * Schematic road network of Mumbai's western suburbs (20 named places) with all-pairs shortest
 * travel times (Floyd-Warshall). Travel time T is in minutes, distance D in km.
 */
public final class Road {

    public static final int N = 20;

    public static final String[] NAMES = {
            "Dadar", "Mahim", "Bandra Stn", "Bandra West", "BKC", "Khar", "Santacruz", "Vile Parle",
            "Juhu Beach", "Juhu Circle", "Andheri West", "Andheri East", "Versova", "Lokhandwala",
            "Jogeshwari", "Goregaon", "Linking Rd", "Bandra East", "Airport", "Milan Subway"};

    public static final double[] LAT = {
            19.0178, 19.0410, 19.0544, 19.0596, 19.0660, 19.0716, 19.0810, 19.0990, 19.0980, 19.1050,
            19.1197, 19.1136, 19.1310, 19.1370, 19.1360, 19.1663, 19.0640, 19.0600, 19.0896, 19.0870};
    public static final double[] LON = {
            72.8478, 72.8400, 72.8402, 72.8295, 72.8680, 72.8370, 72.8400, 72.8440, 72.8260, 72.8330,
            72.8464, 72.8697, 72.8120, 72.8290, 72.8490, 72.8493, 72.8340, 72.8500, 72.8656, 72.8450};

    /** Road segments: {from, to, type} where type 0 = main road, 1 = highway, 2 = local street. */
    public static final int[][] EDGES = {
            {0, 1, 0}, {1, 2, 0}, {1, 3, 0}, {2, 3, 2}, {2, 17, 2}, {17, 4, 0}, {2, 5, 0}, {3, 16, 2}, {16, 5, 2},
            {5, 6, 0}, {6, 19, 0}, {19, 7, 0}, {7, 10, 0}, {10, 14, 1}, {14, 15, 1}, {10, 11, 0}, {11, 18, 0},
            {18, 4, 0}, {6, 18, 0}, {7, 9, 2}, {9, 8, 2}, {9, 10, 2}, {8, 6, 2}, {9, 12, 2}, {12, 13, 2},
            {13, 10, 2}, {13, 14, 2}, {0, 4, 0}};

    /** Approximate coastline (lat, lon) used only to draw the sea on the map. */
    public static final double[][] COAST = {
            {19.000, 72.812}, {19.020, 72.822}, {19.038, 72.822}, {19.043, 72.8205}, {19.060, 72.8215},
            {19.085, 72.8225}, {19.098, 72.8215}, {19.115, 72.8115}, {19.131, 72.806}, {19.150, 72.8045},
            {19.180, 72.812}};

    private static final double LAT0 = 19.178, LON0 = 72.800, HX = 2.2;
    public static final double XMAX = (72.885 - LON0) * 105 * HX;
    public static final double YMAX = (LAT0 - 19.005) * 111;

    /** Travel time in minutes. */
    public static final double[][] T = new double[N][N];
    /** Distance in km. */
    public static final double[][] D = new double[N][N];
    private static final int[][] NX = new int[N][N];

    public static double x(int node) {
        return toX(LON[node]);
    }

    public static double y(int node) {
        return toY(LAT[node]);
    }

    public static double toX(double lon) {
        return (lon - LON0) * 105 * HX;
    }

    public static double toY(double lat) {
        return (LAT0 - lat) * 111;
    }

    private static double hav(int a, int b) {
        double R = 6371, r = Math.PI / 180;
        double dLa = (LAT[b] - LAT[a]) * r, dLo = (LON[b] - LON[a]) * r;
        double h = Math.pow(Math.sin(dLa / 2), 2)
                + Math.cos(LAT[a] * r) * Math.cos(LAT[b] * r) * Math.pow(Math.sin(dLo / 2), 2);
        return 2 * R * Math.asin(Math.sqrt(h));
    }

    static {
        double[] speed = {24, 34, 17};      // main, highway, local (km/h)
        double[] fact = {1.22, 1.12, 1.3};  // road winding factor
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                T[i][j] = Double.POSITIVE_INFINITY;
                D[i][j] = Double.POSITIVE_INFINITY;
                NX[i][j] = -1;
            }
            T[i][i] = 0;
            D[i][i] = 0;
            NX[i][i] = i;
        }
        for (int[] e : EDGES) {
            double km = hav(e[0], e[1]) * fact[e[2]];
            double min = km / speed[e[2]] * 60;
            T[e[0]][e[1]] = min;
            T[e[1]][e[0]] = min;
            D[e[0]][e[1]] = km;
            D[e[1]][e[0]] = km;
            NX[e[0]][e[1]] = e[1];
            NX[e[1]][e[0]] = e[0];
        }
        for (int k = 0; k < N; k++) {
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    if (T[i][k] + T[k][j] < T[i][j] - 1e-9) {
                        T[i][j] = T[i][k] + T[k][j];
                        D[i][j] = D[i][k] + D[k][j];
                        NX[i][j] = NX[i][k];
                    }
                }
            }
        }
    }

    /** Sequence of road nodes on the fastest path a -> b (inclusive). */
    public static int[] pathNodes(int a, int b) {
        int[] tmp = new int[N + 2];
        int n = 0;
        tmp[n++] = a;
        int c = a, guard = 0;
        while (c != b && guard++ < 60 && n < tmp.length) {
            c = NX[c][b];
            if (c < 0) {
                break;
            }
            tmp[n++] = c;
        }
        int[] out = new int[n];
        System.arraycopy(tmp, 0, out, 0, n);
        return out;
    }

    private Road() { }
}
