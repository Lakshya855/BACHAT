package com.example.microfleetapp.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Mamdani fuzzy-logic urgency: inputs deadline slack (min), waiting time (min) and priority score
 * (0-1); output urgency (0-1) via 14 IF-THEN rules, min-implication, max-aggregation and
 * centroid defuzzification.
 */
public final class Fuzzy {

    private interface MF {
        double at(double x);
    }

    private static MF shL(final double a, final double b) {
        return new MF() {
            public double at(double x) {
                return x <= a ? 1 : x >= b ? 0 : (b - x) / (b - a);
            }
        };
    }

    private static MF shR(final double a, final double b) {
        return new MF() {
            public double at(double x) {
                return x <= a ? 0 : x >= b ? 1 : (x - a) / (b - a);
            }
        };
    }

    private static MF tri(final double a, final double b, final double c) {
        return new MF() {
            public double at(double x) {
                return (x <= a || x >= c) ? 0 : (x < b ? (x - a) / (b - a) : (c - x) / (c - b));
            }
        };
    }

    public static final String[] SLACK = {"Tight", "Moderate", "Comfortable"};
    public static final String[] WAIT = {"Short", "Medium", "Long"};
    public static final String[] PRIO = {"Low", "Medium", "High"};
    public static final String[] URG = {"Low", "Medium", "High", "Critical"};

    private static final MF[] SLACK_MF = {shL(10, 35), tri(20, 50, 85), shR(60, 100)};
    private static final MF[] WAIT_MF = {shL(5, 15), tri(8, 18, 30), shR(22, 35)};
    private static final MF[] PRIO_MF = {shL(.25, .5), tri(.3, .5, .75), shR(.55, .8)};
    private static final MF[] URG_MF = {shL(.15, .4), tri(.25, .45, .65), tri(.5, .7, .88), shR(.75, .95)};

    /** {slack set, wait set, priority set, urgency set}; -1 means "any". */
    public static final int[][] RULES = {
            {0, -1, 2, 3}, {0, 2, -1, 3}, {0, -1, 1, 2}, {0, -1, 0, 2},
            {1, 2, 1, 2}, {1, -1, 2, 2}, {1, -1, 1, 1}, {1, 0, 0, 0},
            {1, 1, 0, 1}, {1, 2, 0, 1}, {2, 2, 2, 2},
            {2, -1, 2, 1}, {2, -1, 1, 0}, {2, -1, 0, 0}};

    public static final class Fired {
        public final int rule;
        public final double strength;

        Fired(int rule, double strength) {
            this.rule = rule;
            this.strength = strength;
        }
    }

    public static final class Result {
        public double slack, wait, prio;
        public double[] mSlack = new double[3], mWait = new double[3], mPrio = new double[3];
        public final List<Fired> fired = new ArrayList<Fired>();
        public double urgency;
    }

    public static String label(double u) {
        return u >= 0.8 ? "Critical" : u >= 0.6 ? "High" : u >= 0.35 ? "Medium" : "Low";
    }

    public static String ruleText(int r) {
        int[] q = RULES[r];
        StringBuilder sb = new StringBuilder("IF slack is ").append(SLACK[q[0]]);
        if (q[1] >= 0) {
            sb.append(" AND waiting is ").append(WAIT[q[1]]);
        }
        if (q[2] >= 0) {
            sb.append(" AND priority is ").append(PRIO[q[2]]);
        }
        return sb.append(" THEN urgency is ").append(URG[q[3]]).toString();
    }

    public static Result urgency(double slack, double wait, double prio) {
        return urgency(slack, wait, prio, 201);
    }

    public static Result urgency(double slack, double wait, double prio, int res) {
        Result r = new Result();
        r.slack = Rng.clamp(slack, 0, 120);
        r.wait = Rng.clamp(wait, 0, 40);
        r.prio = Rng.clamp(prio, 0, 1);
        for (int k = 0; k < 3; k++) {
            r.mSlack[k] = SLACK_MF[k].at(r.slack);
            r.mWait[k] = WAIT_MF[k].at(r.wait);
            r.mPrio[k] = PRIO_MF[k].at(r.prio);
        }
        for (int i = 0; i < RULES.length; i++) {
            int[] q = RULES[i];
            double d0 = q[0] < 0 ? 1 : r.mSlack[q[0]];
            double d1 = q[1] < 0 ? 1 : r.mWait[q[1]];
            double d2 = q[2] < 0 ? 1 : r.mPrio[q[2]];
            double s = Math.min(d0, Math.min(d1, d2));
            if (s > 0) {
                r.fired.add(new Fired(i, s));
            }
        }
        double num = 0, den = 0;
        for (int k = 0; k < res; k++) {
            double u = (double) k / (res - 1);
            double m = 0;
            for (Fired f : r.fired) {
                double mu = Math.min(f.strength, URG_MF[RULES[f.rule][3]].at(u));
                if (mu > m) {
                    m = mu;
                }
            }
            num += u * m;
            den += m;
        }
        r.urgency = den > 0 ? num / den : 0.5;
        return r;
    }

    private Fuzzy() { }
}
