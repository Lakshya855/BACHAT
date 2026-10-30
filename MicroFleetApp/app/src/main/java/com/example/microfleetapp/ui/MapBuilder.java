package com.example.microfleetapp.ui;

import com.example.microfleetapp.data.Catalog;
import com.example.microfleetapp.data.Store;
import com.example.microfleetapp.engine.Dispatch;
import com.example.microfleetapp.engine.Types;

import java.util.ArrayList;
import java.util.List;

/** Turns plans and live trips into what {@link RouteMapView} draws. */
public final class MapBuilder {

    private MapBuilder() { }

    public static int batchColor(int i) {
        return Ui.BATCH_COLORS[i % Ui.BATCH_COLORS.length];
    }

    /** Two-digit label that fits inside a map marker. */
    public static String tag(int orderId) {
        return String.valueOf(orderId % 100);
    }

    public static void show(RouteMapView map, Dispatch.Plan plan, int highlightBatch) {
        List<RouteMapView.Route> routes = new ArrayList<RouteMapView.Route>();
        List<RouteMapView.Mark> pool = new ArrayList<RouteMapView.Mark>();
        List<RouteMapView.Pin> pins = new ArrayList<RouteMapView.Pin>();
        for (int bi = 0; bi < plan.batches.size(); bi++) {
            Dispatch.PlannedBatch pb = plan.batches.get(bi);
            if (pb.riderPos < 0) {
                for (int idx : pb.batch.idxs) {
                    Types.Ord o = plan.orders.get(idx);
                    pool.add(new RouteMapView.Mark(o.pickup, 'P', tag(o.appId), false, false));
                }
                continue;
            }
            RouteMapView.Route r = new RouteMapView.Route();
            r.color = batchColor(bi);
            r.dim = highlightBatch >= 0 && highlightBatch != bi;
            List<Integer> stopNodes = new ArrayList<Integer>();
            for (Types.Stop s : pb.route.stops) {
                stopNodes.add(s.node);
                Types.Ord o = plan.orders.get(s.ev.o);
                r.marks.add(new RouteMapView.Mark(s.node, s.ev.k, tag(o.appId), false, false));
            }
            r.path = RouteMapView.pathThrough(pb.route.startNode, stopNodes);
            routes.add(r);
        }
        for (Dispatch.RiderIn ri : plan.riders) {
            pins.add(new RouteMapView.Pin(ri.node, ri.name, Catalog.RIDERS[ri.ref].color));
        }
        map.setData(routes, pins, pool);
    }

    /** Remaining stops of trips already handed to riders. */
    public static void show(RouteMapView map, Store store, int onlyRider) {
        List<RouteMapView.Route> routes = new ArrayList<RouteMapView.Route>();
        List<RouteMapView.Pin> pins = new ArrayList<RouteMapView.Pin>();
        for (Store.BatchRec b : store.allActiveBatches()) {
            if (onlyRider >= 0 && b.riderIdx != onlyRider) {
                continue;
            }
            RouteMapView.Route r = new RouteMapView.Route();
            r.color = onlyRider >= 0 ? batchColor(routes.size()) : Catalog.RIDERS[b.riderIdx].color;
            r.dim = onlyRider >= 0 && routes.size() > 0;
            List<Integer> stopNodes = new ArrayList<Integer>();
            for (int s = b.done; s < b.stops.size(); s++) {
                Store.StopRec st = b.stops.get(s);
                stopNodes.add(st.node);
                r.marks.add(new RouteMapView.Mark(st.node, st.kind, tag(st.orderId), false, s == b.done));
            }
            int startNode = store.riderNode[b.riderIdx];
            if (routes.size() > 0 && onlyRider >= 0) {
                // Later trips start where the earlier one ends.
                RouteMapView.Route prev = routes.get(routes.size() - 1);
                startNode = prev.path.get(prev.path.size() - 1);
            }
            r.path = RouteMapView.pathThrough(startNode, stopNodes);
            routes.add(r);
        }
        for (Catalog.Rider ri : Catalog.RIDERS) {
            if (onlyRider < 0 || ri.id == onlyRider) {
                pins.add(new RouteMapView.Pin(store.riderNode[ri.id], ri.name, ri.color));
            }
        }
        List<RouteMapView.Mark> pool = new ArrayList<RouteMapView.Mark>();
        if (onlyRider < 0) {
            for (Store.OrderRec o : store.pendingOrders()) {
                pool.add(new RouteMapView.Mark(Catalog.MERCHANTS[o.merchantId].node, 'P', tag(o.id), false, false));
            }
        }
        map.setData(routes, pins, pool);
    }
}
