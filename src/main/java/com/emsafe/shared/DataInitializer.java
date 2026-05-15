package com.emsafe.shared;

import com.emsafe.dashboard.entity.Alert;
import com.emsafe.dashboard.entity.RadiationReading;
import com.emsafe.dashboard.repository.AlertRepository;
import com.emsafe.dashboard.repository.RadiationReadingRepository;
import com.emsafe.history.entity.History;
import com.emsafe.history.repository.HistoryRepository;
import com.emsafe.user.entity.AppUser;
import com.emsafe.user.entity.Role;
import com.emsafe.user.repository.UserRepository;
import com.emsafe.workorder.entity.*;
import com.emsafe.workorder.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WorkOrderRepository workOrderRepository;
    private final AlertRepository alertRepository;
    private final RadiationReadingRepository radiationReadingRepository;
    private final HistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded — skipping initialization.");
            return;
        }
        log.info("Seeding database with initial data...");
        seedUsers();
        seedWorkOrders();
        seedAlerts();
        seedRadiationReadings();
        seedHistory();
        log.info("Database seeding complete.");
    }

    // ─── Users ────────────────────────────────────────────────────────────────

    private void seedUsers() {
        List<AppUser> users = List.of(
                // Credentials (login users)
                AppUser.builder()
                        .email("admin@emsafe.com")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .name("Alex Thompson").initials("AT")
                        .role(Role.ADMIN).status("active")
                        .joinDate(LocalDate.of(2023, 9, 1)).build(),

                AppUser.builder()
                        .email("marcus@emsafe.com")
                        .passwordHash(passwordEncoder.encode("tech123"))
                        .name("Marcus Rivera").initials("MR")
                        .role(Role.TECHNICIAN).status("active")
                        .joinDate(LocalDate.of(2023, 11, 1)).build(),

                // Additional team members
                AppUser.builder()
                        .email("s.jenkins@field.emsafe.com")
                        .passwordHash(passwordEncoder.encode("tech123"))
                        .name("Sarah Jenkins").initials("SJ")
                        .role(Role.TECHNICIAN).status("active")
                        .joinDate(LocalDate.of(2023, 10, 15)).build(),

                AppUser.builder()
                        .email("e.rodriguez@field.emsafe.com")
                        .passwordHash(passwordEncoder.encode("tech123"))
                        .name("Elena Rodriguez").initials("ER")
                        .role(Role.TECHNICIAN).status("active")
                        .joinDate(LocalDate.of(2023, 12, 1)).build(),

                AppUser.builder()
                        .email("j.davis@emsafe-iot.com")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .name("Julianne Davis").initials("JD")
                        .role(Role.ADMIN).status("active")
                        .joinDate(LocalDate.of(2023, 9, 15)).build(),

                AppUser.builder()
                        .email("m.silva@field.emsafe.com")
                        .passwordHash(passwordEncoder.encode("tech123"))
                        .name("Maria Silva").initials("MS")
                        .role(Role.TECHNICIAN).status("active")
                        .joinDate(LocalDate.of(2024, 1, 10)).build(),

                AppUser.builder()
                        .email("r.king@field.emsafe.com")
                        .passwordHash(passwordEncoder.encode("tech123"))
                        .name("Robert King").initials("RK")
                        .role(Role.TECHNICIAN).status("inactive")
                        .joinDate(LocalDate.of(2024, 1, 20)).build(),

                // Clients
                AppUser.builder()
                        .email("ops@quantumdyn.com")
                        .passwordHash(passwordEncoder.encode("client123"))
                        .name("Quantum Dynamics Lab").initials("QD")
                        .role(Role.CLIENT).status("active")
                        .joinDate(LocalDate.of(2023, 10, 1)).build(),

                AppUser.builder()
                        .email("facilities@harbormed.com")
                        .passwordHash(passwordEncoder.encode("client123"))
                        .name("Harbor Medical Center").initials("HM")
                        .role(Role.CLIENT).status("active")
                        .joinDate(LocalDate.of(2023, 11, 5)).build(),

                AppUser.builder()
                        .email("safety@globalpharma.com")
                        .passwordHash(passwordEncoder.encode("client123"))
                        .name("Global Pharma Corp").initials("GP")
                        .role(Role.CLIENT).status("active")
                        .joinDate(LocalDate.of(2024, 2, 1)).build()
        );
        userRepository.saveAll(users);
        log.info("Seeded {} users", users.size());
    }

    // ─── Work Orders ──────────────────────────────────────────────────────────

    private void seedWorkOrders() {
        // Fetch technicians by email for FK assignment
        AppUser marcus = userRepository.findByEmail("marcus@emsafe.com").orElse(null);
        AppUser sarah = userRepository.findByEmail("s.jenkins@field.emsafe.com").orElse(null);
        AppUser elena = userRepository.findByEmail("e.rodriguez@field.emsafe.com").orElse(null);
        AppUser maria = userRepository.findByEmail("m.silva@field.emsafe.com").orElse(null);
        AppUser robert = userRepository.findByEmail("r.king@field.emsafe.com").orElse(null);

        // Admin work orders (12 entries from db.json)
        List<WorkOrder> adminOrders = List.of(
                buildWo("#WO-9284", WorkOrderType.INSTALLATION, WorkOrderStatus.IN_PROGRESS,
                        "Metro Logistics Corp.", "Downtown Plaza", "Austin, TX",
                        LocalDate.of(2023, 10, 24), null, sarah, "Sarah Jenkins", "SJ"),

                buildWo("#WO-9283", WorkOrderType.MAINTENANCE, WorkOrderStatus.IN_PROGRESS,
                        "Global Net Systems", "Tower 7 - Substation", "San Jose, CA",
                        LocalDate.of(2023, 10, 25), null, maria, "Maria Silva", "MS"),

                buildWo("#WO-9282", WorkOrderType.COLLECTION, WorkOrderStatus.PENDING,
                        "Urban Health Trust", "North Wing Lab", "Seattle, WA",
                        LocalDate.of(2023, 10, 26), null, robert, "Robert King", "RK"),

                buildWo("#WO-9281", WorkOrderType.INSTALLATION, WorkOrderStatus.COMPLETED,
                        "Coastal Antenna Inc.", "Coastal Antenna 4", "Miami, FL",
                        LocalDate.of(2023, 10, 26), null, sarah, "Sarah Jenkins", "SJ"),

                buildWo("#WO-9280", WorkOrderType.MAINTENANCE, WorkOrderStatus.COMPLETED,
                        "Northwind Energy", "Building A - Floor 3", "Denver, CO",
                        LocalDate.of(2023, 10, 27), null, marcus, "Marcus Rivera", "MR"),

                buildWo("#WO-9279", WorkOrderType.COLLECTION, WorkOrderStatus.PENDING,
                        "Greenfield Labs", "Sector 7", "Boston, MA",
                        LocalDate.of(2023, 10, 27), null, elena, "Elena Rodriguez", "ER"),

                buildWo("#WO-9278", WorkOrderType.MAINTENANCE, WorkOrderStatus.COMPLETED,
                        "Pacific Heights Clinic", "Wing B - MRI Suite", "San Francisco, CA",
                        LocalDate.of(2023, 10, 23), null, marcus, "Marcus Rivera", "MR"),

                buildWo("#WO-9277", WorkOrderType.INSTALLATION, WorkOrderStatus.COMPLETED,
                        "Harbor Medical Center", "ER Department - Ground Floor", "Portland, OR",
                        LocalDate.of(2023, 10, 22), null, elena, "Elena Rodriguez", "ER"),

                buildWo("#WO-9276", WorkOrderType.COLLECTION, WorkOrderStatus.PENDING,
                        "Atlas Semiconductor", "Clean Room B4", "Phoenix, AZ",
                        LocalDate.of(2023, 10, 28), null, sarah, "Sarah Jenkins", "SJ"),

                buildWo("#WO-9275", WorkOrderType.INSTALLATION, WorkOrderStatus.PENDING,
                        "Summit Data Center", "Server Hall 2", "Las Vegas, NV",
                        LocalDate.of(2023, 10, 28), null, marcus, "Marcus Rivera", "MR"),

                buildWo("#WO-9274", WorkOrderType.MAINTENANCE, WorkOrderStatus.COMPLETED,
                        "Nexus Research Lab", "Quantum Lab - Sub B", "Chicago, IL",
                        LocalDate.of(2023, 10, 21), null, maria, "Maria Silva", "MS"),

                buildWo("#WO-9273", WorkOrderType.COLLECTION, WorkOrderStatus.PENDING,
                        "Bay General Hospital", "Radiology - 2nd Floor", "Oakland, CA",
                        LocalDate.of(2023, 10, 29), null, robert, "Robert King", "RK")
        );
        workOrderRepository.saveAll(adminOrders);

        // Tech work orders (detailed, assigned to Marcus Rivera - technicianId=2)
        // WO-8492 — completed
        WorkOrder wo8492 = buildDetailedWo("WO-8492", WorkOrderType.MAINTENANCE, WorkOrderStatus.COMPLETED,
                "TechPark Plaza", "North Wing, L4, TechPark",
                LocalDate.of(2026, 5, 11), "09:00 AM", marcus,
                "Standard", "Sarah Jenkins", "Facility Manager",
                "+1-555-0123", "s.jenkins@techpark.com",
                "Report to the security desk. Badge required.",
                6, "TP-NW-L4",
                "All sensors calibrated and within normal range.",
                List.of("Standard Toolkit"));
        wo8492.getActivityLog().add(logEntry("Work Order Completed", "May 11, 09:45", wo8492));
        wo8492.getActivityLog().add(logEntry("Arrival at Site", "May 11, 08:55", wo8492));
        workOrderRepository.save(wo8492);

        // WO-9102 — in-progress with sensors
        WorkOrder wo9102 = buildDetailedWo("WO-9102", WorkOrderType.INSTALLATION, WorkOrderStatus.IN_PROGRESS,
                "Greenview Residences", "Block B, Lobby, Sector 7",
                LocalDate.of(2026, 5, 12), "01:30 PM", marcus,
                "High Priority", "Linda Torres", "Property Manager",
                "+1-555-0188", "l.torres@greenview.com",
                "Ask for the building manager at the front desk.",
                6, "GV-BL-S7", "",
                List.of("Standard Toolkit", "Calibration Rig B"));
        wo9102.getSensors().add(sensor("#S-101", "Main Hallway - North Wing", "ok", wo9102));
        wo9102.getSensors().add(sensor("#S-102", "MRI Suite Entrance", "maintenance", wo9102));
        wo9102.getSensors().add(sensor("#S-103", "Emergency Exit B", "maintenance", wo9102));
        workOrderRepository.save(wo9102);

        // WO-7734 — in-progress
        buildAndSaveWo("WO-7734", WorkOrderType.COLLECTION, WorkOrderStatus.IN_PROGRESS,
                "St. Jude Medical", "MRI Suite A, Floor 2",
                LocalDate.of(2026, 5, 13), "10:00 AM", marcus,
                "High Priority", "Dr. Sarah Jenkins", "Department Head",
                "+1-555-0143", "s.jenkins@stjude.med",
                "Report to the security desk. Requires Level 2 RFID clearance badge.",
                8, "SJ-MRI-F2", "",
                List.of("Standard Toolkit", "Calibration Rig B"));

        // WO-4401 — pending
        buildAndSaveWo("WO-4401", WorkOrderType.MAINTENANCE, WorkOrderStatus.PENDING,
                "Skyline Towers", "Rooftop Antennas, Main St.",
                LocalDate.of(2026, 5, 14), "09:00 AM", marcus,
                "Standard", "Mike Anderson", "Building Manager",
                "+1-555-0156", "m.anderson@skyline.com",
                "Rooftop access key at security desk.",
                4, "ST-RF-001", "",
                List.of("Standard Toolkit"));

        // WO-5521 — pending
        buildAndSaveWo("WO-5521", WorkOrderType.INSTALLATION, WorkOrderStatus.PENDING,
                "Metropolis Data Center", "Server Room 4, Basement",
                LocalDate.of(2026, 5, 15), "11:00 AM", marcus,
                "High Priority", "James Reilly", "IT Operations Manager",
                "+1-555-0199", "j.reilly@metropolis.com",
                "Report to the security desk. Requires Level 2 RFID clearance badge.",
                8, "MDC-SR4-B", "",
                List.of("Standard Toolkit", "Calibration Rig B"));

        // WO-3310 — pending
        buildAndSaveWo("WO-3310", WorkOrderType.INSTALLATION, WorkOrderStatus.PENDING,
                "Harbor Medical Center", "ER Department, Ground Floor",
                LocalDate.of(2026, 5, 18), "08:00 AM", marcus,
                "High Priority", "Dr. Patel", "Chief Medical Officer",
                "+1-555-0201", "r.patel@harbormed.com",
                "Check in at reception, escort required to ER.",
                10, "HMC-ER-GF", "",
                List.of("Standard Toolkit", "Calibration Rig B", "Safety Meter"));

        // WO-3311 — in-progress with sensors
        WorkOrder wo3311 = buildDetailedWo("WO-3311", WorkOrderType.MAINTENANCE, WorkOrderStatus.IN_PROGRESS,
                "Riverside Tech Hub", "Server Corridor, Level 2",
                LocalDate.of(2026, 5, 19), "10:30 AM", marcus,
                "Standard", "Karen Voss", "Facilities Coordinator",
                "+1-555-0214", "k.voss@riverside.tech",
                "Badge access via main entrance, Level 2 key card needed.",
                5, "RTH-SC-L2",
                "Sensor 203 shows intermittent signal. Investigating.",
                List.of("Standard Toolkit"));
        wo3311.getSensors().add(sensor("#S-201", "Corridor A - Entry", "ok", wo3311));
        wo3311.getSensors().add(sensor("#S-202", "Corridor B - Middle", "ok", wo3311));
        wo3311.getSensors().add(sensor("#S-203", "Server Room Door", "maintenance", wo3311));
        workOrderRepository.save(wo3311);

        // WO-3312 — completed
        WorkOrder wo3312 = buildDetailedWo("WO-3312", WorkOrderType.COLLECTION, WorkOrderStatus.COMPLETED,
                "Downtown Financial Tower", "Executive Floor 28, Zone C",
                LocalDate.of(2026, 5, 20), "02:00 PM", marcus,
                "Standard", "Olivia Grant", "Office Manager",
                "+1-555-0222", "o.grant@dft.com",
                "Security check at lobby, appointment registered.",
                3, "DFT-EF-28C",
                "All 3 sensors collected and sealed for transport.",
                List.of("Collection Kit"));
        wo3312.getActivityLog().add(logEntry("Work Order Completed", "May 20, 14:52", wo3312));
        wo3312.getActivityLog().add(logEntry("Sensors Collected", "May 20, 14:35", wo3312));
        wo3312.getActivityLog().add(logEntry("Arrival at Site", "May 20, 14:05", wo3312));
        workOrderRepository.save(wo3312);

        // WO-3313 — pending
        buildAndSaveWo("WO-3313", WorkOrderType.INSTALLATION, WorkOrderStatus.PENDING,
                "Nexus Research Lab", "Quantum Lab - Sub B",
                LocalDate.of(2026, 5, 21), "09:00 AM", marcus,
                "High Priority", "Dr. Nguyen", "Lab Director",
                "+1-555-0233", "d.nguyen@nexuslab.com",
                "Clearance required. Contact security 30 min prior.",
                12, "NRL-QL-SB", "",
                List.of("Standard Toolkit", "Calibration Rig B"));

        // WO-3314 — pending
        buildAndSaveWo("WO-3314", WorkOrderType.MAINTENANCE, WorkOrderStatus.PENDING,
                "Pacific Heights Clinic", "Wing B - MRI Suite",
                LocalDate.of(2026, 5, 22), "11:00 AM", marcus,
                "Standard", "Nurse Martinez", "Department Supervisor",
                "+1-555-0241", "c.martinez@phclinic.com",
                "Sign in at nursing station. Do not enter when MRI in use.",
                4, "PHC-WB-MRI", "",
                List.of("Standard Toolkit"));

        // WO-4420 — completed
        WorkOrder wo4420 = buildDetailedWo("WO-4420", WorkOrderType.COLLECTION, WorkOrderStatus.COMPLETED,
                "Atlas Semiconductor Plant", "Clean Room B4",
                LocalDate.of(2026, 5, 25), "08:30 AM", marcus,
                "Standard", "Thomas Holt", "Plant Manager",
                "+1-555-0255", "t.holt@atlassemi.com",
                "Full clean room protocol. Suit-up at entry.",
                6, "ASP-CR-B4",
                "Clean room protocol followed. 6 sensors collected.",
                List.of("Collection Kit", "ESD Protection"));
        wo4420.getActivityLog().add(logEntry("Work Order Completed", "May 25, 09:30", wo4420));
        wo4420.getActivityLog().add(logEntry("Arrival at Site", "May 25, 08:25", wo4420));
        workOrderRepository.save(wo4420);

        // WO-4421 — pending
        buildAndSaveWo("WO-4421", WorkOrderType.INSTALLATION, WorkOrderStatus.PENDING,
                "Summit Data Center", "Server Hall 2, Rack Row D",
                LocalDate.of(2026, 5, 26), "09:00 AM", marcus,
                "High Priority", "Bryan Lee", "Data Center Manager",
                "+1-555-0266", "b.lee@summitdc.com",
                "Biometric check at entry. No phones in server hall.",
                8, "SDC-SH2-D", "",
                List.of("Standard Toolkit", "Calibration Rig B"));

        // WO-4422 — in-progress with sensors
        WorkOrder wo4422 = buildDetailedWo("WO-4422", WorkOrderType.MAINTENANCE, WorkOrderStatus.IN_PROGRESS,
                "Bay Area General Hospital", "Radiology - 2nd Floor",
                LocalDate.of(2026, 5, 27), "01:00 PM", marcus,
                "Standard", "Jessica Chan", "Radiology Supervisor",
                "+1-555-0277", "j.chan@bagh.com",
                "Report to radiology reception. Wear hospital ID badge.",
                5, "BAGH-RAD-2F",
                "S-303 needs recalibration. Parts ordered.",
                List.of("Standard Toolkit"));
        wo4422.getSensors().add(sensor("#S-301", "X-Ray Room 1", "ok", wo4422));
        wo4422.getSensors().add(sensor("#S-302", "CT Scan Room", "ok", wo4422));
        wo4422.getSensors().add(sensor("#S-303", "MRI Anteroom", "maintenance", wo4422));
        wo4422.getSensors().add(sensor("#S-304", "Hallway B", "ok", wo4422));
        workOrderRepository.save(wo4422);

        // WO-4423 — pending
        buildAndSaveWo("WO-4423", WorkOrderType.COLLECTION, WorkOrderStatus.PENDING,
                "Urban Tech Campus", "Innovation Hub - 5th Floor",
                LocalDate.of(2026, 5, 28), "03:00 PM", marcus,
                "Standard", "Sophia Reyes", "Campus Operations",
                "+1-555-0288", "s.reyes@urbantechcampus.com",
                "Check in at the main desk. Elevator B to 5th floor.",
                4, "UTC-IH-5F", "",
                List.of("Collection Kit"));

        // WO-5530 — completed
        WorkOrder wo5530 = buildDetailedWo("WO-5530", WorkOrderType.MAINTENANCE, WorkOrderStatus.COMPLETED,
                "Vertex Telecom Hub", "Antenna Array - Roof Level",
                LocalDate.of(2026, 5, 5), "07:00 AM", marcus,
                "Standard", "Paul Simmons", "Telecom Engineer",
                "+1-555-0299", "p.simmons@vertextelecom.com",
                "Roof key at security. Hard hat required.",
                6, "VTH-AR-RF",
                "All antennas inspected. Minor alignment done on sensor 4.",
                List.of("Standard Toolkit", "Safety Meter"));
        wo5530.getActivityLog().add(logEntry("Work Order Completed", "May 05, 09:15", wo5530));
        wo5530.getActivityLog().add(logEntry("Maintenance Performed", "May 05, 08:00", wo5530));
        wo5530.getActivityLog().add(logEntry("Arrival at Site", "May 05, 07:10", wo5530));
        workOrderRepository.save(wo5530);

        // WO-5531 — pending
        buildAndSaveWo("WO-5531", WorkOrderType.INSTALLATION, WorkOrderStatus.PENDING,
                "Global Pharma HQ", "Lab Wing C - Floor 3",
                LocalDate.of(2026, 5, 8), "10:00 AM", marcus,
                "High Priority", "Dr. Amelia Stone", "Lab Safety Officer",
                "+1-555-0311", "a.stone@globalpharma.com",
                "Lab coat and goggles required. Escort from security.",
                10, "GPH-LWC-F3", "",
                List.of("Standard Toolkit", "Calibration Rig B"));

        log.info("Seeded work orders");
    }

    // ─── Alerts ───────────────────────────────────────────────────────────────

    private void seedAlerts() {
        List<Alert> alerts = List.of(
                Alert.builder().type("danger").icon("ph-warning-circle")
                        .title("Abnormal Radiation Spike")
                        .description("Sector 7-B detected levels above 0.42 μSv/h.")
                        .relativeTime("12 minutes ago")
                        .createdAt(LocalDateTime.now().minusMinutes(12)).build(),

                Alert.builder().type("info").icon("ph-arrows-clockwise")
                        .title("Auto-Calibration Success")
                        .description("Network node #401 recalibrated successfully.")
                        .relativeTime("2 hours ago")
                        .createdAt(LocalDateTime.now().minusHours(2)).build(),

                Alert.builder().type("success").icon("ph-user-plus")
                        .title("New Technician Onboarded")
                        .description("Elena Rodriguez added to field service team.")
                        .relativeTime("5 hours ago")
                        .createdAt(LocalDateTime.now().minusHours(5)).build(),

                Alert.builder().type("danger").icon("ph-warning-circle")
                        .title("Sensor Offline — WO-9275")
                        .description("Sensor #S-204 at Summit Data Center lost connection.")
                        .relativeTime("1 hour ago")
                        .createdAt(LocalDateTime.now().minusHours(1)).build(),

                Alert.builder().type("info").icon("ph-clipboard-text")
                        .title("New Work Order Created")
                        .description("WO-9276 assigned to Sarah Jenkins — Atlas Semiconductor.")
                        .relativeTime("3 hours ago")
                        .createdAt(LocalDateTime.now().minusHours(3)).build()
        );
        alertRepository.saveAll(alerts);
        log.info("Seeded {} alerts", alerts.size());
    }

    // ─── Radiation Readings ───────────────────────────────────────────────────

    private void seedRadiationReadings() {
        List<RadiationReading> readings = List.of(
                new RadiationReading(null, LocalDate.of(2026, 5, 1), 0.05),
                new RadiationReading(null, LocalDate.of(2026, 5, 5), 0.07),
                new RadiationReading(null, LocalDate.of(2026, 5, 8), 0.06),
                new RadiationReading(null, LocalDate.of(2026, 5, 10), 0.09),
                new RadiationReading(null, LocalDate.of(2026, 5, 12), 0.08),
                new RadiationReading(null, LocalDate.of(2026, 5, 13), 0.11),
                new RadiationReading(null, LocalDate.of(2026, 5, 14), 0.13),
                new RadiationReading(null, LocalDate.of(2026, 5, 15), 0.12)
        );
        radiationReadingRepository.saveAll(readings);
        log.info("Seeded {} radiation readings", readings.size());
    }

    // ─── History ──────────────────────────────────────────────────────────────

    private void seedHistory() {
        AppUser marcus = userRepository.findByEmail("marcus@emsafe.com").orElse(null);
        AppUser sarah = userRepository.findByEmail("s.jenkins@field.emsafe.com").orElse(null);
        AppUser elena = userRepository.findByEmail("e.rodriguez@field.emsafe.com").orElse(null);

        Long marcusId = marcus != null ? marcus.getId() : null;
        Long sarahId = sarah != null ? sarah.getId() : null;
        Long elenaId = elena != null ? elena.getId() : null;

        List<History> records = List.of(
                hist("#WO-92841", LocalDate.of(2023, 10, 28), "14:45 PM",
                        "Quantum Dynamics Lab", "Sector 7-B, North Wing",
                        "Installation", "Sarah Jenkins", "SJ", "completed", sarahId),

                hist("#WO-92838", LocalDate.of(2023, 10, 27), "11:15 AM",
                        "Metro Data Center", "UPS Battery Room 2",
                        "Maintenance", "Marcus Rivera", "MR", "completed", marcusId),

                hist("#WO-92832", LocalDate.of(2023, 10, 25), "09:30 AM",
                        "Skyline Telecom Hub", "Roof Terrace - Tower 4",
                        "Collection", "Elena Rodriguez", "ER", "completed", elenaId),

                hist("#WO-92829", LocalDate.of(2023, 10, 22), "16:10 PM",
                        "Global Pharma Corp", "MRI Suite 1 - Basement",
                        "Installation", "Sarah Jenkins", "SJ", "completed", sarahId),

                hist("#WO-92821", LocalDate.of(2023, 10, 19), "10:00 AM",
                        "Vertex Office Plaza", "Lobby & Public Zones",
                        "Maintenance", "Marcus Rivera", "MR", "cancelled", marcusId),

                hist("#WO-92815", LocalDate.of(2023, 10, 17), "13:20 PM",
                        "Harbor Medical Center", "ER Department - Ground Floor",
                        "Installation", "Marcus Rivera", "MR", "completed", marcusId),

                hist("#WO-92807", LocalDate.of(2023, 10, 15), "11:45 AM",
                        "Pacific Heights Clinic", "Wing B - MRI Suite",
                        "Maintenance", "Sarah Jenkins", "SJ", "completed", sarahId),

                hist("#WO-92800", LocalDate.of(2023, 10, 14), "15:00 PM",
                        "Nexus Research Lab", "Quantum Lab - Sub B",
                        "Collection", "Elena Rodriguez", "ER", "completed", elenaId),

                hist("#WO-92793", LocalDate.of(2023, 10, 12), "09:00 AM",
                        "Riverside Tech Hub", "Server Corridor - Level 2",
                        "Maintenance", "Marcus Rivera", "MR", "cancelled", marcusId),

                hist("#WO-92785", LocalDate.of(2023, 10, 10), "17:30 PM",
                        "Downtown Financial Tower", "Executive Floor 28, Zone C",
                        "Collection", "Marcus Rivera", "MR", "completed", marcusId),

                hist("#WO-92778", LocalDate.of(2023, 10, 9), "12:00 PM",
                        "Bay Area General Hospital", "Radiology - 2nd Floor",
                        "Installation", "Sarah Jenkins", "SJ", "completed", sarahId),

                hist("#WO-92770", LocalDate.of(2023, 10, 7), "08:45 AM",
                        "Atlas Semiconductor Plant", "Clean Room B4",
                        "Maintenance", "Elena Rodriguez", "ER", "completed", elenaId)
        );
        historyRepository.saveAll(records);
        log.info("Seeded {} history records", records.size());
    }

    // ─── Builder helpers ──────────────────────────────────────────────────────

    private WorkOrder buildWo(String orderId, WorkOrderType type, WorkOrderStatus status,
                              String client, String location, String city, LocalDate date,
                              String time, AppUser technician, String techName, String techInitials) {
        return WorkOrder.builder()
                .orderId(orderId).type(type).status(status)
                .client(client).location(location).city(city)
                .scheduledDate(date).scheduledTime(time)
                .technician(technician)
                .technicianName(techName).technicianInitials(techInitials)
                .build();
    }

    private WorkOrder buildDetailedWo(String orderId, WorkOrderType type, WorkOrderStatus status,
                                       String client, String location, LocalDate date, String time,
                                       AppUser technician, String priority,
                                       String contactName, String contactRole,
                                       String contactPhone, String contactEmail,
                                       String accessInstructions, int expectedSensors,
                                       String assetId, String notes, List<String> tools) {
        WorkOrder wo = WorkOrder.builder()
                .orderId(orderId).type(type).status(status)
                .client(client).location(location)
                .scheduledDate(date).scheduledTime(time)
                .technician(technician)
                .technicianName(technician != null ? technician.getName() : null)
                .technicianInitials(technician != null ? technician.getInitials() : null)
                .priority(priority)
                .contactName(contactName).contactRole(contactRole)
                .contactPhone(contactPhone).contactEmail(contactEmail)
                .accessInstructions(accessInstructions)
                .expectedSensors(expectedSensors).assetId(assetId)
                .technicianNotes(notes)
                .build();
        wo.getRequiredTools().addAll(tools);
        return wo;
    }

    private void buildAndSaveWo(String orderId, WorkOrderType type, WorkOrderStatus status,
                                 String client, String location, LocalDate date, String time,
                                 AppUser technician, String priority,
                                 String contactName, String contactRole,
                                 String contactPhone, String contactEmail,
                                 String accessInstructions, int expectedSensors,
                                 String assetId, String notes, List<String> tools) {
        workOrderRepository.save(buildDetailedWo(orderId, type, status, client, location,
                date, time, technician, priority, contactName, contactRole,
                contactPhone, contactEmail, accessInstructions, expectedSensors,
                assetId, notes, tools));
    }

    private Sensor sensor(String sensorId, String location, String status, WorkOrder wo) {
        return Sensor.builder()
                .sensorId(sensorId).location(location).status(status).workOrder(wo).build();
    }

    private ActivityLogEntry logEntry(String event, String time, WorkOrder wo) {
        return ActivityLogEntry.builder()
                .event(event).logTime(time).workOrder(wo).build();
    }

    private History hist(String orderId, LocalDate date, String time,
                          String client, String site, String serviceType,
                          String technician, String initials, String status, Long technicianId) {
        return History.builder()
                .orderId(orderId).completionDate(date).completionTime(time)
                .client(client).site(site).serviceType(serviceType)
                .technician(technician).technicianInitials(initials)
                .status(status).technicianId(technicianId)
                .build();
    }
}
