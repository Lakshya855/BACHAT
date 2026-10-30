package com.example.microfleetapp.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.microfleetapp.engine.Dispatch;
import com.example.microfleetapp.engine.Rl;
import com.example.microfleetapp.engine.Road;
import com.example.microfleetapp.engine.Core;
import com.example.microfleetapp.engine.Scenario;
import com.example.microfleetapp.engine.Types;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * All app data (orders, trips, riders, history, learned RL policy) kept on the phone as JSON in
 * SharedPreferences. Everything is accessed from the UI thread.
 */
public final class Store {

    public static final String PENDING = "PENDING", ASSIGNED = "ASSIGNED", PICKED_UP = "PICKED_UP",
            DELIVERED = "DELIVERED", CANCELLED = "CANCELLED";

    public static final class OrderRec {
        public int id, merchantId, drop, pk, batchId = -1, riderIdx = -1;
        public double kg;
        public String pkg, cust, status = PENDING;
        public long createdAt, readyAt, deadlineAt, pickedAt, deliveredAt;

        public boolean isOpen() {
            return status.equals(PENDING) || status.equals(ASSIGNED) || status.equals(PICKED_UP);
        }
    }

    public static final class StopRec {
        public char kind;   // 'P' pickup, 'D' drop-off
        public int orderId, node;
    }

    public static final class BatchRec {
        public int id, riderIdx, done;
        public long createdAt;
        public double plannedKm, soloKm;
        public boolean finished;
        public final List<StopRec> stops = new ArrayList<StopRec>();

        public StopRec next() {
            return done < stops.size() ? stops.get(done) : null;
        }
    }

    public static final class RunRec {
        public long at;
        public int orders, trips;
        public double pooledKm, soloKm;
    }

    public static final class PolicyRow {
        public String name;
        public double mean, dist, late, asg;
    }

    private static Store instance;

    public static synchronized Store get(Context ctx) {
        if (instance == null) {
            instance = new Store(ctx.getApplicationContext());
        }
        return instance;
    }

    private final SharedPreferences prefs;
    public final List<OrderRec> orders = new ArrayList<OrderRec>();
    public final List<BatchRec> batches = new ArrayList<BatchRec>();
    public final List<RunRec> runs = new ArrayList<RunRec>();
    public final List<PolicyRow> policyRows = new ArrayList<PolicyRow>();
    public int[] riderNode = new int[Catalog.RIDERS.length];
    public boolean[] riderOnDuty = new boolean[Catalog.RIDERS.length];
    public double[] rlWeights;
    public int rlEpisodes;
    public int nextOrderId = 1, nextBatchId = 1, lastMerchant = 0, lastRider = 0;

    private Store(Context ctx) {
        prefs = ctx.getSharedPreferences("microfleet_store", Context.MODE_PRIVATE);
        resetRiders();
        load();
    }

    private void resetRiders() {
        for (int i = 0; i < Catalog.RIDERS.length; i++) {
            riderNode[i] = Catalog.RIDERS[i].startNode;
            riderOnDuty[i] = true;
        }
    }

    // ------------------------------------------------------------------ queries

    public OrderRec order(int id) {
        for (OrderRec o : orders) {
            if (o.id == id) {
                return o;
            }
        }
        return null;
    }

    public BatchRec batch(int id) {
        for (BatchRec b : batches) {
            if (b.id == id) {
                return b;
            }
        }
        return null;
    }

    public List<OrderRec> pendingOrders() {
        List<OrderRec> out = new ArrayList<OrderRec>();
        for (OrderRec o : orders) {
            if (o.status.equals(PENDING)) {
                out.add(o);
            }
        }
        return out;
    }

    public List<OrderRec> ordersOfMerchant(int merchantId) {
        List<OrderRec> out = new ArrayList<OrderRec>();
        for (OrderRec o : orders) {
            if (o.merchantId == merchantId) {
                out.add(o);
            }
        }
        Collections.sort(out, new Comparator<OrderRec>() {
            public int compare(OrderRec a, OrderRec b) {
                return b.id - a.id;
            }
        });
        return out;
    }

    /** Unfinished trips of a rider, oldest first. */
    public List<BatchRec> activeBatches(int rider) {
        List<BatchRec> out = new ArrayList<BatchRec>();
        for (BatchRec b : batches) {
            if (b.riderIdx == rider && !b.finished) {
                out.add(b);
            }
        }
        return out;
    }

