package com.example.microfleetapp.data;

/** Business numbers shown on the dashboard, derived from the store. */
public final class Stats {

    /** Assumed rates used only to express savings in money; shown on screen as assumptions. */
    public static final double RATE_PER_KM = 8, FEE_PER_TRIP = 25;

    public int ordersTotal, delivered, onTime, open, cancelled, pending;
    public int plannedOrders, plannedTrips;
    public double pooledKm, soloKm, avgDeliveryMin;

    public int tripsSaved() {
        return plannedOrders - plannedTrips;
    }

    public double kmSaved() {
        return soloKm - pooledKm;
    }

    public double kmSavedPct() {
        return soloKm > 0 ? 100 * (soloKm - pooledKm) / soloKm : 0;
    }

    public double costPooled() {
        return plannedTrips * FEE_PER_TRIP + pooledKm * RATE_PER_KM;
    }

    public double costSolo() {
        return plannedOrders * FEE_PER_TRIP + soloKm * RATE_PER_KM;
    }

    public double costSavedPct() {
        double s = costSolo();
        return s > 0 ? 100 * (s - costPooled()) / s : 0;
    }

    public double onTimePct() {
        return delivered > 0 ? 100.0 * onTime / delivered : 0;
    }

    public static Stats compute(Store s) {
        Stats st = new Stats();
        double totalMin = 0;
        for (Store.OrderRec o : s.orders) {
            if (o.status.equals(Store.CANCELLED)) {
                st.cancelled++;
                continue;
            }
            st.ordersTotal++;
            if (o.status.equals(Store.PENDING)) {
                st.pending++;
            }
            if (o.isOpen()) {
                st.open++;
            }
            if (o.status.equals(Store.DELIVERED)) {
                st.delivered++;
                if (o.deliveredAt <= o.deadlineAt) {
                    st.onTime++;
                }
                totalMin += (o.deliveredAt - o.createdAt) / 60000.0;
            }
        }
        st.avgDeliveryMin = st.delivered > 0 ? totalMin / st.delivered : 0;
        for (Store.RunRec r : s.runs) {
            st.plannedOrders += r.orders;
            st.plannedTrips += r.trips;
            st.pooledKm += r.pooledKm;
            st.soloKm += r.soloKm;
        }
        return st;
    }

    private Stats() { }
}
