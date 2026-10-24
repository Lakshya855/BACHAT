package com.example.bachat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Real State Electricity Regulatory Commission (SERC) and Discom Time-of-Day (ToD)
 * Tariff Schedules across Indian States.
 *
 * Sourced from:
 * - Ministry of Power (Electricity Rights of Consumers Amendment Rules)
 * - MERC (Maharashtra Electricity Regulatory Commission - MSEDCL)
 * - DERC (Delhi Electricity Regulatory Commission - BSES/TPDDL)
 * - KERC (Karnataka Electricity Regulatory Commission - BESCOM)
 * - TNERC (Tamil Nadu Generation and Distribution - TANGEDCO)
 * - GERC (Gujarat Electricity Regulatory Commission - GUVNL)
 * - UPERC (Uttar Pradesh Power Corporation Limited - UPPCL)
 * - TGERC / APERC (Telangana & Andhra Pradesh Discoms)
 * - WBERC (West Bengal - WBSEDCL / CESC)
 * - RERC (Rajasthan - JVVNL)
 * - KSERC (Kerala State Electricity Board - KSEB)
 * - PSERC / HERC (Punjab & Haryana Discoms)
 */
public class StateToDSchedule {

    public final String stateName;
    public final String discomAuthority;
    public final String peakHours;
    public final String peakSurcharge;
    public final String offPeakHours;
    public final String offPeakIncentive;
    public final String summaryNote;
    public final int peakStartHour; // 24-hr format
    public final int peakEndHour;

    public StateToDSchedule(
            String stateName,
            String discomAuthority,
            String peakHours,
            String peakSurcharge,
            String offPeakHours,
            String offPeakIncentive,
            String summaryNote,
            int peakStartHour,
            int peakEndHour
    ) {
        this.stateName = stateName;
        this.discomAuthority = discomAuthority;
        this.peakHours = peakHours;
        this.peakSurcharge = peakSurcharge;
        this.offPeakHours = offPeakHours;
        this.offPeakIncentive = offPeakIncentive;
        this.summaryNote = summaryNote;
        this.peakStartHour = peakStartHour;
        this.peakEndHour = peakEndHour;
    }

    private static final Map<String, StateToDSchedule> STATE_SCHEDULES = new HashMap<>();

