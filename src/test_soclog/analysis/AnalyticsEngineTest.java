package test_soclog.analysis;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import edu.hofstra.csc17.proj.soclog.analysis.AnalyticsEngine;
import edu.hofstra.csc17.proj.soclog.model.entity.FileInfo;
import edu.hofstra.csc17.proj.soclog.model.entity.ProcessInfo;
import edu.hofstra.csc17.proj.soclog.model.event.*;

public class AnalyticsEngineTest {

    private AnalyticsEngine analyticsEngine;
    private AnalyticsEngine emptyEngine;
    private List<Event> sampleEvents;
    private ProcessInfo userProcess;
    private ProcessInfo rootProcess;
    private FileInfo sampleFile;
    private Instant baseTime;

    @Before
    public void setUp() {
        // Create test data
        userProcess = new ProcessInfo("test_proc", 100, "/bin/test", ProcessInfo.PRIV_USER);
        rootProcess = new ProcessInfo("root_proc", 1, "/sbin/root", ProcessInfo.PRIV_ROOT);
        sampleFile = new FileInfo("/tmp/test.txt", 5, "644");
        baseTime = Instant.parse("2024-01-01T00:00:00Z");
        
        // Create sample events
        sampleEvents = new ArrayList<>();
        
        // Add some read events (with duplicates)
        sampleEvents.add(new ReadEvent(baseTime, userProcess, sampleFile, "mode=r"));
        sampleEvents.add(new ReadEvent(baseTime, userProcess, sampleFile, "mode=r")); // duplicate
        sampleEvents.add(new ReadEvent(baseTime.plusSeconds(10), userProcess, sampleFile, "mode=r"));
        
        // Add write event
        sampleEvents.add(new WriteEvent(baseTime.plusSeconds(20), userProcess, sampleFile, "mode=w"));
        
        // Add execute event
        FileInfo execFile = new FileInfo("/usr/bin/script.sh", 10, "755");
        sampleEvents.add(new ExecuteEvent(baseTime.plusSeconds(30), rootProcess, execFile, "exit=0"));
        
        // Add fork events
        ProcessInfo childUser = new ProcessInfo("child_proc", 101, "/bin/child", ProcessInfo.PRIV_USER);
        ProcessInfo childRoot = new ProcessInfo("child_root", 102, "/bin/child", ProcessInfo.PRIV_ROOT);
        sampleEvents.add(new ForkEvent(baseTime.plusSeconds(40), userProcess, childUser, "status=success"));
        sampleEvents.add(new ForkEvent(baseTime.plusSeconds(50), userProcess, childRoot, "escalation=true"));
        
        analyticsEngine = new AnalyticsEngine(sampleEvents);
        emptyEngine = new AnalyticsEngine(new ArrayList<>());
    }

    // uniqueEvents Tests
    
    @Test
    public void testUniqueEvents_RemovesDuplicates() {
        List<Event> unique = analyticsEngine.uniqueEvents();
        // Should have 6 unique events (7 total - 1 duplicate)
        assertEquals(6, unique.size());
    }
    
    @Test
    public void testUniqueEvents_EmptyDataset() {
        List<Event> unique = emptyEngine.uniqueEvents();
        assertNotNull(unique);
        assertEquals(0, unique.size());
    }
    
