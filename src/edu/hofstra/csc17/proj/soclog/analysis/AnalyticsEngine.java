package edu.hofstra.csc17.proj.soclog.analysis;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import edu.hofstra.csc17.proj.soclog.model.event.Event;
import edu.hofstra.csc17.proj.soclog.model.event.EventType;
import edu.hofstra.csc17.proj.soclog.model.event.ForkEvent;
import edu.hofstra.csc17.proj.soclog.model.event.ReadEvent;
import edu.hofstra.csc17.proj.soclog.model.event.WriteEvent;
import edu.hofstra.csc17.proj.soclog.model.event.ExecuteEvent;
import edu.hofstra.csc17.proj.soclog.model.event.OpenEvent;
import edu.hofstra.csc17.proj.soclog.model.event.CloseEvent;
import edu.hofstra.csc17.proj.soclog.model.entity.FileInfo;
import edu.hofstra.csc17.proj.soclog.model.entity.ProcessInfo;

public class AnalyticsEngine {
    private final List<Event> events;

    /**
     * Construct the engine with an initial collection of validated and deduplicated events.
     */
    public AnalyticsEngine(List<Event> events) {
        this.events = new ArrayList<>(Objects.requireNonNull(events));
        // Events are stored as-is; deduplication happens in query methods
    }