    static {
        // Maharashtra (MERC / MSEDCL)
        register(new StateToDSchedule(
                "Maharashtra",
                "MSEDCL / MERC Tariff Schedule",
                "6:00 PM – 10:00 PM",
                "+20% TARIFF (+₹1.10/u)",
                "10:00 PM – 6:00 AM",
                "-15% NIGHT REBATE (-₹1.50/u)",
                "MSEDCL levies a steep peak surcharge during evening hours (18:00–22:00) and offers substantial night discounts for overnight loads.",
                18, 22
        ));

        // Karnataka (KERC / BESCOM)
        register(new StateToDSchedule(
                "Karnataka",
                "BESCOM / KERC ToD Schedule",
                "6:00 PM – 10:00 PM",
                "+20% TARIFF (+₹1.00/u)",
                "10:00 PM – 6:00 AM",
                "-₹1.00/u OFF-PEAK REBATE",
                "KERC mandates peak hour surcharges in the evening, with standard rebates for running loads between 10 PM and 6 AM.",
                18, 22
        ));

        // Delhi (DERC / BSES / TPDDL)
        register(new StateToDSchedule(
                "Delhi",
                "DERC (BSES Rajdhani/Yamuna & TPDDL)",
                "2:00 PM – 5:00 PM & 10 PM – 1 AM",
                "+20% SURCHARGE",
                "9:00 AM – 2:00 PM (Solar)",
                "-20% SOLAR REBATE",
                "Delhi operates a dual peak window during high AC cooling afternoon/late night spikes, with solar rebates during midday.",
                14, 17
        ));

        // Tamil Nadu (TNERC / TANGEDCO)
        register(new StateToDSchedule(
                "Tamil Nadu",
                "TANGEDCO / TNERC Schedule",
                "6:00 PM – 9:00 PM (Evening)",
                "+25% TARIFF SURCHARGE",
                "10:00 PM – 5:00 AM",
                "-5% TO -10% NIGHT REBATE",
                "TANGEDCO imposes an evening peak surcharge (+25%) from 6 PM to 9 PM, with an additional morning slot (6 AM–9 AM).",
                18, 21
        ));

        // Gujarat (GERC / GUVNL - DGVCL, MGVCL, PGVCL, UGVCL)
        register(new StateToDSchedule(
                "Gujarat",
                "GUVNL / GERC ToD Framework",
                "7:00 AM – 11:00 AM & 6 PM – 10 PM",
                "+₹0.85/u SURCHARGE",
                "10:00 PM – 6:00 AM",
                "-₹0.40/u NIGHT DISCOUNT",
                "Gujarat features dual morning and evening peak slots with night incentives for continuous operation.",
                18, 22
        ));

        // Uttar Pradesh (UPERC / UPPCL)
        register(new StateToDSchedule(
                "Uttar Pradesh",
                "UPPCL / UPERC Tariff Schedule",
                "5:00 PM – 11:00 PM",
                "+15% TARIFF SURCHARGE",
                "11:00 PM – 6:00 AM",
                "-15% NIGHT REBATE",
                "UPPCL spans an extended 6-hour evening peak window (17:00–23:00) during summer/monsoon grid load spikes.",
                17, 23
        ));

        // Telangana (TGERC / TSSPDCL / TSNPDCL)
        register(new StateToDSchedule(
                "Telangana",
                "TSSPDCL / TSNPDCL ToD Rules",
                "6:00 AM – 10:00 AM & 6 PM – 10 PM",
                "+₹1.00/u SURCHARGE",
                "10:00 PM – 6:00 AM",
                "-₹1.00/u NIGHT REBATE",
                "Telangana enforces a morning and evening dual peak cycle, encouraging heavy agricultural & home loads at night.",
                18, 22
        ));

        // Andhra Pradesh (APERC / APCPDCL / APSEB)
        register(new StateToDSchedule(
                "Andhra Pradesh",
                "APERC / APCPDCL ToD Schedule",
                "6:00 AM – 10:00 AM & 6 PM – 10 PM",
                "+₹1.00/u SURCHARGE",
                "10:00 PM – 6:00 AM",
                "-₹1.00/u NIGHT REBATE",
                "Andhra Pradesh penalizes morning and evening grid demand surges, rewarding night-time consumption.",
                18, 22
        ));

        // West Bengal (WBERC / WBSEDCL / CESC)
        register(new StateToDSchedule(
                "West Bengal",
                "WBSEDCL / CESC Kolkata Schedule",
                "5:00 PM – 11:00 PM",
                "+20% TARIFF SURCHARGE",
                "11:00 PM – 6:00 AM",
                "-10% OFF-PEAK REBATE",
                "CESC & WBSEDCL implement a long evening peak (17:00–23:00) with off-peak discounts overnight.",
                17, 23
        ));

        // Rajasthan (RERC / JVVNL / AVVNL)
        register(new StateToDSchedule(
                "Rajasthan",
                "JVVNL / RERC ToD Tariff",
                "6:00 PM – 10:00 PM",
                "+15% TARIFF SURCHARGE",
                "10:00 PM – 6:00 AM",
                "-10% NIGHT REBATE",
                "Rajasthan discoms penalize post-sunset heating/cooling loads, offering rebates from 10 PM onwards.",
                18, 22
        ));

        // Kerala (KSERC / KSEB)
        register(new StateToDSchedule(
                "Kerala",
                "KSEB / KSERC ToD Framework",
                "6:00 PM – 10:00 PM",
                "+20% TARIFF SURCHARGE",
                "10:00 PM – 6:00 AM",
                "-10% NIGHT REBATE",
                "KSEB domestic ToD meters charge 120% during 18:00–22:00 peak hours and 90% during 22:00–06:00 off-peak.",
                18, 22
        ));

        // Punjab (PSERC / PSPCL)
        register(new StateToDSchedule(
                "Punjab",
                "PSPCL / PSERC Tariff Schedule",
                "6:00 PM – 10:00 PM",
                "+20% TARIFF SURCHARGE",
                "10:00 PM – 6:00 AM",
                "-20% NIGHT REBATE",
                "PSPCL provides heavy night incentives (-20%) to shift agricultural pumps and domestic heavy machinery away from evening peaks.",
                18, 22
        ));

        // Haryana (HERC / DHBVN / UHBVN)
        register(new StateToDSchedule(
                "Haryana",
                "DHBVN / UHBVN ToD Rules",
                "6:00 PM – 10:00 PM",
                "+20% TARIFF SURCHARGE",
                "10:00 PM – 6:00 AM",
                "-15% NIGHT REBATE",
                "Haryana discoms impose evening peak surcharges to manage high NCR air conditioning and industrial demand.",
                18, 22
        ));

        // Madhya Pradesh (MPPKVVCL)
        register(new StateToDSchedule(
                "Madhya Pradesh",
                "MPPKVVCL / MPERC Rules",
                "6:00 PM – 10:00 PM",
                "+15% TARIFF SURCHARGE",
                "10:00 PM – 6:00 AM",
                "-10% NIGHT REBATE",
                "Central Discom of MP enforces evening peak conservation with off-peak incentives.",
                18, 22
        ));

        // Bihar (BERC / NBPDCL / SBPDCL)
        register(new StateToDSchedule(
                "Bihar",
                "NBPDCL / SBPDCL Schedule",
                "5:00 PM – 11:00 PM",
                "+15% TARIFF SURCHARGE",
                "11:00 PM – 6:00 AM",
                "-10% NIGHT REBATE",
                "Bihar state discoms operate evening peak billing during heavy residential illumination and pump usage.",
                17, 23
        ));
    }

