package com.andon.dashboard.config;

import com.andon.dashboard.model.Event;
import com.andon.dashboard.model.Severity;
import com.andon.dashboard.model.Status;
import com.andon.dashboard.repository.EventRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.andon.dashboard.model.Severity.*;
import static com.andon.dashboard.model.Status.*;

@Component
public class DataSeeder implements CommandLineRunner {

    private final EventRepository repo;

    public DataSeeder(EventRepository repo) {
        this.repo = repo;
    }

    private void add(String station, String issue, Severity sev, Status st, int minutesAgo) {
        repo.save(new Event(station, issue, sev, st, LocalDateTime.now().minusMinutes(minutesAgo)));
    }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) return;

        add("Line 1 - Press",     "Hydraulic Pressure Loss",   HIGH, OPEN, 8);
        add("Line 4 - Paint",     "Spray Robot Fault",         HIGH, OPEN, 22);
        add("Line 6 - Welding",   "Emergency Stop Triggered",  HIGH, OPEN, 41);
        add("Line 2 - Assembly",  "Conveyor Jam",              HIGH, OPEN, 55);

        add("Line 3 - CNC",       "Spindle Overheating",       HIGH, IN_PROGRESS, 90);
        add("Line 5 - Packing",   "Sealing Unit Failure",      HIGH, IN_PROGRESS, 130);
        add("Line 1 - Press",     "Safety Light Curtain Fault",HIGH, RESOLVED, 300);
        add("Line 7 - Casting",   "Furnace Temperature Spike", HIGH, RESOLVED, 480);

        add("Line 2 - Assembly",  "Torque Wrench Calibration", MEDIUM, OPEN, 15);
        add("Line 3 - CNC",       "Tool Wear Detected",        MEDIUM, OPEN, 60);
        add("Line 5 - Packing",   "Material Shortage",         MEDIUM, OPEN, 75);
        add("Line 6 - Welding",   "Wire Feed Irregular",       MEDIUM, IN_PROGRESS, 110);
        add("Line 4 - Paint",     "Colour Mismatch",           MEDIUM, IN_PROGRESS, 150);
        add("Line 8 - Inspection","Vision Camera Misalignment",MEDIUM, IN_PROGRESS, 200);
        add("Line 1 - Press",     "Die Change Delay",          MEDIUM, RESOLVED, 360);
        add("Line 7 - Casting",   "Mould Release Issue",       MEDIUM, RESOLVED, 420);
        add("Line 2 - Assembly",  "Fastener Supply Delay",     MEDIUM, RESOLVED, 600);

        add("Line 8 - Inspection","Label Printer Jam",         LOW, OPEN, 30);
        add("Line 5 - Packing",   "Carton Size Mismatch",      LOW, OPEN, 95);
        add("Line 3 - CNC",       "Coolant Level Low",         LOW, IN_PROGRESS, 170);
        add("Line 6 - Welding",   "Fume Extractor Noise",      LOW, IN_PROGRESS, 240);
        add("Line 4 - Paint",     "Booth Filter Replacement",  LOW, RESOLVED, 520);
        add("Line 1 - Press",     "Lubrication Reminder",      LOW, RESOLVED, 700);
        add("Line 7 - Casting",   "Sensor Cleaning Due",       LOW, RESOLVED, 800);
        add("Line 2 - Assembly",  "Barcode Scanner Slow",      LOW, RESOLVED, 900);
        add("Line 8 - Inspection","Gauge Zero Drift",          LOW, RESOLVED, 1100);
        add("Line 5 - Packing",   "Pallet Wrapper Tension",    LOW, RESOLVED, 1300);
        add("Line 3 - CNC",       "Chip Conveyor Blockage",    MEDIUM, RESOLVED, 1500);
        add("Line 6 - Welding",   "Gas Flow Low",              MEDIUM, RESOLVED, 1700);
    }
}