    public List<BatchRec> allActiveBatches() {
        List<BatchRec> out = new ArrayList<BatchRec>();
        for (BatchRec b : batches) {
            if (!b.finished) {
                out.add(b);
            }
        }
        return out;
    }

    public int deliveredCount(int rider) {
        int n = 0;
        for (OrderRec o : orders) {
            if (o.riderIdx == rider && o.status.equals(DELIVERED)) {
                n++;
            }
        }
        return n;
    }

    /** Minutes until the rider has finished everything already queued, and where they will end up. */
    public double[] riderAvailability(int rider) {
        int cur = riderNode[rider];
        double t = 0;
        for (BatchRec b : activeBatches(rider)) {
            for (int s = b.done; s < b.stops.size(); s++) {
                int node = b.stops.get(s).node;
                t += Road.T[cur][node] + Core.SERVICE;
                cur = node;
            }
        }
        return new double[]{t, cur};
    }

    // ------------------------------------------------------------------ commands

    public OrderRec addOrder(int merchantId, int drop, String pkg, double kg, int pk, String cust,
                             int prepMin, int deadlineMin) {
        long now = System.currentTimeMillis();
        OrderRec o = new OrderRec();
        o.id = nextOrderId++;
        o.merchantId = merchantId;
        o.drop = drop;
        o.pkg = pkg;
        o.kg = kg;
        o.pk = pk;
        o.cust = cust;
        o.createdAt = now;
        o.readyAt = now + prepMin * 60000L;
        o.deadlineAt = now + deadlineMin * 60000L;
        orders.add(o);
        save();
        return o;
    }

    public boolean cancelOrder(int id) {
        OrderRec o = order(id);
        if (o == null || !o.status.equals(PENDING)) {
            return false;
        }
        o.status = CANCELLED;
        save();
        return true;
    }

    /** Rider confirms the next stop of a trip (pickup or delivery). */
    public void completeNextStop(BatchRec b) {
        StopRec s = b.next();
        if (s == null) {
            return;
        }
        long now = System.currentTimeMillis();
        OrderRec o = order(s.orderId);
        if (o != null) {
            if (s.kind == 'P') {
                o.status = PICKED_UP;
                o.pickedAt = now;
            } else {
                o.status = DELIVERED;
                o.deliveredAt = now;
            }
        }
        riderNode[b.riderIdx] = s.node;
        b.done++;
        if (b.done >= b.stops.size()) {
            b.finished = true;
        }
        save();
    }

    public void setOnDuty(int rider, boolean on) {
        riderOnDuty[rider] = on;
        save();
    }

    /** Turns an accepted plan into trips for riders. */
    public int commitPlan(Dispatch.Plan plan) {
        long now = System.currentTimeMillis();
        int created = 0;
        double pooled = 0, solo = 0;
        int orderCount = 0;
        for (Dispatch.PlannedBatch pb : plan.batches) {
            if (pb.riderPos < 0) {
                continue;
            }
            BatchRec b = new BatchRec();
            b.id = nextBatchId++;
            b.riderIdx = plan.riders.get(pb.riderPos).ref;
            b.createdAt = now;
            b.plannedKm = pb.route.dist;
            b.soloKm = pb.soloKm;
            for (Types.Stop st : pb.route.stops) {
                StopRec s = new StopRec();
                s.kind = st.ev.k;
                s.orderId = plan.orders.get(st.ev.o).appId;
                s.node = st.node;
                b.stops.add(s);
            }
            for (int idx : pb.batch.idxs) {
                OrderRec o = order(plan.orders.get(idx).appId);
                if (o != null) {
                    o.status = ASSIGNED;
                    o.batchId = b.id;
                    o.riderIdx = b.riderIdx;
                    orderCount++;
                }
            }
            pooled += b.plannedKm;
            solo += b.soloKm;
            batches.add(b);
            created++;
        }
        if (created > 0) {
            RunRec r = new RunRec();
            r.at = now;
            r.orders = orderCount;
            r.trips = created;
            r.pooledKm = pooled;
            r.soloKm = solo;
            runs.add(r);
        }
        save();
        return created;
    }

