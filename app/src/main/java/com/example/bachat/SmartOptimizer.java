package com.example.bachat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SmartOptimizer {

    public static class ApplianceInput {
        public long id;
        public String name;
        public int quantity;
        public double wattage; // Watts
        public double currentHours; // S_max requested
        public String environmentName;

        public ApplianceInput(long id, String name, int quantity, double wattage, double currentHours, String environmentName) {
            this.id = id;
            this.name = name;
            this.quantity = Math.max(1, quantity);
            this.wattage = Math.max(1.0, wattage);
            this.currentHours = Math.max(0.1, currentHours);
            this.environmentName = environmentName != null ? environmentName : "Home";
        }
    }

    public static class ApplianceScheduleItem {
        public long id;
        public String name;
        public String environmentName;
        public int quantity;
        public double wattage; // Watts
        public double S_max; // Maximum requested hours (S_max)
        public double originalHours; // Alias
        public double recommendedHours; // S_k (Allocated hours to run)
        public double allocatedHours; // Alias
        public double gamma_k; // Priority factor γ_k
        public double b_k; // Importance b_k = γ_k / S_max
        public double P_k; // Average power in kW = (wattage * quantity) / 1000
        public double W_k; // Cost to run appliance for 1 hour = alpha * P_k
        public double R_k; // Benefit-to-Cost Ratio = b_k / W_k
        public int rank; // Rank index in greedy sort
        public double costContribution; // alpha * P_k * S_k
        public double psfContribution; // gamma_k * (S_k / S_max)
        public double originalDailyKwh; // P_k * S_max
        public double recommendedDailyKwh; // P_k * S_k
        public double originalDailyCost; // alpha * P_k * S_max
        public double recommendedDailyCost; // costContribution
        public double dailyCostSavings;
        public double monthlyCostSavings;
        public double dailyKwhSavings;
        public double monthlyKwhSavings;
        public String priorityBadge; // User-friendly priority label
        public String statusNote;
        public String beeLabel;
        public double priorityScore; // Raw evaluation score
        public boolean isEssential;
        public int iconRes;
        public int colorRes;
    }

    public static class OptimizationResult {
        public String savingsMode; // "daily" or "monthly"
        public double targetDailySavings; // Desired savings goal (₹/day)
        public double targetMonthlySavings; // Desired savings goal (₹/month)
        public double targetDailyBudget; // Resulting wallet budget Z (₹/day)
        public double targetMonthlyBudget; // Z * 30 (₹/month)
        public double alpha; // Electricity rate (₹/kWh)
        public double totalCost; // Total cost allocated = sum(Cost_Contribution)
        public double totalPsf; // Objective function = sum(PSF_Contribution)
        public double psfPercentage; // Total PSF as a percentage
        public double currentDailyCost;
        public double currentMonthlyCost;
        public double optimizedDailyCost;
        public double optimizedMonthlyCost;
        public double currentDailyKwh;
        public double optimizedDailyKwh;
        public double dailySavingsMoney;
        public double monthlySavingsMoney;
        public double dailySavingsKwh;
        public double monthlySavingsKwh;
        public boolean isWithinBudgetAlready;
        public List<ApplianceScheduleItem> schedules = new ArrayList<>();
    }

    /**
     * Primary optimization entry point:
     * User enters the MONEY THEY WANT TO SAVE per day or per month.
     * Computes optimal appliance hours needed to achieve that exact savings.
     */
    public static OptimizationResult optimizeForSavings(
            List<ApplianceInput> appliances,
            double targetSavings,
            boolean isMonthlyMode,
            double tariffRate,
            String userState
    ) {
        if (tariffRate <= 0) tariffRate = 8.0;

        // Calculate unconstrained daily cost
        double currentDailyCost = 0.0;
        if (appliances != null) {
            for (ApplianceInput app : appliances) {
                double pk = (app.wattage * app.quantity) / 1000.0;
                currentDailyCost += (tariffRate * pk * app.currentHours);
            }
        }

        double desiredDailySavings;
        if (isMonthlyMode) {
            desiredDailySavings = targetSavings / 30.0;
        } else {
            desiredDailySavings = targetSavings;
        }

        // Resulting wallet budget Z
        double targetDailyBudget = Math.max(0.0, currentDailyCost - desiredDailySavings);

        OptimizationResult res = optimize(appliances, targetDailyBudget, isMonthlyMode, tariffRate, userState);
        res.savingsMode = isMonthlyMode ? "monthly" : "daily";
        res.targetDailySavings = desiredDailySavings;
        res.targetMonthlySavings = desiredDailySavings * 30.0;
        return res;
    }

    public static OptimizationResult optimize(List<ApplianceInput> appliances, double targetDailyBudget, boolean isMonthlyMode, double tariffRate) {
        return optimizeWithCustomPriorities(appliances, null, targetDailyBudget, isMonthlyMode, tariffRate, "Maharashtra");
    }

    public static OptimizationResult optimize(List<ApplianceInput> appliances, double targetDailyBudget, boolean isMonthlyMode, double tariffRate, String userState) {
        return optimizeWithCustomPriorities(appliances, null, targetDailyBudget, isMonthlyMode, tariffRate, userState);
    }

    /**
     * Core Knapsack Optimization Formulation from OT_Paper.html:
     * Priority factors and benefit ratios are evaluated automatically behind the scenes.
     */
    public static OptimizationResult optimizeWithCustomPriorities(
            List<ApplianceInput> appliances,
            List<Double> customGammas,
            double targetDailyBudget,
            boolean isMonthlyMode,
            double tariffRate,
            String userState
    ) {
        OptimizationResult result = new OptimizationResult();
        result.savingsMode = isMonthlyMode ? "monthly" : "daily";
        result.targetDailyBudget = Math.max(0.0, targetDailyBudget);
        result.targetMonthlyBudget = result.targetDailyBudget * 30.0;
        if (tariffRate <= 0) tariffRate = 8.0;
        result.alpha = tariffRate;

        if (appliances == null || appliances.isEmpty()) {
            return result;
        }

        // Step 1: Pre-evaluate appliances and compute priority scores automatically
        double sumRawScores = 0.0;
        List<AppliancePriorityEngine.PriorityEvaluation> evals = new ArrayList<>();
        for (ApplianceInput app : appliances) {
            AppliancePriorityEngine.PriorityEvaluation eval =
                    AppliancePriorityEngine.evaluateCurrent(app.name, app.wattage, userState, app.environmentName);
            evals.add(eval);
            sumRawScores += Math.max(0.01, eval.finalPriorityScore);
        }

        // Step 2: Build appliance items with gamma_k, S_max, b_k, P_k, W_k, R_k
        List<ApplianceScheduleItem> items = new ArrayList<>();
        double totalUnconstrainedDailyCost = 0.0;
        double totalUnconstrainedDailyKwh = 0.0;

        for (int i = 0; i < appliances.size(); i++) {
            ApplianceInput app = appliances.get(i);
            AppliancePriorityEngine.PriorityEvaluation eval = evals.get(i);

            ApplianceScheduleItem item = new ApplianceScheduleItem();
            item.id = app.id;
            item.name = app.name;
            item.environmentName = app.environmentName;
            item.quantity = app.quantity;
            item.wattage = app.wattage;

            boolean isContinuous = isContinuousEssential(app.name) && (customGammas == null);
            if (isContinuous) {
                item.S_max = Math.max(24.0, app.currentHours);
                item.isEssential = true;
            } else {
                item.S_max = Math.max(0.1, app.currentHours);
                item.isEssential = eval.basePriority >= 10.0;
            }
            item.originalHours = item.S_max;

            // Normalized priority factor γ_k such that sum(γ_k) == 1.0 (or custom if provided)
            if (customGammas != null && customGammas.size() == appliances.size()) {
                item.gamma_k = customGammas.get(i);
            } else {
                item.gamma_k = (sumRawScores > 0) ? (eval.finalPriorityScore / sumRawScores) : (1.0 / appliances.size());
            }
            item.priorityScore = eval.finalPriorityScore;

            // Importance b_k = γ_k / S_max
            item.b_k = item.gamma_k / item.S_max;

            // Power in kW: P_k = (wattage * quantity) / 1000.0
            item.P_k = Math.max(0.001, (app.wattage * app.quantity) / 1000.0);

            // Cost for 1 hour: W_k = α * P_k
            item.W_k = tariffRate * item.P_k;

            // Benefit Ratio: R_k = b_k / W_k
            item.R_k = item.b_k / Math.max(1e-6, item.W_k);

            item.beeLabel = eval.beeLabel;
            item.isEssential = eval.basePriority >= 10.0;
            assignVisualIconAndColor(item);

            item.originalDailyKwh = item.P_k * item.S_max;
            item.originalDailyCost = tariffRate * item.originalDailyKwh;

            totalUnconstrainedDailyCost += item.originalDailyCost;
            totalUnconstrainedDailyKwh += item.originalDailyKwh;

            items.add(item);
        }

        result.currentDailyCost = totalUnconstrainedDailyCost;
        result.currentMonthlyCost = totalUnconstrainedDailyCost * 30.0;
        result.currentDailyKwh = totalUnconstrainedDailyKwh;

        double walletZ = result.targetDailyBudget;

        // Check if unconstrained cost already fits within target
        if (totalUnconstrainedDailyCost <= walletZ) {
            result.isWithinBudgetAlready = true;
            result.optimizedDailyCost = totalUnconstrainedDailyCost;
            result.optimizedMonthlyCost = result.currentMonthlyCost;
            result.optimizedDailyKwh = totalUnconstrainedDailyKwh;
            result.dailySavingsMoney = Math.max(0, walletZ - totalUnconstrainedDailyCost);
            result.monthlySavingsMoney = result.dailySavingsMoney * 30.0;
            result.dailySavingsKwh = 0.0;
            result.monthlySavingsKwh = 0.0;
            result.totalCost = totalUnconstrainedDailyCost;
            result.totalPsf = 1.0;
            result.psfPercentage = 100.0;

            Collections.sort(items, (a, b) -> Double.compare(b.R_k, a.R_k));

            for (int i = 0; i < items.size(); i++) {
                ApplianceScheduleItem item = items.get(i);
                item.rank = i + 1;
                item.allocatedHours = item.S_max;
                item.recommendedHours = item.S_max;
                item.costContribution = item.originalDailyCost;
                item.psfContribution = item.gamma_k;
                item.recommendedDailyCost = item.costContribution;
                item.recommendedDailyKwh = item.originalDailyKwh;
                item.dailyCostSavings = 0.0;
                item.monthlyCostSavings = 0.0;
                item.dailyKwhSavings = 0.0;
                item.monthlyKwhSavings = 0.0;
                if (isContinuousEssential(item.name) && customGammas == null) {
                    item.priorityBadge = "ESSENTIAL BASELINE";
                    item.statusNote = "Continuous 24h baseline · Maintained for food preservation";
                } else {
                    item.priorityBadge = resolvePriorityBadge(item, item.rank);
                    item.statusNote = "Run full requested hours · Already within target";
                }
                result.schedules.add(item);
            }
            return result;
        }

        // Step 3 & 4: Knapsack Allocation against wallet budget Z
        result.isWithinBudgetAlready = false;

        boolean hasContinuous = false;
        if (customGammas == null) {
            for (ApplianceScheduleItem it : items) {
                if (isContinuousEssential(it.name)) {
                    hasContinuous = true;
                    break;
                }
            }
        }

        if (customGammas == null && hasContinuous) {
            List<ApplianceScheduleItem> continuousItems = new ArrayList<>();
            List<ApplianceScheduleItem> flexibleItems = new ArrayList<>();

            for (ApplianceScheduleItem item : items) {
                if (isContinuousEssential(item.name)) {
                    continuousItems.add(item);
                } else {
                    flexibleItems.add(item);
                }
            }

            // Continuous baseline appliances (e.g. Refrigerators) must ALWAYS run 24h continuously
            double baselineCost = 0.0;
            for (ApplianceScheduleItem item : continuousItems) {
                item.allocatedHours = item.S_max;
                item.recommendedHours = item.S_max;
                item.priorityBadge = "ESSENTIAL BASELINE";
                item.statusNote = "Continuous 24h baseline · Maintained for food preservation";
                baselineCost += tariffRate * item.P_k * item.S_max;
            }

            // Sort flexible items descending by BENEFIT_RATIO (R_k)
            Collections.sort(flexibleItems, (a, b) -> Double.compare(b.R_k, a.R_k));

            // Greedy knapsack on flexible items with remaining budget
            double walletForFlexible = Math.max(0.0, walletZ - baselineCost);
            double flexPaisa = 0.0;

            for (int i = 0; i < flexibleItems.size(); i++) {
                ApplianceScheduleItem item = flexibleItems.get(i);
                item.priorityBadge = resolvePriorityBadge(item, i + 1);
                double cost = tariffRate * item.P_k * item.S_max;

                if ((flexPaisa + cost) <= walletForFlexible) {
                    flexPaisa += cost;
                    item.allocatedHours = item.S_max;
                    item.recommendedHours = item.S_max;
                    item.statusNote = "Run full requested hours";
                } else {
                    double remainingPaisa = Math.max(0.0, walletForFlexible - flexPaisa);
                    double S = remainingPaisa / (tariffRate * item.P_k);
                    S = Math.min(item.S_max, Math.max(0.0, S));
                    item.allocatedHours = S;
                    item.recommendedHours = S;
                    flexPaisa = walletForFlexible;

                    if (S > 0) {
                        double cut = item.S_max - S;
                        item.statusNote = String.format(Locale.getDefault(), "Trim runtime by %.1f hrs to hit savings target", cut);
                    } else {
                        item.statusNote = "Turn off / minimize usage to hit savings target";
                    }

                    // All remaining flexible appliances receive 0 allocated hours
                    for (int j = i + 1; j < flexibleItems.size(); j++) {
                        ApplianceScheduleItem remainingItem = flexibleItems.get(j);
                        remainingItem.priorityBadge = resolvePriorityBadge(remainingItem, j + 1);
                        remainingItem.allocatedHours = 0.0;
                        remainingItem.recommendedHours = 0.0;
                        remainingItem.statusNote = "Turn off during savings cycle";
                    }
                    break;
                }
            }

            items.clear();
            int rankCounter = 1;
            for (ApplianceScheduleItem item : continuousItems) {
                item.rank = rankCounter++;
                items.add(item);
            }
            for (ApplianceScheduleItem item : flexibleItems) {
                item.rank = rankCounter++;
                items.add(item);
            }
        } else {
            // Standard Greedy Knapsack Allocation
            Collections.sort(items, (a, b) -> Double.compare(b.R_k, a.R_k));
            double paisa = 0.0;

            for (int i = 0; i < items.size(); i++) {
                ApplianceScheduleItem item = items.get(i);
                item.rank = i + 1;
                item.priorityBadge = resolvePriorityBadge(item, item.rank);
                double cost = tariffRate * item.P_k * item.S_max;

                if ((paisa + cost) <= walletZ) {
                    paisa += cost;
                    item.allocatedHours = item.S_max;
                    item.recommendedHours = item.S_max;
                    item.statusNote = "Run full requested hours";
                } else {
                    double remainingPaisa = Math.max(0.0, walletZ - paisa);
                    double S = remainingPaisa / (tariffRate * item.P_k);
                    S = Math.min(item.S_max, Math.max(0.0, S));
                    item.allocatedHours = S;
                    item.recommendedHours = S;
                    paisa = walletZ;

                    if (S > 0) {
                        double cut = item.S_max - S;
                        item.statusNote = String.format(Locale.getDefault(), "Trim runtime by %.1f hrs to hit savings target", cut);
                    } else {
                        item.statusNote = "Turn off / minimize usage to hit savings target";
                    }

                    // All remaining appliances receive 0 allocated hours
                    for (int j = i + 1; j < items.size(); j++) {
                        ApplianceScheduleItem remainingItem = items.get(j);
                        remainingItem.rank = j + 1;
                        remainingItem.priorityBadge = resolvePriorityBadge(remainingItem, remainingItem.rank);
                        remainingItem.allocatedHours = 0.0;
                        remainingItem.recommendedHours = 0.0;
                        remainingItem.statusNote = "Turn off during savings cycle";
                    }
                    break;
                }
            }
        }

        // Step 5: Compute Cost_Contribution, PSF_Contribution, and Savings
        double totalCost = 0.0;
        double totalPsf = 0.0;
        double totalOptimizedKwh = 0.0;

        for (ApplianceScheduleItem item : items) {
            item.costContribution = tariffRate * item.P_k * item.allocatedHours;
            item.psfContribution = item.gamma_k * (item.allocatedHours / item.S_max);

            item.recommendedDailyCost = item.costContribution;
            item.recommendedDailyKwh = item.P_k * item.allocatedHours;

            item.dailyCostSavings = Math.max(0.0, item.originalDailyCost - item.recommendedDailyCost);
            item.monthlyCostSavings = item.dailyCostSavings * 30.0;

            item.dailyKwhSavings = Math.max(0.0, item.originalDailyKwh - item.recommendedDailyKwh);
            item.monthlyKwhSavings = item.dailyKwhSavings * 30.0;

            totalCost += item.costContribution;
            totalPsf += item.psfContribution;
            totalOptimizedKwh += item.recommendedDailyKwh;

            result.schedules.add(item);
        }

        result.totalCost = totalCost;
        result.totalPsf = totalPsf;
        result.psfPercentage = Math.min(100.0, Math.max(0.0, totalPsf * 100.0));

        result.optimizedDailyCost = totalCost;
        result.optimizedMonthlyCost = totalCost * 30.0;
        result.optimizedDailyKwh = totalOptimizedKwh;

        result.dailySavingsMoney = Math.max(0.0, result.currentDailyCost - result.optimizedDailyCost);
        result.monthlySavingsMoney = result.dailySavingsMoney * 30.0;

        result.dailySavingsKwh = Math.max(0.0, result.currentDailyKwh - result.optimizedDailyKwh);
        result.monthlySavingsKwh = result.dailySavingsKwh * 30.0;

        return result;
    }

    private static String resolvePriorityBadge(ApplianceScheduleItem item, int rank) {
        if (item.isEssential) {
            return "ESSENTIAL LOAD";
        }
        if (rank <= 2) {
            return "HIGH PRIORITY";
        }
        if (rank <= 4) {
            return "MEDIUM PRIORITY";
        }
        return "FLEXIBLE LOAD";
    }

    private static void assignVisualIconAndColor(ApplianceScheduleItem item) {
        String n = item.name.toLowerCase(Locale.ROOT);
        if (n.contains("fan")) {
            item.iconRes = android.R.drawable.ic_menu_rotate;
            item.colorRes = R.color.icon_fan;
        } else if (n.contains("ac") || n.contains("air") || n.contains("cooler")) {
            item.iconRes = android.R.drawable.ic_menu_sort_by_size;
            item.colorRes = R.color.icon_ac;
        } else if (n.contains("fridge") || n.contains("refrigerator") || n.contains("freezer") || n.contains("chiller") || n.contains("double door") || n.contains("single door")) {
            item.iconRes = android.R.drawable.ic_menu_directions;
            item.colorRes = R.color.icon_fridge;
        } else if (n.contains("light") || n.contains("bulb") || n.contains("tube") || n.contains("lamp")) {
            item.iconRes = android.R.drawable.ic_menu_day;
            item.colorRes = R.color.icon_light;
        } else if (n.contains("tv") || n.contains("screen") || n.contains("oled") || n.contains("qled")) {
            item.iconRes = android.R.drawable.ic_menu_slideshow;
            item.colorRes = R.color.icon_tv;
        } else if (n.contains("wash")) {
            item.iconRes = android.R.drawable.ic_menu_compass;
            item.colorRes = R.color.icon_washing;
        } else if (n.contains("pc") || n.contains("computer") || n.contains("laptop")) {
            item.iconRes = android.R.drawable.ic_menu_call;
            item.colorRes = R.color.icon_computer;
        } else {
            item.iconRes = android.R.drawable.ic_menu_agenda;
            item.colorRes = R.color.icon_default;
        }
    }

    public static boolean isContinuousEssential(String name) {
        if (name == null) return false;
        String n = name.toLowerCase(Locale.ROOT);
        return n.contains("fridge") || n.contains("refrigerator") || n.contains("freezer") ||
                n.contains("chiller") || n.contains("double door") || n.contains("single door") ||
                n.contains("frost free") || n.contains("direct cool");
    }
}
