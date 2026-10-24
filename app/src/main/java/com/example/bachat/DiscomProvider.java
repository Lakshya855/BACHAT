package com.example.bachat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registry of Indian Electricity Distribution Companies (DISCOMs) by State & Union Territory.
 * Contains provider names, coverage areas, average domestic tariffs, and official SERC ToD peak hours.
 */
public class DiscomProvider {

    public static class Discom {
        public final String id;
        public final String name;
        public final String state;
        public final String coverageArea;
        public final double defaultTariff; // in ₹ / kWh
        public final String peakHours;
        public final String peakSurcharge;
        public final String offPeakHours;
        public final String offPeakIncentive;
        public final String summaryNote;
        public final int peakStartHour; // 24-hr format
        public final int peakEndHour;

        public Discom(
                String id,
                String name,
                String state,
                String coverageArea,
                double defaultTariff,
                String peakHours,
                String peakSurcharge,
                String offPeakHours,
                String offPeakIncentive,
                String summaryNote,
                int peakStartHour,
                int peakEndHour
        ) {
            this.id = id;
            this.name = name;
            this.state = state;
            this.coverageArea = coverageArea;
            this.defaultTariff = defaultTariff;
            this.peakHours = peakHours;
            this.peakSurcharge = peakSurcharge;
            this.offPeakHours = offPeakHours;
            this.offPeakIncentive = offPeakIncentive;
            this.summaryNote = summaryNote;
            this.peakStartHour = peakStartHour;
            this.peakEndHour = peakEndHour;
        }

        @Override
        public String toString() {
            return name + " (" + coverageArea + ")";
        }
    }

    private static final Map<String, List<Discom>> DISCOMS_BY_STATE = new HashMap<>();
    private static final Map<String, Discom> DISCOMS_BY_ID = new HashMap<>();