    /** Loads the project's classic worked example: 10 orders from 9 shops. */
    public void seedDemo() {
        long now = System.currentTimeMillis();
        for (Scenario.Spec s : Scenario.CLASSIC_ORDERS) {
            Catalog.Merchant m = Catalog.merchantOfType(s.merchant);
            OrderRec o = new OrderRec();
            o.id = nextOrderId++;
            o.merchantId = m.id;
            o.drop = s.drop;
            o.pkg = s.pkg;
            o.kg = s.kg;
            o.pk = s.pk;
            o.cust = s.cust;
            o.createdAt = now - (long) (s.waited * 60000);
            o.readyAt = now + (long) (s.ready * 60000);
            o.deadlineAt = now + (long) (s.deadline * 60000);
            orders.add(o);
        }
        save();
    }

    public void resetAll() {
        orders.clear();
        batches.clear();
        runs.clear();
        nextOrderId = 1;
        nextBatchId = 1;
        resetRiders();
        save();
    }

    public void setRlPolicy(double[] w, int episodes, List<Rl.PolicyRow> rows) {
        rlWeights = w;
        rlEpisodes = episodes;
        policyRows.clear();
        if (rows != null) {
            for (Rl.PolicyRow r : rows) {
                PolicyRow p = new PolicyRow();
                p.name = r.name;
                p.mean = r.mean;
                p.dist = r.dist;
                p.late = r.late;
                p.asg = r.asg;
                policyRows.add(p);
            }
        }
        save();
    }

    public void setLastMerchant(int i) {
        lastMerchant = i;
        save();
    }

    public void setLastRider(int i) {
        lastRider = i;
        save();
    }

    // ------------------------------------------------------------------ persistence

    public void save() {
        try {
            JSONObject root = new JSONObject();
            root.put("nextOrderId", nextOrderId);
            root.put("nextBatchId", nextBatchId);
            root.put("lastMerchant", lastMerchant);
            root.put("lastRider", lastRider);
            root.put("rlEpisodes", rlEpisodes);
            JSONArray ords = new JSONArray();
            for (OrderRec o : orders) {
                JSONObject j = new JSONObject();
                j.put("id", o.id);
                j.put("m", o.merchantId);
                j.put("drop", o.drop);
                j.put("pkg", o.pkg);
                j.put("kg", o.kg);
                j.put("pk", o.pk);
                j.put("cust", o.cust);
                j.put("st", o.status);
                j.put("c", o.createdAt);
                j.put("r", o.readyAt);
                j.put("d", o.deadlineAt);
                j.put("pa", o.pickedAt);
                j.put("da", o.deliveredAt);
                j.put("b", o.batchId);
                j.put("ri", o.riderIdx);
                ords.put(j);
            }
            root.put("orders", ords);
            JSONArray bs = new JSONArray();
            for (BatchRec b : batches) {
                JSONObject j = new JSONObject();
                j.put("id", b.id);
                j.put("ri", b.riderIdx);
                j.put("done", b.done);
                j.put("c", b.createdAt);
                j.put("km", b.plannedKm);
                j.put("solo", b.soloKm);
                j.put("fin", b.finished);
                JSONArray ss = new JSONArray();
                for (StopRec s : b.stops) {
                    JSONObject q = new JSONObject();
                    q.put("k", String.valueOf(s.kind));
                    q.put("o", s.orderId);
                    q.put("n", s.node);
                    ss.put(q);
                }
                j.put("stops", ss);
                bs.put(j);
            }
            root.put("batches", bs);
            JSONArray rs = new JSONArray();
            for (RunRec r : runs) {
                JSONObject j = new JSONObject();
                j.put("at", r.at);
                j.put("o", r.orders);
                j.put("t", r.trips);
                j.put("km", r.pooledKm);
                j.put("solo", r.soloKm);
                rs.put(j);
            }
            root.put("runs", rs);
            JSONArray nodes = new JSONArray();
            JSONArray duty = new JSONArray();
            for (int i = 0; i < riderNode.length; i++) {
                nodes.put(riderNode[i]);
                duty.put(riderOnDuty[i]);
            }
            root.put("riderNode", nodes);
            root.put("riderDuty", duty);
            if (rlWeights != null) {
                JSONArray w = new JSONArray();
                for (double x : rlWeights) {
                    w.put(x);
                }
                root.put("rlWeights", w);
            }
            JSONArray pr = new JSONArray();
            for (PolicyRow p : policyRows) {
                JSONObject j = new JSONObject();
                j.put("n", p.name);
                j.put("mean", p.mean);
                j.put("dist", p.dist);
                j.put("late", p.late);
                j.put("asg", p.asg);
                pr.put(j);
            }
            root.put("policy", pr);
            prefs.edit().putString("state", root.toString()).apply();
        } catch (JSONException e) {
            // Saving is best effort; the in-memory state stays valid.
        }
    }

