# SOC Log Analysis Toolkit

## Project Overview

You are part of Hofstra's Security Operations Center (SOC) team responding to suspicious activity on campus systems. Analysts drown in millions of heterogeneous application and system logs every day; your team needs a toolkit that ingests raw CSV logs, enforces data contracts, deduplicates entries, and surfaces the events that demand human attention.

This project implements a production-quality log analysis library with two main components:

### Component 1: Parser
The **Parser** component reads from multiple CSV files and produces one collection of validated `Event` objects. Your choice of data structure for holding events must be justified based on memory efficiency, deduplication requirements, and analytics access patterns.

### Component 2: Analyzer
The **Analyzer** component performs comprehensive SOC analytics on the parsed event data, providing both basic statistical analysis and advanced security monitoring capabilities.

## Component 1: Parser Requirements

### Input Processing
- **Multi-file streaming**: Support processing multiple CSV files
- **File handling**: Continue processing remaining files if one is missing; handle empty files gracefully

### CSV Record Validation

#### **Record Structure**
Each CSV record must have exactly 5 fields: `event_type,event_timestamp,event_flags,event_subject,event_object`

#### **Event Type Validation**
- **Valid types**: `read`, `write`, `execute`, `sendto`, `receivefrom`, `open`, `close`, `fork`
- **Case handling**: Event types are case-insensitive
- **Invalid handling**: Reject any other event types and record rejection reason

#### **Timestamp Validation**
- **Format**: ISO-8601 strings in UTC (e.g., `2024-01-15T09:24:05Z`)
- **Chronological order**: Events within each file must be in temporal order
- **Out-of-order handling**: If timestamp is earlier than previous event, reject and record violation
- **Malformed timestamps**: Reject records with unparseable timestamps

#### **Subject Field Validation (ProcessInfo)**
All events have a process as subject. Required format: semicolon-separated key=value pairs.
- **Required fields**: `name`, `pid`, `path`, `privilege`
- **Example**: `name=backupd;pid=4242;path=/usr/local/bin/backupd;privilege=root`
- **Privilege values**: Must be exactly `user` or `root` (case-sensitive)
- **PID validation**: Must be a valid positive integer
- **Missing fields**: Reject if any required field is missing or malformed

#### **Object Field Validation (varies by event type)**

**File Events** (`read`, `write`, `execute`, `open`, `close`):
- **Required fields**: `path`, `fd`, `permissions`
- **Example**: `path=/var/data/report.txt;fd=12;permissions=640`
- **Permissions format**: 3 digit octal strings (e.g., `640`, `755`, `775`)
- **File descriptor**: Must be a valid non-negative integer

**Network Events** (`sendto`, `receivefrom`):
- **Required fields**: `ip`, `port`, `protocol`
- **Example**: `ip=192.168.1.100;port=80;protocol=TCP`
- **Port validation**: Must be integer between 1-65535
- **Protocol validation**: Must be `TCP`, `UDP`, or `ICMP`
- **IP validation**: Must be valid IPv4 address format

**Fork Events** (`fork`):
- **Required fields**: Same as subject (ProcessInfo for child process)
- **Example**: `name=worker;pid=1002;path=/bin/bash;privilege=user`
- **Validation**: Apply same rules as subject ProcessInfo

### Error Handling and Tracking
- **Continue processing**: Don't stop parsing on individual record failures
- **Record all rejections**: Track specific validation failure reasons
- **Error categorization**: Group similar errors for reporting
- **Must reject and track**:
  - Invalid event types
  - Malformed timestamps
  - Out-of-order timestamps
  - Missing required fields in subject/object
  - Invalid privilege values (not "user" or "root")
  - Invalid PID (non-integer or negative)
  - Invalid file permissions (non-octal format)
  - Invalid network ports (outside 1-65535 range)
  - Invalid IP addresses
  - Malformed key=value pairs (missing = or ;)

### Object Model Design
Given an abstract `ObjectInfo` base class to unify different object types, implement the following classes:
- **ProcessInfo**: Contains name, PID, path, privilege (extends ObjectInfo)
- **FileInfo**: Contains path, file descriptor, permissions (extends ObjectInfo)
- **NetworkInfo**: Contains IP address, port, protocol (extends ObjectInfo)

Given an abstract `Event` base class with fields: type, timestamp, subject (ProcessInfo), object (ObjectInfo), flags. Implement concrete subclasses: `ReadEvent`, `WriteEvent`, `ExecuteEvent`, `SendToEvent`, `ReceiveFromEvent`, `OpenEvent`, `CloseEvent`, `ForkEvent`.

### Parser Output
The parser must produce:
1. **Valid Events Collection**: Data structure holding all successfully parsed and validated events (The project skeleton uses ArrayList as a placeholder but you can change it)
2. **Error Collection**: Data structure tracking all rejected records with reasons
3. **Parsing Summary**: Including:
   - Total records processed (excluding headers)
   - Valid events count
   - Rejection rate (rejected/total * 100)
   - Top 3 rejection reasons with counts
   - Time range (earliest to latest event timestamp)
   - Processing time

**Data Structure Justification**: Document your choice of data structure for holding events based on:
- Memory efficiency for large datasets
- Deduplication strategy support
- Analytics query performance
- Iteration and access patterns

---

## Component 2: Analyzer Requirements

The `AnalyticsEngine` performs comprehensive analysis on parsed event data. All methods below are required for full credit.

