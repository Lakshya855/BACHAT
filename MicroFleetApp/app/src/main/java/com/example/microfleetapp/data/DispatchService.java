package com.example.microfleetapp.data;

import android.os.Handler;
import android.os.Looper;

import com.example.microfleetapp.engine.Dispatch;
import com.example.microfleetapp.engine.Rl;
import com.example.microfleetapp.engine.Types.Ord;
import com.example.microfleetapp.engine.Types.Params;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Connects the app's data to the intelligent-system engine and runs it on a background thread:
 * the UI thread builds an immutable snapshot, a worker computes the plan, the result comes back
 * on the UI thread.
 */
public final class DispatchService {

    /** Largest pool handled in one round; the rest simply waits for the next round. */
    public static final int MAX_POOL = 24;

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private DispatchService() { }

    public interface Listener {
        void onStage(String message);

        void onDone(Dispatch.Plan plan, double[] trainedWeights, List<Rl.PolicyRow> policyRows);

        void onError(Exception e);
    }

    /** Everything the worker needs, copied from the store so it never touches live data. */
    private static final class Snapshot {
        List<Ord> orders = new ArrayList<Ord>();
        List<Dispatch.RiderIn> riders = new ArrayList<Dispatch.RiderIn>();
        double[] weights;
        int episodes;
        Params params = new Params();
        int seed;
    }

    /** Fuzzy-scored view of the pending pool (used by the dispatcher screen as well). */
    public static List<Ord> scorePending(Store store, long now) {
        List<Store.OrderRec> pending = store.pendingOrders();
        List<Ord> first = new ArrayList<Ord>();
        for (int i = 0; i < pending.size(); i++) {
            first.add(toOrd(pending.get(i), i, now));
        }
        if (first.size() > MAX_POOL) {
            Collections.sort(first, new Comparator<Ord>() {
                public int compare(Ord a, Ord b) {
                    double d = b.u - a.u;
                    return d < 0 ? -1 : d > 0 ? 1 : 0;
                }
            });
            first = new ArrayList<Ord>(first.subList(0, MAX_POOL));
        }
        // Re-number so engine indices are 0..n-1.
        List<Ord> out = new ArrayList<Ord>();
        for (int i = 0; i < first.size(); i++) {
            Ord o = first.get(i);
            out.add(toOrd(store.order(o.appId), i, now));
        }
        return out;
    }

    private static Ord toOrd(Store.OrderRec o, int i, long now) {
        Catalog.Merchant m = Catalog.MERCHANTS[o.merchantId];
        double ready = Math.max(0, (o.readyAt - now) / 60000.0);
        double deadline = (o.deadlineAt - now) / 60000.0;
        double waited = Math.max(0, (now - o.createdAt) / 60000.0);
        return new Ord(i, o.id, m.type, m.node, o.drop, o.pkg, o.kg, o.pk, o.cust, m.priority,
                deadline, waited, ready, 201);
    }

    public static List<Dispatch.RiderIn> onDutyRiders(Store store) {
        List<Dispatch.RiderIn> out = new ArrayList<Dispatch.RiderIn>();
        for (Catalog.Rider r : Catalog.RIDERS) {
            if (!store.riderOnDuty[r.id]) {
                continue;
            }
            double[] av = store.riderAvailability(r.id);
            out.add(new Dispatch.RiderIn(r.id, r.name, (int) av[1], av[0], store.activeBatches(r.id).size(),
                    r.capKg, r.capPk));
        }
        return out;
    }

    /** Runs GA -> ACO -> RL on a worker thread. Call from the UI thread. */
    public static void run(Store store, final Listener listener) {
        final Snapshot snap = new Snapshot();
        long now = System.currentTimeMillis();
        snap.orders = scorePending(store, now);
        snap.riders = onDutyRiders(store);
        snap.weights = store.rlWeights;
        snap.episodes = snap.params.episodes;
        snap.seed = (int) (now / 60000L % 9000) + 1;
        WORKER.execute(new Runnable() {
            public void run() {
                try {
                    double[] w = snap.weights;
                    List<Rl.PolicyRow> rows = null;
                    boolean trained = false;
                    if (w == null) {
                        post(listener, "RL: learning a rider-assignment policy (first run only)");
                        w = Rl.train(1, snap.episodes, snap.params.lr);
                        rows = Rl.evaluatePolicies(w, 60, 5000);
                        trained = true;
                    }
                    final Dispatch.Plan plan = Dispatch.plan(snap.orders, snap.riders, snap.params, w, snap.seed,
                            new Dispatch.Progress() {
                                public void stage(String message) {
                                    post(listener, message);
                                }
                            });
                    final double[] wf = trained ? w : null;
                    final List<Rl.PolicyRow> rf = rows;
                    MAIN.post(new Runnable() {
                        public void run() {
                            listener.onDone(plan, wf, rf);
                        }
                    });
                } catch (final Exception e) {
                    MAIN.post(new Runnable() {
                        public void run() {
                            listener.onError(e);
                        }
                    });
                }
            }
        });
    }

    /** Re-trains the RL policy from scratch in the background. */
    public static void retrain(final Params params, final Listener listener) {
        WORKER.execute(new Runnable() {
            public void run() {
                try {
                    post(listener, "RL: training on " + params.episodes + " random dispatch episodes");
                    final double[] w = Rl.train(1, params.episodes, params.lr);
                    final List<Rl.PolicyRow> rows = Rl.evaluatePolicies(w, 60, 5000);
                    MAIN.post(new Runnable() {
                        public void run() {
                            listener.onDone(null, w, rows);
                        }
                    });
                } catch (final Exception e) {
                    MAIN.post(new Runnable() {
                        public void run() {
                            listener.onError(e);
                        }
                    });
                }
            }
        });
    }

    private static void post(final Listener l, final String msg) {
        MAIN.post(new Runnable() {
            public void run() {
                l.onStage(msg);
            }
        });
    }
}
