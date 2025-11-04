package edu.hofstra.csc17.proj.soclog.ingest.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import edu.hofstra.csc17.proj.soclog.model.entity.FileInfo;
import edu.hofstra.csc17.proj.soclog.model.entity.NetworkInfo;
import edu.hofstra.csc17.proj.soclog.model.entity.ProcessInfo;
import edu.hofstra.csc17.proj.soclog.model.event.*;

public class EventParser {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    private static final Pattern OCTAL_PATTERN = Pattern.compile("^[0-7]{3}$");

    public static final class ParseResult {
        private final List<Event> events;
        private final List<String> errors;

        public ParseResult(List<Event> events, List<String> errors) {
            this.events = events;
            this.errors = errors;
        }

        public List<Event> getEvents() {
            return events;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    public ParseResult parse(Path path) throws IOException {
        List<Event> events = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Instant lastTimestamp = null;
        
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            long lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    // Parse CSV record (5 fields)
                    String[] fields = parseCsvLine(line);
                    
                    if (fields.length != 5) {
                        errors.add(String.format("Line %d: Expected 5 fields, got %d", 
                            lineNumber, fields.length));
                        continue;
                    }
                    
                    String eventTypeStr = fields[0].trim();
                    String timestampStr = fields[1].trim();
                    String flags = fields[2].trim();
                    String subjectStr = fields[3].trim();
                    String objectStr = fields[4].trim();
                    
                    // Validate and parse event type (case-insensitive)
                    EventType eventType;
                    try {
                        eventType = EventType.valueOf(eventTypeStr.toLowerCase());
                    } catch (IllegalArgumentException e) {
                        errors.add(String.format("Line %d: Invalid event type '%s'", 
                            lineNumber, eventTypeStr));
                        continue;
                    }
                    
                    // Validate and parse timestamp
                    Instant timestamp;
                    try {
                        timestamp = Instant.parse(timestampStr);
                    } catch (DateTimeParseException e) {
                        errors.add(String.format("Line %d: Malformed timestamp '%s'", 
                            lineNumber, timestampStr));
                        continue;
                    }
                    
                    // Check chronological order
                    if (lastTimestamp != null && timestamp.isBefore(lastTimestamp)) {
                        errors.add(String.format("Line %d: Timestamp %s is before previous timestamp %s", 
                            lineNumber, timestamp, lastTimestamp));
                        continue;
                    }
                    lastTimestamp = timestamp;
                    
                    // Parse subject (always ProcessInfo)
                    ProcessInfo subject;
                    try {
                        subject = parseProcessInfo(subjectStr);
                    } catch (Exception e) {
                        errors.add(String.format("Line %d: Invalid subject: %s", 
                            lineNumber, e.getMessage()));
                        continue;
                    }
                    
                    // Create event based on type
                    try {
                        Event event = createEvent(eventType, timestamp, subject, objectStr, flags, lineNumber);
                        events.add(event);
                    } catch (Exception e) {
                        errors.add(String.format("Line %d: Invalid object for %s event: %s", 
                            lineNumber, eventType, e.getMessage()));
                    }
                    
                } catch (Exception e) {
                    errors.add(String.format("Line %d: Parse error: %s", 
                        lineNumber, e.getMessage()));
                }
            }
        }
        