    public List<Event> uniqueEvents() {
        // Deduplicate based on all fields using Event's equals() method
        return events.stream()
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Return (deduplicated) events whose timestamps fall in the inclusive range.
     * @throws IllegalArgumentException if start > end
     */
    public List<Event> uniqueEvents(Instant startInclusive, Instant endInclusive) {
        if (startInclusive == null || endInclusive == null) {
            throw new IllegalArgumentException("Start and end timestamps cannot be null");
        }
        if (startInclusive.isAfter(endInclusive)) {
            throw new IllegalArgumentException("Start time must not be after end time");
        }
        
        return events.stream()
            .filter(e -> !e.getTimestamp().isBefore(startInclusive) && 
                        !e.getTimestamp().isAfter(endInclusive))
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Return top-k frequent events across the entire dataset ranked by canonical key.
     * @param k number of top events to return
     * @throws IllegalArgumentException if k <= 0 or k larger than population
     */
    public List<Event> topKFrequentEvents(int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("k must be positive, got: " + k);
        }
        
        List<Event> uniqueEvents = uniqueEvents();
        if (k > uniqueEvents.size()) {
            throw new IllegalArgumentException("k (" + k + ") cannot be larger than unique event count (" + uniqueEvents.size() + ")");
        }
        
        // Count frequencies using Event's equals() for grouping
        Map<Event, Long> frequencyMap = events.stream()
            .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        
        // Sort by frequency (descending), then by canonical representation for deterministic tie-breaking
        return frequencyMap.entrySet().stream()
            .sorted(Comparator
                .<Map.Entry<Event, Long>>comparingLong(Map.Entry::getValue).reversed()
                .thenComparing(e -> getCanonicalEventKey(e.getKey())))
            .limit(k)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Return top-k frequent events within the specified time window ranked by canonical key.
     */
    public List<Event> topKFrequentEvents(Instant startInclusive, Instant endInclusive, int k) {
        if (startInclusive == null || endInclusive == null) {
            throw new IllegalArgumentException("Start and end timestamps cannot be null");
        }
        if (startInclusive.isAfter(endInclusive)) {
            throw new IllegalArgumentException("Start time must not be after end time");
        }
        if (k <= 0) {
            throw new IllegalArgumentException("k must be positive, got: " + k);
        }
        
        // Filter events in time window
        List<Event> windowEvents = events.stream()
            .filter(e -> !e.getTimestamp().isBefore(startInclusive) && 
                        !e.getTimestamp().isAfter(endInclusive))
            .collect(Collectors.toList());
        
        List<Event> uniqueWindowEvents = windowEvents.stream().distinct().collect(Collectors.toList());
        if (k > uniqueWindowEvents.size()) {
            throw new IllegalArgumentException("k (" + k + ") cannot be larger than unique event count in window (" + uniqueWindowEvents.size() + ")");
        }
        
        // Count frequencies
        Map<Event, Long> frequencyMap = windowEvents.stream()
            .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        
        // Sort by frequency (descending), then by canonical representation
        return frequencyMap.entrySet().stream()
            .sorted(Comparator
                .<Map.Entry<Event, Long>>comparingLong(Map.Entry::getValue).reversed()
                .thenComparing(e -> getCanonicalEventKey(e.getKey())))
            .limit(k)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Generate a canonical string representation for deterministic sorting.
     */
    private String getCanonicalEventKey(Event event) {
        return event.getType() + "|" + 
               event.getSubject().getCanonicalId() + "|" + 
               event.getObject().getCanonicalId() + "|" +
               event.getTimestamp();
    }

    /**
     * Count events grouped by concrete subclass name.
     */
    public Map<EventType, Long> countByEventType() {
        return events.stream()
            .collect(Collectors.groupingBy(Event::getType, Collectors.counting()));
    }

    public Long countByEventType(EventType type) {
        if (type == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
        return events.stream()
            .filter(e -> e.getType() == type)
            .count();
    }


    /**
     * Return top processes within window ranked by event count.
     */
    public Map<String, Long> topProcessesByWindow(Instant startInclusive, Instant endInclusive, int limit) {
        if (startInclusive == null || endInclusive == null) {
            throw new IllegalArgumentException("Start and end timestamps cannot be null");
        }
        if (startInclusive.isAfter(endInclusive)) {
            throw new IllegalArgumentException("Start time must not be after end time");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive, got: " + limit);
        }
        
        // Filter events in time window and count by process name
        Map<String, Long> processCounts = events.stream()
            .filter(e -> !e.getTimestamp().isBefore(startInclusive) && 
                        !e.getTimestamp().isAfter(endInclusive))
            .collect(Collectors.groupingBy(e -> e.getSubject().getName(), Collectors.counting()));
        
        // Sort by count (descending), then by process name for deterministic ordering
        return processCounts.entrySet().stream()
            .sorted(Comparator
                .<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey))
            .limit(limit)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    // ========== ANOMALY DETECTION ==========

    /**
     * Identify processes with privilege escalation patterns (user -> root transitions).
     * Returns map of process name to escalation count.
     */
    public Map<String, Long> detectPrivilegeEscalation(Instant startInclusive, Instant endInclusive) {
        // Allow null timestamps to search entire dataset
        List<Event> relevantEvents = events;
        
        if (startInclusive != null && endInclusive != null) {
            if (startInclusive.isAfter(endInclusive)) {
                throw new IllegalArgumentException("Start time must not be after end time");
            }
            
            relevantEvents = events.stream()
                .filter(e -> !e.getTimestamp().isBefore(startInclusive) && 
                            !e.getTimestamp().isAfter(endInclusive))
                .collect(Collectors.toList());
        }
        
        // Find fork events where parent is user and child is root
        Map<String, Long> escalations = relevantEvents.stream()
            .filter(e -> e instanceof ForkEvent)
            .map(e -> (ForkEvent) e)
            .filter(fe -> {
                ProcessInfo parent = fe.getParentProcess();
                ProcessInfo child = fe.getChildProcess();
                return !parent.isRoot() && child.isRoot();
            })
            .collect(Collectors.groupingBy(
                fe -> fe.getParentProcess().getName(),
                Collectors.counting()
            ));
        
        // Sort by count (descending), then by process name
        return escalations.entrySet().stream()
            .sorted(Comparator
                .<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }


    /**
     * Detect unusually high-frequency events from specific processes (potential DoS or malware).
     * @param thresholdPerMinute events per minute threshold
     */
    public Map<String, Long> detectHighFrequencyProcesses(Instant startInclusive, Instant endInclusive, long thresholdPerMinute) {
        if (startInclusive == null || endInclusive == null) {
            throw new IllegalArgumentException("Start and end timestamps cannot be null");
        }
        if (startInclusive.isAfter(endInclusive)) {
            throw new IllegalArgumentException("Start time must not be after end time");
        }
        if (thresholdPerMinute <= 0) {
            throw new IllegalArgumentException("Threshold must be positive, got: " + thresholdPerMinute);
        }
        
        // Calculate time window duration in minutes
        long durationSeconds = endInclusive.getEpochSecond() - startInclusive.getEpochSecond();
        double durationMinutes = durationSeconds / 60.0;
        
        if (durationMinutes <= 0) {
            durationMinutes = 1.0 / 60.0; // At least 1 second
        }
        
        // Count events per process in time window
        Map<String, Long> processCounts = events.stream()
            .filter(e -> !e.getTimestamp().isBefore(startInclusive) && 
                        !e.getTimestamp().isAfter(endInclusive))
            .collect(Collectors.groupingBy(e -> e.getSubject().getName(), Collectors.counting()));
        
        // Calculate threshold for the window
        long thresholdForWindow = (long) Math.ceil(thresholdPerMinute * durationMinutes);
        
        // Filter processes exceeding threshold, sorted by count (descending)
        return processCounts.entrySet().stream()
            .filter(e -> e.getValue() > thresholdForWindow)
            .sorted(Comparator
                .<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    /**
     * Find processes accessing sensitive file locations (e.g., /etc/shadow, /var/log, registry).
     */
    public List<Event> findSensitiveFileAccess(List<String> sensitiveFilePaths) {
        if (sensitiveFilePaths == null) {
            throw new IllegalArgumentException("Sensitive file paths list cannot be null");
        }
        
        // Filter file events (read, write, execute, open, close) that access sensitive paths
        return events.stream()
            .filter(e -> isFileEvent(e))
            .filter(e -> {
                FileInfo fileInfo = (FileInfo) e.getObject();
                String path = fileInfo.getPath();
                
                // Check if path matches any sensitive path (exact match or prefix match)
                return sensitiveFilePaths.stream()
                    .anyMatch(sensitivePath -> 
                        path.equals(sensitivePath) || 
                        path.startsWith(sensitivePath + "/") ||
                        sensitivePath.startsWith(path + "/"));
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Helper method to check if an event is a file-related event.
     */
    private boolean isFileEvent(Event event) {
        return event instanceof ReadEvent || 
               event instanceof WriteEvent || 
               event instanceof ExecuteEvent || 
               event instanceof OpenEvent || 
               event instanceof CloseEvent;
    }

}