### Analytics Methods

#### **`List<Event> uniqueEvents()`**
Return all unique events from the dataset.
- **Deduplication criteria**: Events are duplicates if ALL fields match exactly
- **Return**: List of unique events in chronological order

#### **`List<Event> uniqueEvents(Instant startInclusive, Instant endInclusive)`**
Return unique events within specified time window.
- **Time filtering**: Include events where timestamp falls within inclusive range [start, end]
- **Parameter validation**: Throw `IllegalArgumentException` if start > end
- **Deduplication**: Apply same uniqueness criteria as above
- **Return**: Chronologically ordered list of unique events in window

#### **`List<Event> topKFrequentEvents(int k)`**
Return the k most frequent events across entire dataset.
- **Frequency calculation**: Count based on combination of event type + subject + object
- **Tie breaking**: When frequencies equal, sort lexicographically by frequency key
- **Parameter validation**: Throw `IllegalArgumentException` if k ≤ 0 or k > unique event count
- **Return**: Most frequent events first, deterministic ordering for ties

#### **`List<Event> topKFrequentEvents(Instant startInclusive, Instant endInclusive, int k)`**
Return the k most frequent events within specified time window.
- **Combine logic**: Apply time filtering from time window method + frequency ranking
- **Parameter validation**: Validate both time window and k parameter constraints

#### **`Map<EventType, Long> countByEventType()`**
Count events grouped by event type.
- **Grouping**: Use EventType enum values as keys
- **Counting**: Include all events (not just unique ones)
- **Empty handling**: Return empty map for empty dataset
- **Return**: Map with event type counts

#### **`Long countByEventType(EventType type)`**
Count events of a specific event type.
- **Type filtering**: Count only events matching the specified EventType
- **Return**: Number of events of the given type

#### **`Map<String, Long> topProcessesByWindow(Instant startInclusive, Instant endInclusive, int limit)`**
Find most active processes within time window.
- **Process identification**: Group by process name (from subject ProcessInfo)
- **Time filtering**: Only count events within specified window
- **Activity metric**: Count of events generated by each process
- **Ranking**: Return top `limit` processes by event count
- **Tie handling**: Break ties deterministically (e.g., alphabetical by process name)

#### **`Map<String, Long> detectPrivilegeEscalation(Instant startInclusive, Instant endInclusive)`**
Detect privilege escalation patterns in fork events.
- **Detection criteria**: Fork events where parent has `privilege=user` and child has `privilege=root`
- **Grouping**: Count escalations by parent process name
- **Time filtering**: Only analyze events within specified window
- **Return**: Map of process names to escalation counts

#### **`Map<String, Long> detectHighFrequencyProcesses(Instant startInclusive, Instant endInclusive, long thresholdPerMinute)`**
Detect unusually high-frequency events from specific processes.
- **Frequency calculation**: Events per minute for each process within time window
- **Threshold application**: Only return processes exceeding the specified threshold
- **Process identification**: Group by process name (not PID)
- **Return**: Map of process names to their event frequencies that exceed threshold

#### **`List<Event> findSensitiveFileAccess(List<String> sensitiveFilePaths)`**
Find processes accessing sensitive file locations.
- **Event filtering**: Include only file-related events (`read`, `write`, `execute`, `open`)
- **Path matching**: Check if file path exactly matches or starts with sensitive paths
- **Case sensitivity**: File path matching is case-sensitive
- **Return**: Events accessing any of the specified sensitive file paths

### General Analytics Requirements
- **Null handling**: Throw `IllegalArgumentException` for null parameters
- **Empty datasets**: Return empty results (not null) for empty input
- **Parameter validation**: Validate all input parameters and throw descriptive exceptions
- **Deterministic results**: Ensure consistent output for same inputs (important for tie-breaking)

---

## Testing Requirements

### Coverage Targets
- **Unit testing**: Test each method you implemented
- **Component testing**: Test parser and analyzer independently
- **Integration testing**: Test end-to-end workflow with sample data

### Required Test Scenarios
#### Parser Testing
- **Valid data**: Each event type with correctly formatted data
- **Invalid timestamps**: Malformed, out-of-order, future dates
- **Invalid event types**: Unsupported types, case variations
- **Malformed fields**: Missing semicolons, invalid privilege values, bad IPs
- **Edge cases**: Empty files, single events, boundary values
- **Large datasets**: Performance testing with substantial data volumes

#### Analyzer Testing
- **Empty datasets**: Verify proper handling of empty input
- **Single event**: Test methods with minimal data
- **Boundary conditions**: Test parameter validation and edge cases
- **Time window logic**: Verify inclusive/exclusive boundary handling
- **Deduplication**: Confirm exact duplicate detection
- **Sorting stability**: Verify deterministic tie-breaking

### Test Data
- **Use provided fixtures**: Build tests using CSV files in `data/` directory
- **Create minimal examples**: Simple test cases for specific validation rules
- **Document test intent**: Each test should clearly state what it validates

## Submission

- Source code: Submit to Gradescope as a ZIP of your /src folder. Exclude build artifacts.
- A design report: Submit to Canvas, covering design decisions (e.g., data structure used to store Event, Analyzer method design, etc.), complexity analysis

## Grading Rubric

- **50%** Correctness: Parser validation, analyzer accuracy, error handling
- **25%** Design: Architecture clarity, API design rationale
- **25%** Testing: Coverage metrics, test quality, edge case handling