        return new ParseResult(events, errors);
    }
    
    private String[] parseCsvLine(String line) {
        // Simple CSV parser (assumes no quotes or escaping)
        return line.split(",", -1);
    }
    
    private Event createEvent(EventType eventType, Instant timestamp, ProcessInfo subject, 
                             String objectStr, String flags, long lineNumber) throws Exception {
        switch (eventType) {
            case read:
                return new ReadEvent(timestamp, subject, parseFileInfo(objectStr), flags);
            case write:
                return new WriteEvent(timestamp, subject, parseFileInfo(objectStr), flags);
            case execute:
                return new ExecuteEvent(timestamp, subject, parseFileInfo(objectStr), flags);
            case open:
                return new OpenEvent(timestamp, subject, parseFileInfo(objectStr), flags);
            case close:
                return new CloseEvent(timestamp, subject, parseFileInfo(objectStr), flags);
            case sendto:
                return new SendToEvent(timestamp, subject, parseNetworkInfo(objectStr), flags);
            case receivefrom:
                return new ReceiveFromEvent(timestamp, subject, parseNetworkInfo(objectStr), flags);
            case fork:
                return new ForkEvent(timestamp, subject, parseProcessInfo(objectStr), flags);
            default:
                throw new IllegalArgumentException("Unsupported event type: " + eventType);
        }
    }
    
    private ProcessInfo parseProcessInfo(String data) throws Exception {
        Map<String, String> fields = parseKeyValuePairs(data);
        
        String name = fields.get("name");
        String pidStr = fields.get("pid");
        String path = fields.get("path");
        String privilege = fields.get("privilege");
        
        if (name == null || name.isEmpty()) {
            throw new Exception("Missing or empty 'name' field");
        }
        if (pidStr == null || pidStr.isEmpty()) {
            throw new Exception("Missing or empty 'pid' field");
        }
        if (path == null || path.isEmpty()) {
            throw new Exception("Missing or empty 'path' field");
        }
        if (privilege == null || privilege.isEmpty()) {
            throw new Exception("Missing or empty 'privilege' field");
        }
        
        // Validate PID
        Integer pid;
        try {
            pid = Integer.parseInt(pidStr);
            if (pid <= 0) {
                throw new Exception("PID must be positive, got: " + pid);
            }
        } catch (NumberFormatException e) {
            throw new Exception("Invalid PID format: " + pidStr);
        }
        
        // Validate privilege
        if (!ProcessInfo.PRIV_USER.equals(privilege) && !ProcessInfo.PRIV_ROOT.equals(privilege)) {
            throw new Exception("Privilege must be 'user' or 'root', got: " + privilege);
        }
        
        return new ProcessInfo(name, pid, path, privilege);
    }
    
    private FileInfo parseFileInfo(String data) throws Exception {
        Map<String, String> fields = parseKeyValuePairs(data);
        
        String path = fields.get("path");
        String fdStr = fields.get("fd");
        String permissions = fields.get("permissions");
        
        if (path == null || path.isEmpty()) {
            throw new Exception("Missing or empty 'path' field");
        }
        if (fdStr == null || fdStr.isEmpty()) {
            throw new Exception("Missing or empty 'fd' field");
        }
        if (permissions == null || permissions.isEmpty()) {
            throw new Exception("Missing or empty 'permissions' field");
        }
        
        // Validate file descriptor
        Integer fd;
        try {
            fd = Integer.parseInt(fdStr);
            if (fd < 0) {
                throw new Exception("File descriptor must be non-negative, got: " + fd);
            }
        } catch (NumberFormatException e) {
            throw new Exception("Invalid file descriptor format: " + fdStr);
        }
        
        // Validate permissions (3-digit octal)
        if (!OCTAL_PATTERN.matcher(permissions).matches()) {
            throw new Exception("Permissions must be 3-digit octal format, got: " + permissions);
        }
        
        return new FileInfo(path, fd, permissions);
    }
    
    private NetworkInfo parseNetworkInfo(String data) throws Exception {
        Map<String, String> fields = parseKeyValuePairs(data);
        
        String ip = fields.get("ip");
        String portStr = fields.get("port");
        String protocol = fields.get("protocol");
        
        if (ip == null || ip.isEmpty()) {
            throw new Exception("Missing or empty 'ip' field");
        }
        if (portStr == null || portStr.isEmpty()) {
            throw new Exception("Missing or empty 'port' field");
        }
        if (protocol == null || protocol.isEmpty()) {
            throw new Exception("Missing or empty 'protocol' field");
        }
        
        // Validate IP address
        if (!IPV4_PATTERN.matcher(ip).matches()) {
            throw new Exception("Invalid IPv4 address format: " + ip);
        }
        
        // Validate port
        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port < 0 || port > 65535) {
                throw new Exception("Port must be between 0-65535, got: " + port);
            }
        } catch (NumberFormatException e) {
            throw new Exception("Invalid port format: " + portStr);
        }
        
        // Validate protocol (case-insensitive)
        String protocolUpper = protocol.toUpperCase();
        if (!protocolUpper.equals("TCP") && !protocolUpper.equals("UDP") && !protocolUpper.equals("ICMP")) {
            throw new Exception("Protocol must be TCP, UDP, or ICMP, got: " + protocol);
        }
        
        return new NetworkInfo(ip, port, protocolUpper);
    }
    
    private Map<String, String> parseKeyValuePairs(String data) throws Exception {
        Map<String, String> result = new HashMap<>();
        
        if (data == null || data.trim().isEmpty()) {
            throw new Exception("Empty key=value data");
        }
        
        String[] pairs = data.split(";");
        for (String pair : pairs) {
            if (pair.trim().isEmpty()) {
                continue;
            }
            
            int equalsIndex = pair.indexOf('=');
            if (equalsIndex == -1) {
                throw new Exception("Malformed key=value pair (missing '='): " + pair);
            }
            
            String key = pair.substring(0, equalsIndex).trim();
            String value = pair.substring(equalsIndex + 1).trim();
            
            if (key.isEmpty()) {
                throw new Exception("Empty key in key=value pair: " + pair);
            }
            
            result.put(key, value);
        }
        
        return result;
    }
}
