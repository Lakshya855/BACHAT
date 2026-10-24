package com.example.bachat;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class SmartOptimizerTest {

    @Test
    public void testExactPaperFormulation_MatchesNotebookOutput() {
        // Reproduces Block 1 to 15 from OT_Paper.html exactly
        List<SmartOptimizer.ApplianceInput> appliances = new ArrayList<>();
        // ID, name, quantity, wattage (Watts), max_hours, environment
        appliances.add(new SmartOptimizer.ApplianceInput(1, "ref", 1, 150.0, 24.0, "Home"));
        appliances.add(new SmartOptimizer.ApplianceInput(2, "air", 1, 1500.0, 2.0, "Home"));
        appliances.add(new SmartOptimizer.ApplianceInput(3, "micr", 1, 1200.0, 0.5, "Home"));
        appliances.add(new SmartOptimizer.ApplianceInput(4, "wash", 1, 500.0, 1.5, "Home"));

        List<Double> paperGammas = Arrays.asList(0.48, 0.24, 0.16, 0.12);
        double tariffRate = 0.16; // alpha in paper
        double walletZ = 0.5; // budget Z in paper

        SmartOptimizer.OptimizationResult result = SmartOptimizer.optimizeWithCustomPriorities(
                appliances,
                paperGammas,
                walletZ,
                false, // daily mode
                tariffRate,
                "Maharashtra"
        );

        assertNotNull(result);
        assertEquals(4, result.schedules.size());

        // Verify sorted order by R_k descending:
        // Rank 1: micr (R = 1.6667)
        SmartOptimizer.ApplianceScheduleItem micr = result.schedules.get(0);
        assertEquals("micr", micr.name);
        assertEquals(1.6667, micr.R_k, 0.001);
        assertEquals(0.5, micr.allocatedHours, 0.001);
        assertEquals(0.096, micr.costContribution, 0.001);
        assertEquals(0.16, micr.psfContribution, 0.001);

        // Rank 2: wash (R = 1.0000)
        SmartOptimizer.ApplianceScheduleItem wash = result.schedules.get(1);
        assertEquals("wash", wash.name);
        assertEquals(1.0, wash.R_k, 0.001);
        assertEquals(1.5, wash.allocatedHours, 0.001);
        assertEquals(0.120, wash.costContribution, 0.001);
        assertEquals(0.12, wash.psfContribution, 0.001);

        // Rank 3: ref (R = 0.8333)
        SmartOptimizer.ApplianceScheduleItem ref = result.schedules.get(2);
        assertEquals("ref", ref.name);
        assertEquals(0.8333, ref.R_k, 0.001);
        assertEquals(11.8333, ref.allocatedHours, 0.001);
        assertEquals(0.284, ref.costContribution, 0.001);
        assertEquals(0.2367, ref.psfContribution, 0.001);

        // Rank 4: air (R = 0.5000)
        SmartOptimizer.ApplianceScheduleItem air = result.schedules.get(3);
        assertEquals("air", air.name);
        assertEquals(0.5, air.R_k, 0.001);
        assertEquals(0.0, air.allocatedHours, 0.001);
        assertEquals(0.0, air.costContribution, 0.001);
        assertEquals(0.0, air.psfContribution, 0.001);

        // Verify total cost and objective PSF
        assertEquals(0.5, result.totalCost, 0.001);
        assertEquals(0.5167, result.totalPsf, 0.001);
    }

    @Test
    public void testAutomatedPriorityEvaluation_GeneratesOptimalSchedule() {
        List<SmartOptimizer.ApplianceInput> list = new ArrayList<>();
        list.add(new SmartOptimizer.ApplianceInput(1, "Refrigerator", 1, 180.0, 24.0, "Home"));
        list.add(new SmartOptimizer.ApplianceInput(2, "BLDC Ceiling Fan", 3, 28.0, 14.0, "Home"));
        list.add(new SmartOptimizer.ApplianceInput(3, "LED Tube Light", 4, 20.0, 6.0, "Home"));
        list.add(new SmartOptimizer.ApplianceInput(4, "Split AC 1.5T", 1, 1450.0, 8.0, "Home"));

        double tariffRate = 8.0;
        double dailyBudget = 80.0; // ₹ 80/day

        SmartOptimizer.OptimizationResult result = SmartOptimizer.optimize(list, dailyBudget, false, tariffRate, "Maharashtra");

        assertNotNull(result);
        assertEquals(4, result.schedules.size());
        assertTrue(result.totalCost <= dailyBudget + 0.01);
        assertTrue(result.totalPsf > 0.0 && result.totalPsf <= 1.0);
        assertTrue(result.dailySavingsMoney >= 0.0);
        assertTrue(result.dailySavingsKwh >= 0.0);
    }

    @Test
    public void testRefrigeratorContinuousOperation_AlwaysKeptAt24Hours() {
        List<SmartOptimizer.ApplianceInput> list = new ArrayList<>();
        // Simulate corrupted refrigerator with 21.0 hours
        list.add(new SmartOptimizer.ApplianceInput(1, "5-Star Inverter Double Door Refrigerator", 1, 150.0, 21.0, "Home"));
        list.add(new SmartOptimizer.ApplianceInput(2, "Split AC 1.5T", 1, 1500.0, 8.0, "Home"));

        double tariffRate = 8.0;
        double tightDailyBudget = 40.0; // ₹ 40/day - barely covers fridge

        SmartOptimizer.OptimizationResult result = SmartOptimizer.optimize(list, tightDailyBudget, false, tariffRate, "Maharashtra");

        assertNotNull(result);
        assertEquals(2, result.schedules.size());

        SmartOptimizer.ApplianceScheduleItem fridge = null;
        for (SmartOptimizer.ApplianceScheduleItem item : result.schedules) {
            if (SmartOptimizer.isContinuousEssential(item.name)) {
                fridge = item;
                break;
            }
        }

        assertNotNull("Refrigerator item must be present", fridge);
        assertEquals("Refrigerator must strictly run 24 hours continuously", 24.0, fridge.allocatedHours, 0.001);
        assertEquals("Refrigerator recommended hours must be 24.0", 24.0, fridge.recommendedHours, 0.001);
        assertEquals("ESSENTIAL BASELINE", fridge.priorityBadge);
        assertTrue(fridge.statusNote.contains("Continuous 24h baseline"));
    }
}
