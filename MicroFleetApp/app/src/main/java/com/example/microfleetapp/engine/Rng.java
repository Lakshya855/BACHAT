package com.example.microfleetapp.engine;

import java.util.List;

/** Small deterministic helpers shared by the intelligent-system engines (Mulberry32 RNG etc.). */
public final class Rng {

    private int a;

    public Rng(int seed) {
        this.a = seed;
    }

    /** Uniform double in [0, 1) - identical sequence to the simulator's mulberry32. */
    public double next() {
        a += 0x6D2B79F5;
        int t = a;
        t = (t ^ (t >>> 15)) * (1 | t);
        t = (t + ((t ^ (t >>> 7)) * (61 | t))) ^ t;
        return ((t ^ (t >>> 14)) & 0xFFFFFFFFL) / 4294967296.0;
    }

    /** floor(next() * n) */
    public int nextInt(int n) {
        return (int) Math.floor(next() * n);
    }

    /** In-place Fisher-Yates shuffle (same draw order as the simulator). */
    public <T> List<T> shuffle(List<T> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = (int) Math.floor(next() * (i + 1));
            T tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
        return list;
    }

    /** FNV-1a 32-bit hash of a string, as an unsigned value. */
    public static long hashStr(String s) {
        int h = (int) 2166136261L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 16777619;
        }
        return h & 0xFFFFFFFFL;
    }

    /** JavaScript-style number to string (1 -> "1", 0.25 -> "0.25"), used in cache keys. */
    public static String num(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return Long.toString((long) d);
        }
        return Double.toString(d);
    }

    public static double clamp(double x, double a, double b) {
        return Math.max(a, Math.min(b, x));
    }
}