    private void load() {
        String raw = prefs.getString("state", null);
        if (raw == null) {
            return;
        }
        try {
            JSONObject root = new JSONObject(raw);
            nextOrderId = root.optInt("nextOrderId", 1);
            nextBatchId = root.optInt("nextBatchId", 1);
            lastMerchant = root.optInt("lastMerchant", 0);
            lastRider = root.optInt("lastRider", 0);
            rlEpisodes = root.optInt("rlEpisodes", 0);
            JSONArray ords = root.optJSONArray("orders");
            for (int i = 0; ords != null && i < ords.length(); i++) {
                JSONObject j = ords.getJSONObject(i);
                OrderRec o = new OrderRec();
                o.id = j.getInt("id");
                o.merchantId = j.getInt("m");
                o.drop = j.getInt("drop");
                o.pkg = j.getString("pkg");
                o.kg = j.getDouble("kg");
                o.pk = j.getInt("pk");
                o.cust = j.getString("cust");
                o.status = j.getString("st");
                o.createdAt = j.getLong("c");
                o.readyAt = j.getLong("r");
                o.deadlineAt = j.getLong("d");
                o.pickedAt = j.optLong("pa", 0);
                o.deliveredAt = j.optLong("da", 0);
                o.batchId = j.optInt("b", -1);
                o.riderIdx = j.optInt("ri", -1);
                orders.add(o);
            }
            JSONArray bs = root.optJSONArray("batches");
            for (int i = 0; bs != null && i < bs.length(); i++) {
                JSONObject j = bs.getJSONObject(i);
                BatchRec b = new BatchRec();
                b.id = j.getInt("id");
                b.riderIdx = j.getInt("ri");
                b.done = j.getInt("done");
                b.createdAt = j.getLong("c");
                b.plannedKm = j.getDouble("km");
                b.soloKm = j.optDouble("solo", 0);
                b.finished = j.getBoolean("fin");
                JSONArray ss = j.getJSONArray("stops");
                for (int k = 0; k < ss.length(); k++) {
                    JSONObject q = ss.getJSONObject(k);
                    StopRec s = new StopRec();
                    s.kind = q.getString("k").charAt(0);
                    s.orderId = q.getInt("o");
                    s.node = q.getInt("n");
                    b.stops.add(s);
                }
                batches.add(b);
            }
            JSONArray rs = root.optJSONArray("runs");
            for (int i = 0; rs != null && i < rs.length(); i++) {
                JSONObject j = rs.getJSONObject(i);
                RunRec r = new RunRec();
                r.at = j.getLong("at");
                r.orders = j.getInt("o");
                r.trips = j.getInt("t");
                r.pooledKm = j.getDouble("km");
                r.soloKm = j.getDouble("solo");
                runs.add(r);
            }
            JSONArray nodes = root.optJSONArray("riderNode");
            JSONArray duty = root.optJSONArray("riderDuty");
            for (int i = 0; nodes != null && i < nodes.length() && i < riderNode.length; i++) {
                riderNode[i] = nodes.getInt(i);
                riderOnDuty[i] = duty == null || duty.optBoolean(i, true);
            }
            JSONArray w = root.optJSONArray("rlWeights");
            if (w != null && w.length() == Rl.NF + 1) {
                rlWeights = new double[w.length()];
                for (int i = 0; i < w.length(); i++) {
                    rlWeights[i] = w.getDouble(i);
                }
            }
            JSONArray pr = root.optJSONArray("policy");
            for (int i = 0; pr != null && i < pr.length(); i++) {
                JSONObject j = pr.getJSONObject(i);
                PolicyRow p = new PolicyRow();
                p.name = j.getString("n");
                p.mean = j.getDouble("mean");
                p.dist = j.getDouble("dist");
                p.late = j.getDouble("late");
                p.asg = j.getDouble("asg");
                policyRows.add(p);
            }
        } catch (JSONException e) {
            // Corrupt data: start clean rather than crash.
            orders.clear();
            batches.clear();
            runs.clear();
            policyRows.clear();
            rlWeights = null;
            nextOrderId = 1;
            nextBatchId = 1;
            resetRiders();
        }
    }
}
