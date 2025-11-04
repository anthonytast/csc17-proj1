package edu.hofstra.csc17.proj.soclog;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import edu.hofstra.csc17.proj.soclog.analysis.AnalyticsEngine;
import edu.hofstra.csc17.proj.soclog.ingest.LogIngestor;
import edu.hofstra.csc17.proj.soclog.ingest.parser.EventParser;

public final class Main {

    public static void main(String[] args) throws Exception {
        List<Path> inputs;

        if (args.length == 0) {
            // Default to data directory if no arguments provided
            System.out.println("No arguments provided. Using default 'data' directory...");
            inputs = getCsvFilesFromDirectory(Paths.get("data"));
        } else if (args.length == 1) {
            Path arg = Paths.get(args[0]);
            if (Files.isDirectory(arg)) {
                // Single argument is a directory - process all CSV files in it
                System.out.println("Processing all CSV files in directory: " + arg);
                inputs = getCsvFilesFromDirectory(arg);
            } else {
                // Single argument is a file
                inputs = Arrays.asList(arg);
            }
        } else {
            // Multiple arguments - treat as individual files
            inputs = Arrays.stream(args)
                    .map(Paths::get)
                    .collect(Collectors.toList());
        }

        if (inputs.isEmpty()) {
            System.err.println("No CSV files found to process.");
            printUsage();
            System.exit(1);
        }

        System.out.println("Processing " + inputs.size() + " CSV file(s):");
        for (Path input : inputs) {
            System.out.println("  - " + input);
        }

        LogIngestor ingestor = new LogIngestor(new EventParser());
        LogIngestor.IngestionResult summary = ingestor.ingest(inputs);

        System.out.println("Valid events: " + summary.getEvents().size());
        System.out.println("Rejections: " + summary.getErrors().size());
        
        // Print all rejection errors
        if (!summary.getErrors().isEmpty()) {
            System.out.println("\nAll rejections:");
            for (int i = 0; i < summary.getErrors().size(); i++) {
                System.out.println("  " + (i + 1) + ". " + summary.getErrors().get(i));
            }
        }

        // Demonstrate analytics functionality
        AnalyticsEngine engine = new AnalyticsEngine(summary.getEvents());
        System.out.println();
        System.out.println("Analytics Engine Demo");

        // Basic analytics
        demonstrateBasicAnalytics(engine);

        // Advanced SOC analytics
        demonstrateSOCAnalytics(engine);
    }

    private static void demonstrateBasicAnalytics(AnalyticsEngine engine) {
        System.out.println("Basic Analytics:");
        try {
            engine.topKFrequentEvents(3);
            System.out.println("  ✓ Frequency analysis available");
        } catch (UnsupportedOperationException ex) {
            System.out.println("  • Frequency analysis requires implementation");
        }

        try {
            engine.countByEventType();
            System.out.println("  ✓ Event type counting available");
        } catch (UnsupportedOperationException ex) {
            System.out.println("  • Event type counting requires implementation");
        }
    }

    private static void demonstrateSOCAnalytics(AnalyticsEngine engine) {
        System.out.println("\nAdvanced SOC Analytics:");


        try {
            engine.detectPrivilegeEscalation(null, null);
            System.out.println("  ✓ Privilege escalation detection available");
        } catch (UnsupportedOperationException ex) {
            System.out.println("  • Privilege escalation detection requires implementation");
        }


    }

    /**
     * Get all CSV files from the given directory.
     */
    private static List<Path> getCsvFilesFromDirectory(Path directory) throws IOException {
        List<Path> csvFiles = new ArrayList<>();

        if (!Files.exists(directory)) {
            System.err.println("Directory does not exist: " + directory);
            return csvFiles;
        }

        if (!Files.isDirectory(directory)) {
            System.err.println("Path is not a directory: " + directory);
            return csvFiles;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.csv")) {
            for (Path file : stream) {
                csvFiles.add(file);
            }
        }

        if (csvFiles.isEmpty()) {
            System.out.println("No CSV files found in directory: " + directory);
        } else {
            System.out.println("Found " + csvFiles.size() + " CSV file(s) in " + directory);
        }

        return csvFiles;
    }

    /**
     * Print usage information.
     */
    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("  java edu.hofstra.csc17.proj.soclog.Main");
        System.err.println("    (processes all CSV files in 'data' directory)");
        System.err.println("  java edu.hofstra.csc17.proj.soclog.Main <directory>");
        System.err.println("    (processes all CSV files in specified directory)");
        System.err.println("  java edu.hofstra.csc17.proj.soclog.Main <csv-file> [<csv-file>...]");
        System.err.println("    (processes specified CSV files)");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  java edu.hofstra.csc17.proj.soclog.Main");
        System.err.println("  java edu.hofstra.csc17.proj.soclog.Main data");
        System.err.println("  java edu.hofstra.csc17.proj.soclog.Main /path/to/logs");
        System.err.println("  java edu.hofstra.csc17.proj.soclog.Main file1.csv file2.csv");
    }

    private Main() {
    }
}
