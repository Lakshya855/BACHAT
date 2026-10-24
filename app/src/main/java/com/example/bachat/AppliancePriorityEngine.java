package com.example.bachat;

import java.util.Calendar;
import java.util.Locale;

public class AppliancePriorityEngine {

    public enum ClimateZone {
        HOT_COMPOSITE, // North & Central India (Extreme summer heat, chilly winter)
        WARM_HUMID,    // South & Coastal India (Year-round high humidity & cooling need)
        COLD_MOUNTAIN  // Himalayan & Hill states (High heating demand, negligible AC)
    }

    public enum TimeSlot {
        MORNING_PEAK,  // 06:00 - 10:00 (Water heating, breakfast load)
        DAY_OFFPEAK,   // 10:00 - 18:00 (Solar surplus, ideal for washing/heavy runs)
        EVENING_SURGE, // 18:00 - 22:00 (Grid Peak +20% ToD Tariff window)
        NIGHT_SLEEP    // 22:00 - 06:00 (Sleep ventilation & bedroom cooling)
    }

    public enum Season {
        SUMMER,  // March - June
        MONSOON, // July - September
        WINTER   // October - February
    }

    public static class PriorityEvaluation {
        public double basePriority;
        public double beeBonus;
        public double stateMultiplier;
        public double areaMultiplier;
        public double timeMultiplier;
        public double finalPriorityScore;
        public double recommendedMinHours;
        public String beeLabel;
        public String rationale;
        public ClimateZone climateZone;
        public TimeSlot timeSlot;
        public Season season;
    }

    /**
     * Evaluates the priority factor of an appliance based on BEE Star Label standards,
     * Indian State / Climate Zone, Area (Environment type), and Time of day.
     */
    public static PriorityEvaluation evaluate(
            String applianceName,
            double wattage,
            String stateName,
            String environmentType,
            int hourOfDay,
            int monthOfYear
    ) {
        PriorityEvaluation eval = new PriorityEvaluation();
        String n = (applianceName != null) ? applianceName.toLowerCase(Locale.ROOT) : "";

        // 1. Resolve State & Climate Zone
        eval.climateZone = resolveClimateZone(stateName);

        // 2. Resolve Season
        eval.season = resolveSeason(monthOfYear);

        // 3. Resolve Time Slot (State-specific ToD peak window)
        eval.timeSlot = resolveTimeSlot(hourOfDay, stateName);

        // 4. Base Essentiality
        eval.basePriority = resolveBasePriority(n);

        // 5. BEE Star Labeling & Technology Tier
        resolveBeeTechnology(n, wattage, eval);

        // 6. State / Climate & Season Multiplier
        eval.stateMultiplier = resolveStateClimateMultiplier(n, eval.climateZone, eval.season);

        // 7. Area / Environment Multiplier
        eval.areaMultiplier = resolveAreaMultiplier(n, environmentType, eval.timeSlot);

        // 8. Time of Day (Time-of-Day ToD Peak Surge) Multiplier
        eval.timeMultiplier = resolveTimeMultiplier(n, eval.timeSlot);

        // Compute final score
        eval.finalPriorityScore = eval.basePriority * eval.beeBonus * eval.stateMultiplier * eval.areaMultiplier * eval.timeMultiplier;

        // Compute recommended minimum hours
        eval.recommendedMinHours = resolveMinHours(n, eval.climateZone, eval.season);

        // Generate context-aware rationale
        eval.rationale = buildRationale(n, eval, stateName, environmentType);

        return eval;
    }