    private static void register(StateToDSchedule schedule) {
        STATE_SCHEDULES.put(schedule.stateName.toLowerCase(Locale.ROOT), schedule);
    }

    /**
     * Resolves the official ToD schedule for the given Indian state and DISCOM provider.
     */
    public static StateToDSchedule getSchedule(String stateName, String discomQuery) {
        if (discomQuery != null && !discomQuery.trim().isEmpty()) {
            DiscomProvider.Discom discom = DiscomProvider.findDiscom(stateName, discomQuery);
            if (discom != null) {
                return new StateToDSchedule(
                        discom.state,
                        discom.name + " (" + discom.coverageArea + ")",
                        discom.peakHours,
                        discom.peakSurcharge,
                        discom.offPeakHours,
                        discom.offPeakIncentive,
                        discom.summaryNote,
                        discom.peakStartHour,
                        discom.peakEndHour
                );
            }
        }
        return getSchedule(stateName);
    }

    /**
     * Resolves the official ToD schedule for the given Indian state.
     * If unlisted, returns the national Ministry of Power / CEA model baseline.
     */
    public static StateToDSchedule getSchedule(String stateName) {
        if (stateName != null) {
            String key = stateName.trim().toLowerCase(Locale.ROOT);
            for (Map.Entry<String, StateToDSchedule> entry : STATE_SCHEDULES.entrySet()) {
                if (key.contains(entry.getKey()) || entry.getKey().contains(key)) {
                    return entry.getValue();
                }
            }
        }

        // National Standard / Ministry of Power Model Baseline
        return new StateToDSchedule(
                (stateName != null && !stateName.isEmpty()) ? stateName : "National Standard",
                "Central Electricity Regulatory Commission (CERC / CEA)",
                "6:00 PM – 10:00 PM",
                "+20% PEAK SURCHARGE",
                "10:00 PM – 6:00 AM",
                "-15% OFF-PEAK REBATE",
                "Standard Indian grid peak demand occurs during evening hours. Defer heavy loads to off-peak slots to avoid tariff surcharges.",
                18, 22
        );
    }
}
