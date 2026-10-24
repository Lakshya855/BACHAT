package com.example.bachat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PowerUsageDataset {

    public static final List<String> INDIAN_STATES = Arrays.asList(
            "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", "Delhi", "Goa",
            "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala",
            "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland",
            "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana",
            "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal"
    );

    public static class ApplianceModel {
        public String category;
        public String name;
        public double baseWattage;
        public double defaultHours;
        public String badge;

        public ApplianceModel(String category, String name, double baseWattage, double defaultHours, String badge) {
            this.category = category;
            this.name = name;
            this.baseWattage = baseWattage;
            this.defaultHours = defaultHours;
            this.badge = badge;
        }

        public String getDisplayName() {
            return name + " (" + (int) baseWattage + "W)";
        }
    }

    public static class ApplianceCategory {
        public String name;
        public List<ApplianceModel> models;

        public ApplianceCategory(String name, List<ApplianceModel> models) {
            this.name = name;
            this.models = models;
        }
    }

    public static List<ApplianceCategory> getApplianceCatalog() {
        List<ApplianceCategory> catalog = new ArrayList<>();

        // 1. Fans
        List<ApplianceModel> fans = new ArrayList<>();
        fans.add(new ApplianceModel("Fans", "BLDC Smart Fan", 28, 12, "65% Power Saver"));
        fans.add(new ApplianceModel("Fans", "Standard Induction Fan", 75, 12, "Conventional"));
        fans.add(new ApplianceModel("Fans", "High-Speed Induction Fan", 85, 12, "High Airflow"));
        fans.add(new ApplianceModel("Fans", "Pedestal / Table Fan", 55, 8, "Portable"));
        fans.add(new ApplianceModel("Fans", "Exhaust Fan", 40, 4, "Ventilation"));
        fans.add(new ApplianceModel("Fans", "Industrial Heavy Fan", 140, 10, "High Draw"));
        catalog.add(new ApplianceCategory("Fans", fans));

        // 2. Air Conditioners
        List<ApplianceModel> acs = new ArrayList<>();
        acs.add(new ApplianceModel("Air Conditioners", "5-Star Inverter AC 1.5T", 800, 8, "Super Efficient"));
        acs.add(new ApplianceModel("Air Conditioners", "3-Star Inverter AC 1.5T", 1150, 8, "Standard Inverter"));
        acs.add(new ApplianceModel("Air Conditioners", "3-Star Non-Inverter Split AC", 1450, 8, "Fixed Speed"));
        acs.add(new ApplianceModel("Air Conditioners", "Window AC 1.5T", 1650, 8, "High Draw"));
        acs.add(new ApplianceModel("Air Conditioners", "5-Star Inverter AC 1.0T", 650, 8, "Compact Room"));
        acs.add(new ApplianceModel("Air Conditioners", "Commercial Heavy AC 2.0T", 1950, 8, "Heavy Duty"));
        catalog.add(new ApplianceCategory("Air Conditioners", acs));

        // 3. Refrigerators
        List<ApplianceModel> fridges = new ArrayList<>();
        fridges.add(new ApplianceModel("Refrigerators", "5-Star Inverter Double Door Refrigerator", 110, 24, "Smart Inverter"));
        fridges.add(new ApplianceModel("Refrigerators", "3-Star Frost Free Refrigerator", 180, 24, "Standard"));
        fridges.add(new ApplianceModel("Refrigerators", "Single Door Direct Cool Refrigerator 190L", 85, 24, "Low Draw"));
        fridges.add(new ApplianceModel("Refrigerators", "Side-by-Side Inverter Refrigerator", 240, 24, "Large Capacity"));
        fridges.add(new ApplianceModel("Refrigerators", "Commercial Display Cooler / Refrigerator", 550, 24, "Retail Chiller"));
        catalog.add(new ApplianceCategory("Refrigerators", fridges));

        // 4. Lighting
        List<ApplianceModel> lights = new ArrayList<>();
        lights.add(new ApplianceModel("Lighting", "LED Bulb 9W", 9, 6, "Ultra Efficient"));
        lights.add(new ApplianceModel("Lighting", "LED Batten / Tube 20W", 20, 6, "Bright White"));
        lights.add(new ApplianceModel("Lighting", "Fluorescent Tubelight 40W", 40, 6, "Legacy Tube"));
        lights.add(new ApplianceModel("Lighting", "CFL Spiral Bulb 18W", 18, 6, "Medium Draw"));
        lights.add(new ApplianceModel("Lighting", "Incandescent Filament Bulb 60W", 60, 4, "High Heat"));
        lights.add(new ApplianceModel("Lighting", "Outdoor LED Floodlight 50W", 50, 8, "High Lumen"));
        catalog.add(new ApplianceCategory("Lighting", lights));

        // 5. Television & Media
        List<ApplianceModel> tvs = new ArrayList<>();
        tvs.add(new ApplianceModel("Entertainment", "Smart LED TV 32\"–43\"", 55, 6, "Modern LED"));
        tvs.add(new ApplianceModel("Entertainment", "Large 4K QLED / OLED 55\"+", 115, 6, "Premium Display"));
        tvs.add(new ApplianceModel("Entertainment", "Old LCD / Plasma TV", 160, 6, "Legacy Screen"));
        tvs.add(new ApplianceModel("Entertainment", "Soundbar & Subwoofer", 45, 4, "Audio System"));
        tvs.add(new ApplianceModel("Entertainment", "Gaming Console (PS5/Xbox)", 160, 3, "High Compute"));
        catalog.add(new ApplianceCategory("Entertainment", tvs));

        // 6. Water Heating & Kitchen
        List<ApplianceModel> heating = new ArrayList<>();
        heating.add(new ApplianceModel("Water & Kitchen", "5-Star Storage Geyser 15L", 2000, 1, "Insulated Tank"));
        heating.add(new ApplianceModel("Water & Kitchen", "Instant Geyser 3L", 3000, 0.5, "Rapid Heat"));
        heating.add(new ApplianceModel("Water & Kitchen", "Immersion Heating Rod", 1500, 1, "Direct Element"));
        heating.add(new ApplianceModel("Water & Kitchen", "Microwave Oven", 1200, 0.5, "Reheating"));
        heating.add(new ApplianceModel("Water & Kitchen", "Induction Cooktop", 1800, 1.5, "Fast Cook"));
        heating.add(new ApplianceModel("Water & Kitchen", "Electric Kettle", 1500, 0.3, "Rapid Boil"));
        heating.add(new ApplianceModel("Water & Kitchen", "Mixer Grinder", 500, 0.5, "Kitchen Motor"));
        catalog.add(new ApplianceCategory("Water & Kitchen", heating));

        // 7. Laundry
        List<ApplianceModel> laundry = new ArrayList<>();
        laundry.add(new ApplianceModel("Laundry", "Inverter Front Load Washer", 500, 1, "Eco Inverter"));
        laundry.add(new ApplianceModel("Laundry", "Fully Automatic Top Load", 400, 1, "Standard Wash"));
        laundry.add(new ApplianceModel("Laundry", "Semi-Automatic Twin Tub", 320, 1, "Manual Drain"));
        catalog.add(new ApplianceCategory("Laundry", laundry));

        // 8. Computing & Work
        List<ApplianceModel> office = new ArrayList<>();
        office.add(new ApplianceModel("Computing & Work", "Laptop / Notebook", 65, 8, "Workstation"));
        office.add(new ApplianceModel("Computing & Work", "Desktop PC with Monitor", 200, 8, "Office Computer"));
        office.add(new ApplianceModel("Computing & Work", "Heavy Graphics Workstation", 500, 8, "High Compute"));
        office.add(new ApplianceModel("Computing & Work", "Laser Printer", 550, 0.5, "Office Print"));
        office.add(new ApplianceModel("Computing & Work", "Wi-Fi Router & Modem", 12, 24, "Continuous Draw"));
        office.add(new ApplianceModel("Computing & Work", "Server Rack Unit", 800, 24, "Data Server"));
        catalog.add(new ApplianceCategory("Computing & Work", office));

        return catalog;
    }

    public static List<ApplianceModel> getAllModels() {
        List<ApplianceModel> all = new ArrayList<>();
        for (ApplianceCategory cat : getApplianceCatalog()) {
            all.addAll(cat.models);
        }
        return all;
    }

    public static List<ApplianceModel> getAppliancesForEnvironment(String environment) {
        String env = (environment == null) ? "home" : environment.toLowerCase();
        List<ApplianceModel> list = new ArrayList<>();
        List<ApplianceModel> all = getAllModels();

        if (env.contains("office")) {
            for (ApplianceModel m : all) {
                if (m.category.equals("Computing & Work") || m.category.equals("Air Conditioners") || m.name.contains("Batten") || m.name.contains("BLDC")) {
                    list.add(m);
                }
            }
        } else if (env.contains("factory")) {
            for (ApplianceModel m : all) {
                if (m.name.contains("Industrial") || m.name.contains("Server") || m.name.contains("Floodlight") || m.name.contains("Commercial")) {
                    list.add(m);
                }
            }
        } else if (env.contains("shop")) {
            for (ApplianceModel m : all) {
                if (m.name.contains("Display") || m.name.contains("Air Conditioners") || m.name.contains("LED") || m.name.contains("Desktop") || m.name.contains("BLDC")) {
                    list.add(m);
                }
            }
        } else {
            // Home default
            for (ApplianceModel m : all) {
                if (!m.name.contains("Industrial") && !m.name.contains("Server") && !m.name.contains("Commercial Display")) {
                    list.add(m);
                }
            }
        }
        return list;
    }

    /**
     * Smart voice matcher that detects both the general appliance and specific subtype/technology.
     */
    public static ApplianceModel matchVoiceAppliance(String spokenPhrase) {
        String p = spokenPhrase.toLowerCase();

        // Specific high-efficiency & model matches first
        if (p.contains("bldc") || p.contains("brushless")) {
            return new ApplianceModel("Fans", "BLDC Smart Fan", 28, 12, "65% Power Saver");
        }
        if (p.contains("high speed fan")) {
            return new ApplianceModel("Fans", "High-Speed Induction Fan", 85, 12, "High Airflow");
        }
        if (p.contains("exhaust fan") || p.contains("exhaust")) {
            return new ApplianceModel("Fans", "Exhaust Fan", 40, 4, "Ventilation");
        }
        if (p.contains("table fan") || p.contains("pedestal")) {
            return new ApplianceModel("Fans", "Pedestal / Table Fan", 55, 8, "Portable");
        }
        if (p.contains("fan")) {
            return new ApplianceModel("Fans", "Standard Induction Fan", 75, 12, "Conventional");
        }

        // AC matches
        if (p.contains("5 star ac") || p.contains("five star ac") || p.contains("inverter ac")) {
            return new ApplianceModel("Air Conditioners", "5-Star Inverter AC 1.5T", 800, 8, "Super Efficient");
        }
        if (p.contains("window ac")) {
            return new ApplianceModel("Air Conditioners", "Window AC 1.5T", 1650, 8, "High Draw");
        }
        if (p.contains("3 star ac") || p.contains("three star ac")) {
            return new ApplianceModel("Air Conditioners", "3-Star Inverter AC 1.5T", 1150, 8, "Standard Inverter");
        }
        if (p.contains("ac") || p.contains("air conditioner") || p.contains("air condition")) {
            return new ApplianceModel("Air Conditioners", "3-Star Inverter AC 1.5T", 1150, 8, "Standard Inverter");
        }

        // Refrigerator matches
        if (p.contains("double door") || p.contains("inverter fridge")) {
            return new ApplianceModel("Refrigerators", "5-Star Inverter Double Door Refrigerator", 110, 24, "Smart Inverter");
        }
        if (p.contains("single door") || p.contains("small fridge")) {
            return new ApplianceModel("Refrigerators", "Single Door Direct Cool Refrigerator 190L", 85, 24, "Low Draw");
        }
        if (p.contains("fridge") || p.contains("refrigerator")) {
            return new ApplianceModel("Refrigerators", "5-Star Inverter Double Door Refrigerator", 110, 24, "Smart Inverter");
        }

        // Lighting matches
        if (p.contains("led bulb") || p.contains("bulb")) {
            return new ApplianceModel("Lighting", "LED Bulb 9W", 9, 6, "Ultra Efficient");
        }
        if (p.contains("batten") || p.contains("led tube") || p.contains("tubelight") || p.contains("tube light")) {
            return new ApplianceModel("Lighting", "LED Batten / Tube 20W", 20, 6, "Bright White");
        }
        if (p.contains("fluorescent") || p.contains("old tube")) {
            return new ApplianceModel("Lighting", "Fluorescent Tubelight 40W", 40, 6, "Legacy Tube");
        }
        if (p.contains("light") || p.contains("lamp")) {
            return new ApplianceModel("Lighting", "LED Bulb 9W", 9, 6, "Ultra Efficient");
        }

        // TV matches
        if (p.contains("qled") || p.contains("oled") || p.contains("4k tv") || p.contains("large tv")) {
            return new ApplianceModel("Entertainment", "Large 4K QLED / OLED 55\"+", 115, 6, "Premium Display");
        }
        if (p.contains("smart tv") || p.contains("tv") || p.contains("television") || p.contains("screen")) {
            return new ApplianceModel("Entertainment", "Smart LED TV 32\"–43\"", 55, 6, "Modern LED");
        }

        // Geyser / Water Heater
        if (p.contains("instant geyser")) {
            return new ApplianceModel("Water & Kitchen", "Instant Geyser 3L", 3000, 0.5, "Rapid Heat");
        }
        if (p.contains("geyser") || p.contains("water heater") || p.contains("boiler")) {
            return new ApplianceModel("Water & Kitchen", "5-Star Storage Geyser 15L", 2000, 1, "Insulated Tank");
        }

        // Washing Machine
        if (p.contains("front load")) {
            return new ApplianceModel("Laundry", "Inverter Front Load Washer", 500, 1, "Eco Inverter");
        }
        if (p.contains("top load")) {
            return new ApplianceModel("Laundry", "Fully Automatic Top Load", 400, 1, "Standard Wash");
        }
        if (p.contains("washing") || p.contains("washer")) {
            return new ApplianceModel("Laundry", "Inverter Front Load Washer", 500, 1, "Eco Inverter");
        }

        // Kitchen & Computing
        if (p.contains("microwave") || p.contains("oven")) {
            return new ApplianceModel("Water & Kitchen", "Microwave Oven", 1200, 0.5, "Reheating");
        }
        if (p.contains("induction stove") || p.contains("induction cooktop")) {
            return new ApplianceModel("Water & Kitchen", "Induction Cooktop", 1800, 1.5, "Fast Cook");
        }
        if (p.contains("kettle")) {
            return new ApplianceModel("Water & Kitchen", "Electric Kettle", 1500, 0.3, "Rapid Boil");
        }
        if (p.contains("laptop") || p.contains("notebook")) {
            return new ApplianceModel("Computing & Work", "Laptop / Notebook", 65, 8, "Workstation");
        }
        if (p.contains("computer") || p.contains("pc") || p.contains("desktop")) {
            return new ApplianceModel("Computing & Work", "Desktop PC with Monitor", 200, 8, "Office Computer");
        }
        if (p.contains("printer")) {
            return new ApplianceModel("Computing & Work", "Laser Printer", 550, 0.5, "Office Print");
        }
        if (p.contains("router") || p.contains("wifi")) {
            return new ApplianceModel("Computing & Work", "Wi-Fi Router & Modem", 12, 24, "Continuous Draw");
        }

        return null;
    }

    public static double applyStateMultiplier(double baseWattage, String state) {
        if (state == null) return baseWattage;
        Map<String, Double> multipliers = new HashMap<>();
        multipliers.put("Maharashtra", 1.1);
        multipliers.put("Gujarat", 1.05);
        multipliers.put("Tamil Nadu", 1.08);
        multipliers.put("Delhi", 1.15);
        multipliers.put("Kerala", 0.95);

        double multiplier = multipliers.containsKey(state) ? multipliers.get(state) : 1.0;
        return baseWattage * multiplier;
    }
}