    public static PriorityEvaluation evaluateCurrent(String applianceName, double wattage, String stateName, String environmentType) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int month = cal.get(Calendar.MONTH) + 1; // 1-based (1 = Jan, 12 = Dec)
        return evaluate(applianceName, wattage, stateName, environmentType, hour, month);
    }

    private static ClimateZone resolveClimateZone(String state) {
        if (state == null) return ClimateZone.HOT_COMPOSITE;
        String s = state.trim().toLowerCase(Locale.ROOT);

        if (s.contains("himachal") || s.contains("uttarakhand") || s.contains("kashmir") ||
                s.contains("ladakh") || s.contains("sikkim") || s.contains("arunachal") ||
                s.contains("meghalaya") || s.contains("nagaland") || s.contains("manipur") || s.contains("mizoram")) {
            return ClimateZone.COLD_MOUNTAIN;
        }

        if (s.contains("maharashtra") || s.contains("tamil") || s.contains("kerala") ||
                s.contains("andhra") || s.contains("telangana") || s.contains("karnataka") ||
                s.contains("bengal") || s.contains("odisha") || s.contains("goa") || s.contains("pondicherry")) {
            return ClimateZone.WARM_HUMID;
        }

        // Default: North & Central India
        return ClimateZone.HOT_COMPOSITE;
    }

    private static Season resolveSeason(int month) {
        if (month >= 3 && month <= 6) {
            return Season.SUMMER;
        } else if (month >= 7 && month <= 9) {
            return Season.MONSOON;
        } else {
            return Season.WINTER;
        }
    }

    private static TimeSlot resolveTimeSlot(int hour, String stateName) {
        StateToDSchedule sched = StateToDSchedule.getSchedule(stateName);
        if (hour >= sched.peakStartHour && hour < sched.peakEndHour) {
            return TimeSlot.EVENING_SURGE;
        } else if (hour >= 6 && hour < 10) {
            return TimeSlot.MORNING_PEAK;
        } else if (hour >= 10 && hour < sched.peakStartHour) {
            return TimeSlot.DAY_OFFPEAK;
        } else {
            return TimeSlot.NIGHT_SLEEP;
        }
    }

    private static TimeSlot resolveTimeSlot(int hour) {
        return resolveTimeSlot(hour, null);
    }

    private static double resolveBasePriority(String n) {
        if (SmartOptimizer.isContinuousEssential(n) || n.contains("router") || n.contains("server")) {
            return 12.0; // Essential non-negotiable
        }
        if (n.contains("fan")) {
            return 6.5; // Fundamental thermal comfort
        }
        if (n.contains("light") || n.contains("bulb") || n.contains("tube") || n.contains("batten")) {
            return 5.5; // Visibility
        }
        if (n.contains("computer") || n.contains("pc") || n.contains("laptop")) {
            return 4.5;
        }
        if (n.contains("tv") || n.contains("television") || n.contains("screen")) {
            return 3.5;
        }
        if (n.contains("wash")) {
            return 3.0;
        }
        // Heavy thermal loads: trimmed first to produce massive savings
        if (n.contains("ac") || n.contains("air conditioner") || n.contains("geyser") ||
                n.contains("heater") || n.contains("oven") || n.contains("machinery")) {
            return 2.0;
        }
        return 3.0;
    }

    private static void resolveBeeTechnology(String n, double wattage, PriorityEvaluation eval) {
        if (n.contains("bldc") || n.contains("5-star") || n.contains("5 star") || n.contains("dual inverter")) {
            eval.beeBonus = 1.40;
            eval.beeLabel = "BEE 5-Star · High ISEER/BLDC";
        } else if (n.contains("inverter") || n.contains("3-star") || n.contains("3 star") || n.contains("led")) {
            eval.beeBonus = 1.15;
            eval.beeLabel = "BEE 3-Star · Inverter Tech";
        } else if (n.contains("window ac") || n.contains("non-inverter") || n.contains("induction") ||
                n.contains("fluorescent") || n.contains("filament") || wattage >= 1500) {
            eval.beeBonus = 0.80; // High draw legacy model
            eval.beeLabel = "BEE Conventional · High Draw";
        } else {
            eval.beeBonus = 1.0;
            eval.beeLabel = "BEE Standard Model";
        }
    }

    private static double resolveStateClimateMultiplier(String n, ClimateZone zone, Season season) {
        // Cooling Appliances (AC, Fan, Cooler)
        if (n.contains("ac") || n.contains("air") || n.contains("cooler") || n.contains("fan")) {
            switch (zone) {
                case COLD_MOUNTAIN:
                    return 0.35; // Cooling seldom needed in hill states
                case WARM_HUMID:
                    return 1.35; // Year-round humidity demands continuous ventilation
                case HOT_COMPOSITE:
                    return (season == Season.SUMMER) ? 1.50 : (season == Season.MONSOON ? 1.25 : 0.45);
            }
        }

        // Heating Appliances (Geyser, Water Heater, Room Heater)
        if (n.contains("geyser") || n.contains("water heater") || n.contains("heater") || n.contains("boiler")) {
            switch (zone) {
                case COLD_MOUNTAIN:
                    return 1.95; // Critical heating requirement
                case HOT_COMPOSITE:
                    return (season == Season.WINTER) ? 1.75 : 0.70;
                case WARM_HUMID:
                    return 0.75; // Brief morning shower only
            }
        }

        // Refrigerators (ambient temperature impacts food spoilage)
        if (SmartOptimizer.isContinuousEssential(n)) {
            if (zone == ClimateZone.WARM_HUMID || season == Season.SUMMER) {
                return 1.25;
            }
            if (zone == ClimateZone.COLD_MOUNTAIN) {
                return 0.95;
            }
        }

        return 1.0;
    }

    private static double resolveAreaMultiplier(String n, String environmentType, TimeSlot timeSlot) {
        String env = (environmentType != null) ? environmentType.toLowerCase(Locale.ROOT) : "home";

        if (env.contains("office")) {
            if (n.contains("computer") || n.contains("pc") || n.contains("laptop") || n.contains("printer") || n.contains("router")) {
                return 1.70;
            }
            if (n.contains("ac") || n.contains("light")) {
                return (timeSlot == TimeSlot.DAY_OFFPEAK || timeSlot == TimeSlot.MORNING_PEAK) ? 1.40 : 0.30;
            }
            return 0.70;
        }

        if (env.contains("shop")) {
            if (n.contains("display") || n.contains("cooler") || n.contains("light") || n.contains("billing")) {
                return 1.65;
            }
        }

        if (env.contains("factory")) {
            if (n.contains("machinery") || n.contains("conveyor") || n.contains("industrial") || n.contains("motor")) {
                return 1.80;
            }
        }

        // Default: Home
        return 1.0;
    }

    private static double resolveTimeMultiplier(String n, TimeSlot timeSlot) {
        switch (timeSlot) {
            case EVENING_SURGE:
                // Grid peak surge window (6 PM - 10 PM)
                // Discretionary heavy loads receive a heavy reduction factor to prevent bill shock
                if (n.contains("wash") || n.contains("geyser") || n.contains("water heater") || n.contains("iron") || n.contains("pump")) {
                    return 0.40; // Shift to off-peak / morning
                }
                if (n.contains("light") || n.contains("tv") || n.contains("fan")) {
                    return 1.25; // Active evening family living
                }
                if (n.contains("ac") || n.contains("air")) {
                    return 0.85; // Recommend pre-cooling & eco setpoint
                }
                break;

            case NIGHT_SLEEP:
                // 10 PM - 6 AM
                if (n.contains("fan")) {
                    return 1.45; // Essential sleeping air circulation
                }
                if (n.contains("ac") || n.contains("air")) {
                    return 1.20; // Bedroom comfort
                }
                if (n.contains("tv") || n.contains("pc") || n.contains("computer") || n.contains("printer") || n.contains("light")) {
                    return 0.30;
                }
                break;

            case MORNING_PEAK:
                // 6 AM - 10 AM
                if (n.contains("geyser") || n.contains("water heater") || n.contains("kettle") || n.contains("induction")) {
                    return 1.55; // Morning hygiene & breakfast prep
                }
                break;

            case DAY_OFFPEAK:
                // 10 AM - 6 PM
                if (n.contains("wash") || n.contains("dryer") || n.contains("pump")) {
                    return 1.40; // Best solar / off-peak grid slot
                }
                break;
        }

        return 1.0;
    }

    private static double resolveMinHours(String n, ClimateZone zone, Season season) {
        if (SmartOptimizer.isContinuousEssential(n) || n.contains("router") || n.contains("server")) {
            return 24.0;
        }
        if (n.contains("fan")) {
            return (zone == ClimateZone.COLD_MOUNTAIN) ? 2.0 : 4.0;
        }
        if (n.contains("light") || n.contains("bulb") || n.contains("tube")) {
            return 2.5;
        }
        if (n.contains("ac") || n.contains("air")) {
            if (zone == ClimateZone.COLD_MOUNTAIN || (zone == ClimateZone.HOT_COMPOSITE && season == Season.WINTER)) {
                return 0.0;
            }
            return 1.5;
        }
        if (n.contains("geyser") || n.contains("water heater")) {
            if (zone == ClimateZone.COLD_MOUNTAIN || season == Season.WINTER) {
                return 0.8;
            }
            return 0.4;
        }
        if (n.contains("wash")) {
            return 0.3;
        }
        if (n.contains("tv") || n.contains("gaming")) {
            return 1.0;
        }
        return 1.0;
    }

    private static String buildRationale(String n, PriorityEvaluation eval, String state, String env) {
        if (SmartOptimizer.isContinuousEssential(n)) {
            return "24h Essential · Compressor food preservation cycle maintained";
        }
        if (n.contains("router") || n.contains("server")) {
            return "Continuous 24h network gateway";
        }
        if (eval.timeSlot == TimeSlot.EVENING_SURGE && (n.contains("wash") || n.contains("geyser") || n.contains("pump"))) {
            return "Peak Grid Window (6-10 PM) · Shift heavy load to day off-peak to save +20% ToD surcharge";
        }
        if (n.contains("ac") || n.contains("air")) {
            if (eval.climateZone == ClimateZone.WARM_HUMID) {
                return String.format(Locale.getDefault(), "Coastal/Humid climate in %s · Prioritized for bedtime dehumidification & eco 24°C",
                        state != null && !state.isEmpty() ? state : "State");
            } else if (eval.season == Season.SUMMER) {
                return String.format(Locale.getDefault(), "Summer heat wave in %s · Optimized runtime to maintain room setpoint",
                        state != null && !state.isEmpty() ? state : "State");
            } else {
                return "Discretionary cooling load · Scaled down to prevent budget ceiling rupture";
            }
        }
        if (n.contains("geyser") || n.contains("water heater")) {
            if (eval.season == Season.WINTER || eval.climateZone == ClimateZone.COLD_MOUNTAIN) {
                return "Winter hygiene demand · Allocated concentrated morning heating burst";
            } else {
                return "Warm season · Rapid 0.5h heating cycle is sufficient; eliminate idle heat loss";
            }
        }
        if (n.contains("bldc") || n.contains("5-star")) {
            return "BEE 5-Star Efficiency Bonus · Granted extended duty cycle due to 65% power saving";
        }
        if (n.contains("fan")) {
            return "Primary baseline air circulation protected for daily comfort";
        }
        if (n.contains("light")) {
            return "Essential nighttime illumination";
        }
        return "Standard duty cycle optimized against daily budget ceiling";
    }
}
