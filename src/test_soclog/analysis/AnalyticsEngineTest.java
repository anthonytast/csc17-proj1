package test_soclog.analysis;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import edu.hofstra.csc17.proj.soclog.analysis.AnalyticsEngine;
import edu.hofstra.csc17.proj.soclog.model.event.Event;

public class AnalyticsEngineTest {

    private AnalyticsEngine analyticsEngine;
    private List<Event> sampleEvents;

    @Before
    public void setUp() {
        // TODO: Create sample events for testing
        sampleEvents = Arrays.asList();
        analyticsEngine = new AnalyticsEngine(sampleEvents);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUniqueEventsBetween_InvalidWindow() {
        // TODO: Test that start > end throws IllegalArgumentException
        Instant end = Instant.now();
        Instant start = end.plusSeconds(60);
        analyticsEngine.uniqueEvents(start, end);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTopKFrequentEvents_InvalidK_Zero() {
        // TODO: Test that k <= 0 throws IllegalArgumentException
        analyticsEngine.topKFrequentEvents(0);
    }
}