    @Test
    public void testUniqueEventsBetween_FiltersByTime() {
        Instant start = baseTime.plusSeconds(15);
        Instant end = baseTime.plusSeconds(45);
        List<Event> filtered = analyticsEngine.uniqueEvents(start, end);
        
        assertEquals(3, filtered.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUniqueEventsBetween_InvalidWindow() {
        Instant end = baseTime;
        Instant start = baseTime.plusSeconds(60);
        analyticsEngine.uniqueEvents(start, end);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testUniqueEventsBetween_NullStart() {
        analyticsEngine.uniqueEvents(null, baseTime);
    }

    // topKFrequentEvents tests
    
    @Test
    public void testTopKFrequentEvents_ReturnsTopK() {
        List<Event> topK = analyticsEngine.topKFrequentEvents(2);
        assertEquals(2, topK.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTopKFrequentEvents_InvalidK_Zero() {
        analyticsEngine.topKFrequentEvents(0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testTopKFrequentEvents_InvalidK_Negative() {
        analyticsEngine.topKFrequentEvents(-1);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testTopKFrequentEvents_InvalidK_TooLarge() {
        analyticsEngine.topKFrequentEvents(100); // More than unique events
    }
    
    @Test
    public void testTopKFrequentEvents_WithTimeWindow() {
        Instant start = baseTime;
        Instant end = baseTime.plusSeconds(60);
        List<Event> topK = analyticsEngine.topKFrequentEvents(start, end, 2);
        assertEquals(2, topK.size());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testTopKFrequentEvents_TimeWindow_InvalidWindow() {
        Instant start = baseTime.plusSeconds(60);
        Instant end = baseTime;
        analyticsEngine.topKFrequentEvents(start, end, 1);
    }

    // countByEventType tests
    
    @Test
    public void testCountByEventType_AllTypes() {
        Map<EventType, Long> counts = analyticsEngine.countByEventType();
        assertNotNull(counts);
        assertTrue(counts.containsKey(EventType.read));
        assertEquals(Long.valueOf(3), counts.get(EventType.read));
        assertEquals(Long.valueOf(1), counts.get(EventType.write));
        assertEquals(Long.valueOf(1), counts.get(EventType.execute));
        assertEquals(Long.valueOf(2), counts.get(EventType.fork));
    }
    
    @Test
    public void testCountByEventType_SpecificType() {
        Long readCount = analyticsEngine.countByEventType(EventType.read);
        assertEquals(Long.valueOf(3), readCount);
        
        Long writeCount = analyticsEngine.countByEventType(EventType.write);
        assertEquals(Long.valueOf(1), writeCount);
    }
    
    @Test
    public void testCountByEventType_NonExistentType() {
        Long count = analyticsEngine.countByEventType(EventType.close);
        assertEquals(Long.valueOf(0), count);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCountByEventType_NullType() {
        analyticsEngine.countByEventType(null);
    }
    
    @Test
    public void testCountByEventType_EmptyDataset() {
        Map<EventType, Long> counts = emptyEngine.countByEventType();
        assertNotNull(counts);
        assertTrue(counts.isEmpty());
    }

    // topProcessesByWindow tests
    
    @Test
    public void testTopProcessesByWindow_ReturnsTopProcesses() {
        Instant start = baseTime;
        Instant end = baseTime.plusSeconds(60);
        Map<String, Long> topProcesses = analyticsEngine.topProcessesByWindow(start, end, 2);
        
        assertNotNull(topProcesses);
        assertTrue(topProcesses.size() <= 2);
        assertTrue(topProcesses.containsKey("test_proc")); // Has most events
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testTopProcessesByWindow_InvalidLimit_Zero() {
        analyticsEngine.topProcessesByWindow(baseTime, baseTime.plusSeconds(60), 0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testTopProcessesByWindow_InvalidWindow() {
        Instant start = baseTime.plusSeconds(60);
        Instant end = baseTime;
        analyticsEngine.topProcessesByWindow(start, end, 1);
    }

    // detectPrivilegeEscalation tests
    
    @Test
    public void testDetectPrivilegeEscalation_FindsEscalations() {
        Map<String, Long> escalations = analyticsEngine.detectPrivilegeEscalation(baseTime, baseTime.plusSeconds(60));
        
        assertNotNull(escalations);
        assertTrue(escalations.containsKey("test_proc"));
        assertEquals(Long.valueOf(1), escalations.get("test_proc"));
    }
    
    @Test
    public void testDetectPrivilegeEscalation_NoEscalations() {
        // Create events without escalation
        List<Event> noEscalationEvents = new ArrayList<>();
        ProcessInfo child = new ProcessInfo("child", 200, "/bin/child", ProcessInfo.PRIV_USER);
        noEscalationEvents.add(new ForkEvent(baseTime, userProcess, child, "normal"));
        
        AnalyticsEngine engine = new AnalyticsEngine(noEscalationEvents);
        Map<String, Long> escalations = engine.detectPrivilegeEscalation(baseTime, baseTime.plusSeconds(60));
        
        assertNotNull(escalations);
        assertTrue(escalations.isEmpty());
    }
    
    @Test
    public void testDetectPrivilegeEscalation_NullTimestamps() {
        // Should work with null timestamps (search entire dataset)
        Map<String, Long> escalations = analyticsEngine.detectPrivilegeEscalation(null, null);
        assertNotNull(escalations);
    }

    // detectHighFrequencyProcesses tests
    
    @Test
    public void testDetectHighFrequencyProcesses_FindsHighFrequency() {
        // low threshold
        Instant start = baseTime;
        Instant end = baseTime.plusSeconds(60);
        Map<String, Long> highFreq = analyticsEngine.detectHighFrequencyProcesses(start, end, 1);
        
        assertNotNull(highFreq);
        assertTrue(highFreq.containsKey("test_proc"));
    }
    
    @Test
    public void testDetectHighFrequencyProcesses_NoExceedingThreshold() {
        // high threshold
        Instant start = baseTime;
        Instant end = baseTime.plusSeconds(60);
        Map<String, Long> highFreq = analyticsEngine.detectHighFrequencyProcesses(start, end, 1000);
        
        assertNotNull(highFreq);
        assertTrue(highFreq.isEmpty());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testDetectHighFrequencyProcesses_InvalidThreshold() {
        analyticsEngine.detectHighFrequencyProcesses(baseTime, baseTime.plusSeconds(60), 0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testDetectHighFrequencyProcesses_InvalidWindow() {
        Instant start = baseTime.plusSeconds(60);
        Instant end = baseTime;
        analyticsEngine.detectHighFrequencyProcesses(start, end, 10);
    }

    // findSensitiveFileAccess tests
    
    @Test
    public void testFindSensitiveFileAccess_FindsAccess() {
        List<String> sensitivePaths = Arrays.asList("/tmp/test.txt");
        List<Event> sensitiveAccess = analyticsEngine.findSensitiveFileAccess(sensitivePaths);
        
        assertNotNull(sensitiveAccess);
        assertTrue(sensitiveAccess.size() > 0);
    }
    
    @Test
    public void testFindSensitiveFileAccess_NoMatches() {
        List<String> sensitivePaths = Arrays.asList("/etc/shadow", "/var/log");
        List<Event> sensitiveAccess = analyticsEngine.findSensitiveFileAccess(sensitivePaths);
        
        assertNotNull(sensitiveAccess);
        assertEquals(0, sensitiveAccess.size());
    }
    
    @Test
    public void testFindSensitiveFileAccess_PrefixMatching() {
        List<String> sensitivePaths = Arrays.asList("/tmp");
        List<Event> sensitiveAccess = analyticsEngine.findSensitiveFileAccess(sensitivePaths);
        
        assertNotNull(sensitiveAccess);
        assertTrue(sensitiveAccess.size() > 0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testFindSensitiveFileAccess_NullList() {
        analyticsEngine.findSensitiveFileAccess(null);
    }
    
    @Test
    public void testFindSensitiveFileAccess_EmptyList() {
        List<Event> sensitiveAccess = analyticsEngine.findSensitiveFileAccess(new ArrayList<>());
        assertNotNull(sensitiveAccess);
        assertEquals(0, sensitiveAccess.size());
    }
    
    @Test
    public void testFindSensitiveFileAccess_EmptyDataset() {
        List<String> sensitivePaths = Arrays.asList("/etc/shadow");
        List<Event> sensitiveAccess = emptyEngine.findSensitiveFileAccess(sensitivePaths);
        
        assertNotNull(sensitiveAccess);
        assertEquals(0, sensitiveAccess.size());
    }
}