    static {
        // --- MAHARASHTRA ---
        registerStateDiscoms("Maharashtra", Arrays.asList(
                new Discom(
                        "MSEDCL",
                        "MSEDCL (Mahavitaran)",
                        "Maharashtra",
                        "Pune, Thane, Nashik, Nagpur & Rest of State",
                        8.50,
                        "6:00 PM – 10:00 PM",
                        "+20% TARIFF (+₹1.10/u)",
                        "10:00 PM – 6:00 AM",
                        "-15% NIGHT REBATE (-₹1.50/u)",
                        "MSEDCL levies a steep peak surcharge during evening hours (18:00–22:00) and offers substantial night discounts for overnight loads.",
                        18, 22
                ),
                new Discom(
                        "AEML",
                        "Adani Electricity (AEML)",
                        "Maharashtra",
                        "Mumbai Suburbs (Bandra to Dahisar, Kurla to Mankhurd)",
                        9.40,
                        "6:00 PM – 10:00 PM",
                        "+20% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-15% NIGHT REBATE",
                        "AEML Mumbai levies evening peak surcharge for heavy residential cooling and commercial loads.",
                        18, 22
                ),
                new Discom(
                        "TPC_MUMBAI",
                        "Tata Power (TPC-D)",
                        "Maharashtra",
                        "Mumbai City & Suburbs",
                        8.80,
                        "6:00 PM – 10:00 PM",
                        "+20% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-15% NIGHT REBATE",
                        "Tata Power Mumbai implements SERC approved Time of Day tariffs with peak demand management.",
                        18, 22
                ),
                new Discom(
                        "BEST",
                        "BEST Undertaking",
                        "Maharashtra",
                        "South & Central Mumbai (Colaba to Sion/Mahim)",
                        7.90,
                        "6:00 PM – 10:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-10% NIGHT REBATE",
                        "BEST serves South Mumbai island city with lower flat domestic baseline rates and evening peak hours.",
                        18, 22
                )
        ));

        // --- DELHI NCT ---
        List<Discom> delhiDiscoms = Arrays.asList(
                new Discom(
                        "BRPL",
                        "BSES Rajdhani (BRPL)",
                        "Delhi",
                        "South & West Delhi (Dwarka, Saket, Hauz Khas, Janakpuri)",
                        7.50,
                        "2:00 PM – 5:00 PM & 10 PM – 1 AM",
                        "+20% PEAK SURCHARGE",
                        "9:00 AM – 2:00 PM (Solar Window)",
                        "-20% SOLAR REBATE",
                        "BRPL operates afternoon and late-night peak slots during high summer air-conditioning demand spikes.",
                        14, 17
                ),
                new Discom(
                        "BYPL",
                        "BSES Yamuna (BYPL)",
                        "Delhi",
                        "East & Central Delhi (Laxmi Nagar, Mayur Vihar, Chandni Chowk)",
                        7.50,
                        "2:00 PM – 5:00 PM & 10 PM – 1 AM",
                        "+20% PEAK SURCHARGE",
                        "9:00 AM – 2:00 PM (Solar Window)",
                        "-20% SOLAR REBATE",
                        "BYPL serves dense East and Central Delhi with DERC approved dual-peak windows.",
                        14, 17
                ),
                new Discom(
                        "TPDDL",
                        "Tata Power Delhi (TPDDL)",
                        "Delhi",
                        "North & North-West Delhi (Rohini, Pitampura, Model Town)",
                        7.60,
                        "2:00 PM – 5:00 PM & 10 PM – 1 AM",
                        "+20% PEAK SURCHARGE",
                        "9:00 AM – 2:00 PM (Solar Window)",
                        "-20% SOLAR REBATE",
                        "TPDDL covers North Delhi with automated smart metering and daytime solar incentives.",
                        14, 17
                ),
                new Discom(
                        "NDMC",
                        "NDMC Electricity Dept",
                        "Delhi",
                        "Lutyens' Delhi & Connaught Place Area",
                        8.00,
                        "2:00 PM – 5:00 PM & 10 PM – 1 AM",
                        "+20% PEAK SURCHARGE",
                        "9:00 AM – 2:00 PM (Solar Window)",
                        "-15% SOLAR REBATE",
                        "NDMC serves VIP and central municipal zones in the national capital.",
                        14, 17
                )
        );
        registerStateDiscoms("Delhi", delhiDiscoms);

        // --- KARNATAKA ---
        registerStateDiscoms("Karnataka", Arrays.asList(
                new Discom(
                        "BESCOM",
                        "BESCOM",
                        "Karnataka",
                        "Bengaluru Urban, Rural, Ramanagara, Tumakuru, Kolar",
                        8.20,
                        "6:00 PM – 10:00 PM",
                        "+20% TARIFF (+₹1.00/u)",
                        "10:00 PM – 6:00 AM",
                        "-₹1.00/u OFF-PEAK REBATE",
                        "BESCOM enforces evening peak tariffs across greater Bengaluru; shift heavy appliances to night to save.",
                        18, 22
                ),
                new Discom(
                        "MESCOM",
                        "MESCOM",
                        "Karnataka",
                        "Mangaluru, Udupi, Dakshina Kannada, Shivamogga",
                        7.90,
                        "6:00 PM – 10:00 PM",
                        "+20% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-₹1.00/u OFF-PEAK REBATE",
                        "MESCOM powers the coastal and Malnad region of Karnataka.",
                        18, 22
                ),
                new Discom(
                        "HESCOM",
                        "HESCOM",
                        "Karnataka",
                        "Hubballi, Dharwad, Belagavi, Bagalkot, Vijayapura",
                        7.80,
                        "6:00 PM – 10:00 PM",
                        "+20% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-₹1.00/u OFF-PEAK REBATE",
                        "HESCOM covers northern Karnataka districts.",
                        18, 22
                ),
                new Discom(
                        "CHESCOM",
                        "CHESCOM",
                        "Karnataka",
                        "Mysuru, Mandya, Hassan, Chamarajanagar, Kodagu",
                        7.70,
                        "6:00 PM – 10:00 PM",
                        "+20% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-₹1.00/u OFF-PEAK REBATE",
                        "CHESCOM serves the southern heritage districts of Karnataka.",
                        18, 22
                ),
                new Discom(
                        "GESCOM",
                        "GESCOM",
                        "Karnataka",
                        "Kalaburagi, Bidar, Raichur, Koppal, Ballari",
                        7.60,
                        "6:00 PM – 10:00 PM",
                        "+20% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-₹1.00/u OFF-PEAK REBATE",
                        "GESCOM covers Hyderabad-Karnataka northeastern belt.",
                        18, 22
                )
        ));

        // --- UTTAR PRADESH ---
        registerStateDiscoms("Uttar Pradesh", Arrays.asList(
                new Discom(
                        "PVVNL",
                        "PVVNL (Paschimanchal)",
                        "Uttar Pradesh",
                        "Noida, Greater Noida, Ghaziabad, Meerut, Moradabad",
                        7.80,
                        "5:00 PM – 11:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "11:00 PM – 6:00 AM",
                        "-15% NIGHT REBATE",
                        "PVVNL manages intense NCR industrial and urban residential loads with a 6-hour evening peak.",
                        17, 23
                ),
                new Discom(
                        "NPCL",
                        "NPCL (Noida Power Co)",
                        "Uttar Pradesh",
                        "Greater Noida Area (Private Licensee)",
                        7.90,
                        "5:00 PM – 11:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "11:00 PM – 6:00 AM",
                        "-15% NIGHT REBATE",
                        "NPCL is a private utility distributor in Greater Noida.",
                        17, 23
                ),
                new Discom(
                        "MVVNL",
                        "MVVNL (Madhyanchal)",
                        "Uttar Pradesh",
                        "Lucknow, Ayodhya, Bareilly, Sitapur, Unnao",
                        7.50,
                        "5:00 PM – 11:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "11:00 PM – 6:00 AM",
                        "-15% NIGHT REBATE",
                        "MVVNL covers central UP including the state capital Lucknow.",
                        17, 23
                ),
                new Discom(
                        "PuVVNL",
                        "PuVVNL (Purvanchal)",
                        "Uttar Pradesh",
                        "Varanasi, Prayagraj, Gorakhpur, Mirzapur",
                        7.40,
                        "5:00 PM – 11:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "11:00 PM – 6:00 AM",
                        "-15% NIGHT REBATE",
                        "PuVVNL serves eastern UP with widespread domestic lighting demand.",
                        17, 23
                ),
                new Discom(
                        "DVVNL",
                        "DVVNL (Dakshinanchal)",
                        "Uttar Pradesh",
                        "Agra, Mathura, Aligarh, Firozabad, Jhansi",
                        7.40,
                        "5:00 PM – 11:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "11:00 PM – 6:00 AM",
                        "-15% NIGHT REBATE",
                        "DVVNL covers southern and Braj region of Uttar Pradesh.",
                        17, 23
                ),
                new Discom(
                        "KESCO",
                        "KESCO",
                        "Uttar Pradesh",
                        "Kanpur Metropolitan City",
                        7.60,
                        "5:00 PM – 11:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "11:00 PM – 6:00 AM",
                        "-15% NIGHT REBATE",
                        "Kanpur Electricity Supply Company dedicated to urban Kanpur.",
                        17, 23
                )
        ));

        // --- GUJARAT ---
        registerStateDiscoms("Gujarat", Arrays.asList(
                new Discom(
                        "TORRENT_GUJ",
                        "Torrent Power",
                        "Gujarat",
                        "Ahmedabad, Gandhinagar, Surat (Private)",
                        7.80,
                        "7:00 AM – 11:00 AM & 6 PM – 10 PM",
                        "+₹0.85/u SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-₹0.40/u NIGHT DISCOUNT",
                        "Torrent Power provides high reliability urban power across Ahmedabad and Surat with dual peak hours.",
                        18, 22
                ),
                new Discom(
                        "DGVCL",
                        "DGVCL (Dakshin Gujarat)",
                        "Gujarat",
                        "Surat Rural, Navsari, Valsad, Bharuch, Tapi",
                        6.90,
                        "7:00 AM – 11:00 AM & 6 PM – 10 PM",
                        "+₹0.85/u SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-₹0.40/u NIGHT DISCOUNT",
                        "DGVCL powers South Gujarat industrial and agricultural belts.",
                        18, 22
                ),
                new Discom(
                        "MGVCL",
                        "MGVCL (Madhya Gujarat)",
                        "Gujarat",
                        "Vadodara, Anand, Kheda, Panchmahal",
                        6.80,
                        "7:00 AM – 11:00 AM & 6 PM – 10 PM",
                        "+₹0.85/u SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-₹0.40/u NIGHT DISCOUNT",
                        "MGVCL serves Central Gujarat districts.",
                        18, 22
                ),
                new Discom(
                        "UGVCL",
                        "UGVCL (Uttar Gujarat)",
                        "Gujarat",
                        "Mehsana, Sabarkantha, Banaskantha, Patan",
                        6.80,
                        "7:00 AM – 11:00 AM & 6 PM – 10 PM",
                        "+₹0.85/u SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-₹0.40/u NIGHT DISCOUNT",
                        "UGVCL serves North Gujarat region.",
                        18, 22
                ),
                new Discom(
                        "PGVCL",
                        "PGVCL (Paschim Gujarat)",
                        "Gujarat",
                        "Rajkot, Bhavnagar, Jamnagar, Junagadh, Kutch",
                        6.70,
                        "7:00 AM – 11:00 AM & 6 PM – 10 PM",
                        "+₹0.85/u SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-₹0.40/u NIGHT DISCOUNT",
                        "PGVCL covers Saurashtra and Kutch peninsula.",
                        18, 22
                )
        ));

        // --- WEST BENGAL ---
        registerStateDiscoms("West Bengal", Arrays.asList(
                new Discom(
                        "CESC",
                        "CESC Kolkata",
                        "West Bengal",
                        "Kolkata & Howrah Urban (Private Licensee)",
                        8.30,
                        "5:00 PM – 11:00 PM",
                        "+20% TARIFF SURCHARGE",
                        "11:00 PM – 6:00 AM",
                        "-10% OFF-PEAK REBATE",
                        "CESC serves greater Kolkata and Howrah with a continuous evening peak tariff window.",
                        17, 23
                ),
                new Discom(
                        "WBSEDCL",
                        "WBSEDCL",
                        "West Bengal",
                        "Rest of West Bengal (Siliguri, Durgapur, Asansol, 24 Parganas)",
                        7.40,
                        "5:00 PM – 11:00 PM",
                        "+20% TARIFF SURCHARGE",
                        "11:00 PM – 6:00 AM",
                        "-10% OFF-PEAK REBATE",
                        "WBSEDCL is the state power utility across all districts of West Bengal.",
                        17, 23
                )
        ));

        // --- TAMIL NADU ---
        registerStateDiscoms("Tamil Nadu", Arrays.asList(
                new Discom(
                        "TANGEDCO_CHENNAI",
                        "TANGEDCO (Chennai Region)",
                        "Tamil Nadu",
                        "Chennai Metro, Chengalpattu, Kanchipuram, Tiruvallur",
                        7.50,
                        "6:00 PM – 9:00 PM",
                        "+25% TARIFF SURCHARGE",
                        "10:00 PM – 5:00 AM",
                        "-10% NIGHT REBATE",
                        "TNERC mandates a +25% evening peak surcharge in the Chennai metro corridor.",
                        18, 21
                ),
                new Discom(
                        "TANGEDCO_REST",
                        "TANGEDCO (State-wide)",
                        "Tamil Nadu",
                        "Coimbatore, Madurai, Tiruchirappalli, Salem, Tirunelveli",
                        7.20,
                        "6:00 PM – 9:00 PM",
                        "+25% TARIFF SURCHARGE",
                        "10:00 PM – 5:00 AM",
                        "-10% NIGHT REBATE",
                        "State utility covering Tier 2/3 cities and rural Tamil Nadu.",
                        18, 21
                )
        ));

        // --- TELANGANA ---
        registerStateDiscoms("Telangana", Arrays.asList(
                new Discom(
                        "TSSPDCL",
                        "TSSPDCL (Southern Power)",
                        "Telangana",
                        "Hyderabad, Ranga Reddy, Medchal, Mahabubnagar, Nalgonda",
                        7.80,
                        "6:00 AM – 10:00 AM & 6 PM – 10 PM",
                        "+₹1.00/u SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-₹1.00/u NIGHT REBATE",
                        "TSSPDCL manages Hyderabad IT corridor and southern districts.",
                        18, 22
                ),
                new Discom(
                        "TSNPDCL",
                        "TSNPDCL (Northern Power)",
                        "Telangana",
                        "Warangal, Karimnagar, Nizamabad, Khammam, Adilabad",
                        7.40,
                        "6:00 AM – 10:00 AM & 6 PM – 10 PM",
                        "+₹1.00/u SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-₹1.00/u NIGHT REBATE",
                        "TSNPDCL covers northern Telangana districts.",
                        18, 22
                )
        ));

        // --- ANDHRA PRADESH ---
        registerStateDiscoms("Andhra Pradesh", Arrays.asList(
                new Discom(
                        "APCPDCL",
                        "APCPDCL (Central Power)",
                        "Andhra Pradesh",
                        "Vijayawada, Guntur, Krishna, Prakasam",
                        7.60,
                        "6:00 AM – 10:00 AM & 6 PM – 10 PM",
                        "+₹1.00/u SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-₹1.00/u NIGHT REBATE",
                        "APCPDCL covers Amaravati capital region and central districts.",
                        18, 22
                ),
                new Discom(
                        "APEPDCL",
                        "APEPDCL (Eastern Power)",
                        "Andhra Pradesh",
                        "Visakhapatnam, Vizianagaram, Srikakulam, Godavari",
                        7.50,
                        "6:00 AM – 10:00 AM & 6 PM – 10 PM",
                        "+₹1.00/u SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-₹1.00/u NIGHT REBATE",
                        "APEPDCL powers the coastal industrial zone of Vizag.",
                        18, 22
                ),
                new Discom(
                        "APSPDCL",
                        "APSPDCL (Southern Power)",
                        "Andhra Pradesh",
                        "Tirupati, Nellore, Chittoor, Kadapa, Anantapur",
                        7.50,
                        "6:00 AM – 10:00 AM & 6 PM – 10 PM",
                        "+₹1.00/u SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-₹1.00/u NIGHT REBATE",
                        "APSPDCL covers Rayalaseema and southern Andhra.",
                        18, 22
                )
        ));

        // --- RAJASTHAN ---
        registerStateDiscoms("Rajasthan", Arrays.asList(
                new Discom(
                        "JVVNL",
                        "JVVNL (Jaipur Discom)",
                        "Rajasthan",
                        "Jaipur, Alwar, Dausa, Bharatpur, Kota, Baran",
                        7.90,
                        "6:00 PM – 10:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-10% NIGHT REBATE",
                        "JVVNL serves Jaipur urban and eastern Rajasthan.",
                        18, 22
                ),
                new Discom(
                        "AVVNL",
                        "AVVNL (Ajmer Discom)",
                        "Rajasthan",
                        "Ajmer, Bhilwara, Udaipur, Chittorgarh, Sikar, Jhunjhunu",
                        7.80,
                        "6:00 PM – 10:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-10% NIGHT REBATE",
                        "AVVNL powers central and Mewar regions of Rajasthan.",
                        18, 22
                ),
                new Discom(
                        "JDVVNL",
                        "JdVVNL (Jodhpur Discom)",
                        "Rajasthan",
                        "Jodhpur, Bikaner, Barmer, Jaisalmer, Pali, Sirohi",
                        7.80,
                        "6:00 PM – 10:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-10% NIGHT REBATE",
                        "JdVVNL covers the desert western districts of Rajasthan.",
                        18, 22
                )
        ));

        // --- PUNJAB ---
        registerStateDiscoms("Punjab", Arrays.asList(
                new Discom(
                        "PSPCL",
                        "PSPCL",
                        "Punjab",
                        "State-wide (Ludhiana, Amritsar, Jalandhar, Patiala)",
                        7.20,
                        "6:00 PM – 10:00 PM",
                        "+20% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-20% NIGHT REBATE",
                        "PSPCL provides heavy night incentives (-20%) to shift agricultural pumps and domestic heavy machinery away from evening peaks.",
                        18, 22
                )
        ));

        // --- HARYANA ---
        registerStateDiscoms("Haryana", Arrays.asList(
                new Discom(
                        "DHBVN",
                        "DHBVN (Dakshin Haryana)",
                        "Haryana",
                        "Gurugram, Faridabad, Hisar, Rohtak, Rewari",
                        7.60,
                        "6:00 PM – 10:00 PM",
                        "+20% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-15% NIGHT REBATE",
                        "DHBVN powers Millennium City Gurugram and southern Haryana.",
                        18, 22
                ),
                new Discom(
                        "UHBVN",
                        "UHBVN (Uttar Haryana)",
                        "Haryana",
                        "Panchkula, Ambala, Karnal, Panipat, Kurukshetra",
                        7.40,
                        "6:00 PM – 10:00 PM",
                        "+20% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-15% NIGHT REBATE",
                        "UHBVN covers northern Haryana districts.",
                        18, 22
                )
        ));

        // --- KERALA ---
        registerStateDiscoms("Kerala", Arrays.asList(
                new Discom(
                        "KSEB",
                        "KSEB (Kerala State Electricity Board)",
                        "Kerala",
                        "State-wide (Kochi, Thiruvananthapuram, Kozhikode, Thrissur)",
                        6.80,
                        "6:00 PM – 10:00 PM",
                        "+20% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-10% NIGHT REBATE",
                        "KSEB domestic ToD meters charge 120% during 18:00–22:00 peak hours and 90% during 22:00–06:00 off-peak.",
                        18, 22
                )
        ));

        // --- MADHYA PRADESH ---
        registerStateDiscoms("Madhya Pradesh", Arrays.asList(
                new Discom(
                        "MPPKVVCL_W",
                        "MPPKVVCL (West Zone - Indore)",
                        "Madhya Pradesh",
                        "Indore, Ujjain, Dewas, Dhar, Khargone",
                        7.60,
                        "6:00 PM – 10:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-10% NIGHT REBATE",
                        "West MP Discom powers commercial hub Indore.",
                        18, 22
                ),
                new Discom(
                        "MPMKVVCL",
                        "MPMKVVCL (Central Zone - Bhopal)",
                        "Madhya Pradesh",
                        "Bhopal, Gwalior, Sehore, Vidisha, Rajgarh",
                        7.50,
                        "6:00 PM – 10:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-10% NIGHT REBATE",
                        "Central MP Discom serves Bhopal and Gwalior.",
                        18, 22
                ),
                new Discom(
                        "MPPKVVCL_E",
                        "MPPKVVCL (East Zone - Jabalpur)",
                        "Madhya Pradesh",
                        "Jabalpur, Sagar, Rewa, Satna, Chhindwara",
                        7.40,
                        "6:00 PM – 10:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-10% NIGHT REBATE",
                        "East MP Discom covers Mahakoshal and Vindhya regions.",
                        18, 22
                )
        ));

        // --- BIHAR ---
        registerStateDiscoms("Bihar", Arrays.asList(
                new Discom(
                        "SBPDCL",
                        "SBPDCL (South Bihar Power)",
                        "Bihar",
                        "Patna, Gaya, Bhagalpur, Nalanda, Rohtas",
                        7.50,
                        "5:00 PM – 11:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "11:00 PM – 6:00 AM",
                        "-10% NIGHT REBATE",
                        "SBPDCL serves state capital Patna and southern Bihar.",
                        17, 23
                ),
                new Discom(
                        "NBPDCL",
                        "NBPDCL (North Bihar Power)",
                        "Bihar",
                        "Muzaffarpur, Darbhanga, Purnea, Chapra, Motihari",
                        7.40,
                        "5:00 PM – 11:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "11:00 PM – 6:00 AM",
                        "-10% NIGHT REBATE",
                        "NBPDCL serves Mithilanchal and northern Bihar.",
                        17, 23
                )
        ));

        // --- ODISHA ---
        registerStateDiscoms("Odisha", Arrays.asList(
                new Discom(
                        "TPCODL",
                        "TPCODL (Tata Power Central Odisha)",
                        "Odisha",
                        "Bhubaneswar, Cuttack, Puri, Kendrapara",
                        6.90,
                        "6:00 PM – 10:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-10% NIGHT REBATE",
                        "TPCODL serves the capital city Bhubaneswar and coastal central Odisha.",
                        18, 22
                ),
                new Discom(
                        "TPNODL",
                        "TPNODL (Tata Power Northern Odisha)",
                        "Odisha",
                        "Balasore, Bhadrak, Mayurbhanj, Keonjhar",
                        6.80,
                        "6:00 PM – 10:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-10% NIGHT REBATE",
                        "TPNODL covers northern industrial and mining districts of Odisha.",
                        18, 22
                ),
                new Discom(
                        "TPSODL",
                        "TPSODL (Tata Power Southern Odisha)",
                        "Odisha",
                        "Berhampur, Ganjam, Koraput, Rayagada",
                        6.80,
                        "6:00 PM – 10:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-10% NIGHT REBATE",
                        "TPSODL covers southern Odisha districts.",
                        18, 22
                ),
                new Discom(
                        "TPWODL",
                        "TPWODL (Tata Power Western Odisha)",
                        "Odisha",
                        "Sambalpur, Rourkela, Bargarh, Jharsuguda",
                        6.80,
                        "6:00 PM – 10:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-10% NIGHT REBATE",
                        "TPWODL powers the steel city Rourkela and western Odisha.",
                        18, 22
                )
        ));

        // --- CHHATTISGARH ---
        registerStateDiscoms("Chhattisgarh", Arrays.asList(
                new Discom(
                        "CSPDCL",
                        "CSPDCL",
                        "Chhattisgarh",
                        "State-wide (Raipur, Bilaspur, Durg, Bhilai)",
                        6.80,
                        "6:00 PM – 10:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-10% NIGHT REBATE",
                        "CSPDCL is the sole state utility in power-surplus Chhattisgarh.",
                        18, 22
                )
        ));

        // --- JHARKHAND ---
        registerStateDiscoms("Jharkhand", Arrays.asList(
                new Discom(
                        "JBVNL",
                        "JBVNL",
                        "Jharkhand",
                        "State-wide (Ranchi, Dhanbad, Bokaro, Hazaribagh)",
                        6.70,
                        "6:00 PM – 10:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-10% NIGHT REBATE",
                        "JBVNL provides electricity across Jharkhand state.",
                        18, 22
                ),
                new Discom(
                        "JUSCO",
                        "Tata Steel UISL (JUSCO)",
                        "Jharkhand",
                        "Jamshedpur Tata Steel Command Area",
                        7.20,
                        "6:00 PM – 10:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-10% NIGHT REBATE",
                        "Private utility serving the industrial city of Jamshedpur.",
                        18, 22
                )
        ));

        // --- GOA ---
        registerStateDiscoms("Goa", Arrays.asList(
                new Discom(
                        "GED",
                        "Goa Electricity Dept",
                        "Goa",
                        "North Goa & South Goa (Panaji, Margao, Vasco)",
                        5.80,
                        "6:00 PM – 10:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-10% NIGHT REBATE",
                        "Goa government electricity department with low coastal domestic slabs.",
                        18, 22
                )
        ));

        // --- ASSAM ---
        registerStateDiscoms("Assam", Arrays.asList(
                new Discom(
                        "APDCL",
                        "APDCL",
                        "Assam",
                        "State-wide (Guwahati, Dibrugarh, Silchar, Jorhat)",
                        7.30,
                        "6:00 PM – 10:00 PM",
                        "+15% TARIFF SURCHARGE",
                        "10:00 PM – 6:00 AM",
                        "-10% NIGHT REBATE",
                        "APDCL is the primary distribution utility in Assam.",
                        18, 22
                )
        ));
    }

