package edu.hofstra.csc17.proj.soclog.ingest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.hofstra.csc17.proj.soclog.ingest.parser.EventParser;
import edu.hofstra.csc17.proj.soclog.model.event.Event;

public class LogIngestor {
    private final EventParser parser;

    public LogIngestor(EventParser parser) {
        this.parser = parser;
    }

    public IngestionResult ingest(List<Path> logFiles) throws IOException {
        long startTime = System.currentTimeMillis();
        
        List<Event> events = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Path file : logFiles) {
            if (!Files.exists(file)) {
                errors.add("Missing file: " + file);
                continue;
            }
            if (Files.size(file) == 0) {
                continue;
            }
            EventParser.ParseResult parsed = parser.parse(file);
            events.addAll(parsed.getEvents());
            errors.addAll(parsed.getErrors());
        }
        
        long endTime = System.currentTimeMillis();
        long processingTimeMs = endTime - startTime;
        
        return new IngestionResult(events, errors, processingTimeMs);
    }

    public static final class IngestionResult {
        private final List<Event> events;
        private final List<String> errors;
        private final long processingTimeMs;

        public IngestionResult(List<Event> events, List<String> errors) {
            this(events, errors, 0);
        }
        
        public IngestionResult(List<Event> events, List<String> errors, long processingTimeMs) {
            this.events = Collections.unmodifiableList(new ArrayList<>(events));
            this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
            this.processingTimeMs = processingTimeMs;
        }

        public List<Event> getEvents() {
            return events;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void printIngestionSummary() {
            System.out.println("Ingestion Summary:");
            System.out.println("  Valid events: " + events.size());
            System.out.println("  Rejections: " + errors.size());
            System.out.println("  Processing time: " + processingTimeMs + " ms");
            
            // Add rejection rate
            int totalRecords = events.size() + errors.size();
            if (totalRecords > 0) {
                double rejectionRate = (errors.size() * 100.0 / totalRecords);
                System.out.printf("  Rejection rate: %.2f%%\n", rejectionRate);
            }
            
            // Add time range if events exist
            if (!events.isEmpty()) {
                System.out.println("  Time range: " + startTime() + " to " + endTime());
            }
            
            // Add top rejection reasons if there are errors
            if (!errors.isEmpty()) {
                Map<String, Long> errorCounts = errors.stream()
                    .map(this::extractErrorReason)
                    .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
                
                System.out.println("  Top rejection reasons:");
                errorCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(3)
                    .forEach(e -> System.out.println("    - " + e.getKey() + ": " + e.getValue()));
            }
        }
        
        private String extractErrorReason(String error) {
            // Extract the type of error from the error message
            if (error.contains("Invalid event type")) {
                return "Invalid event type";
            } else if (error.contains("Malformed timestamp")) {
                return "Malformed timestamp";
            } else if (error.contains("is before previous timestamp")) {
                return "Out-of-order timestamp";
            } else if (error.contains("Missing or empty")) {
                return "Missing required field";
            } else if (error.contains("Invalid subject") || error.contains("Invalid object")) {
                return "Invalid field format";
            } else if (error.contains("Privilege must be")) {
                return "Invalid privilege value";
            } else if (error.contains("Invalid PID")) {
                return "Invalid PID";
            } else if (error.contains("Permissions must be")) {
                return "Invalid file permissions";
            } else if (error.contains("Port must be")) {
                return "Invalid network port";
            } else if (error.contains("Invalid IPv4")) {
                return "Invalid IP address";
            } else if (error.contains("Malformed key=value")) {
                return "Malformed key=value pairs";
            } else if (error.contains("Expected 5 fields")) {
                return "Invalid record structure";
            } else {
                return "Other error";
            }
        }

        public void datasetSummary() {
            System.out.println("Dataset Summary:");
            System.out.println("Total valid events: " + events.size());
            
            if (events.isEmpty()) {
                System.out.println("No valid events in dataset");
                System.out.println("\n");
                return;
            }
            
            // Event type distribution
            Map<String, Long> eventTypeCounts = events.stream()
                .collect(Collectors.groupingBy(e -> e.getType().toString(), Collectors.counting()));
            
            System.out.println("\nEvent type distribution:");
            eventTypeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue()));
            
            // Time range
            System.out.println("\nTime range:");
            System.out.println("  Start: " + startTime());
            System.out.println("  End: " + endTime());
            
            // Unique processes
            long uniqueProcesses = events.stream()
                .map(e -> e.getSubject().getName())
                .distinct()
                .count();
            System.out.println("\nUnique processes: " + uniqueProcesses);
            
            // Privilege distribution
            long rootEvents = events.stream()
                .filter(e -> e.getSubject().isRoot())
                .count();
            long userEvents = events.size() - rootEvents;
            System.out.println("\nPrivilege distribution:");
            System.out.println("  Root: " + rootEvents);
            System.out.println("  User: " + userEvents);
            
            System.out.println("\n");
        }

        public Instant startTime() {
            if (events.isEmpty()) {
                return null;
            }
            return events.stream()
                .map(Event::getTimestamp)
                .min(Instant::compareTo)
                .orElse(null);
        }

        public Instant endTime() {
            if (events.isEmpty()) {
                return null;
            }
            return events.stream()
                .map(Event::getTimestamp)
                .max(Instant::compareTo)
                .orElse(null);
        }
    }
}