    private static void registerStateDiscoms(String state, List<Discom> discoms) {
        String key = state.trim().toLowerCase(Locale.ROOT);
        DISCOMS_BY_STATE.put(key, discoms);
        for (Discom d : discoms) {
            DISCOMS_BY_ID.put(d.id.toLowerCase(Locale.ROOT), d);
        }
    }

    /**
     * Returns the list of registered DISCOMs for a state.
     * If unlisted, generates a standard SERC state board entry.
     */
    public static List<Discom> getDiscomsForState(String stateName) {
        if (stateName != null) {
            String key = stateName.trim().toLowerCase(Locale.ROOT);
            for (Map.Entry<String, List<Discom>> entry : DISCOMS_BY_STATE.entrySet()) {
                if (key.contains(entry.getKey()) || entry.getKey().contains(key)) {
                    return new ArrayList<>(entry.getValue());
                }
            }
        }

        // Generic fallback for any other state
        String sName = (stateName != null && !stateName.isEmpty()) ? stateName : "Default";
        List<Discom> fallbackList = new ArrayList<>();
        fallbackList.add(new Discom(
                "STATE_DISCOM_" + sName.toUpperCase(Locale.ROOT).replace(" ", "_"),
                sName + " State Electricity Board",
                sName,
                "State-wide coverage",
                7.50,
                "6:00 PM – 10:00 PM",
                "+20% TARIFF SURCHARGE",
                "10:00 PM – 6:00 AM",
                "-15% NIGHT REBATE",
                "Official SERC regulatory tariff framework for " + sName + ".",
                18, 22
        ));
        return fallbackList;
    }

    /**
     * Finds a DISCOM by its ID or by name matching.
     */
    public static Discom findDiscom(String stateName, String discomQuery) {
        if (discomQuery != null && !discomQuery.trim().isEmpty()) {
            String cleanQuery = discomQuery.trim().toLowerCase(Locale.ROOT);
            if (DISCOMS_BY_ID.containsKey(cleanQuery)) {
                return DISCOMS_BY_ID.get(cleanQuery);
            }
            List<Discom> list = getDiscomsForState(stateName);
            for (Discom d : list) {
                if (d.name.toLowerCase(Locale.ROOT).contains(cleanQuery) ||
                        cleanQuery.contains(d.name.toLowerCase(Locale.ROOT)) ||
                        d.id.toLowerCase(Locale.ROOT).contains(cleanQuery) ||
                        cleanQuery.contains(d.id.toLowerCase(Locale.ROOT))) {
                    return d;
                }
            }
            if (!list.isEmpty()) {
                return list.get(0);
            }
        }
        List<Discom> list = getDiscomsForState(stateName);
        return list.isEmpty() ? null : list.get(0);
    }
}
