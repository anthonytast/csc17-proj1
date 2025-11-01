package test_soclog.model;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.Instant;

import edu.hofstra.csc17.proj.soclog.model.entity.ProcessInfo;
import edu.hofstra.csc17.proj.soclog.model.event.EventType;
import edu.hofstra.csc17.proj.soclog.model.event.ForkEvent;

public class ForkEventTest {

    @Test
    public void testForkEvent_ValidConstruction() {
        ProcessInfo parent = new ProcessInfo("shell", 1001, "/bin/bash", ProcessInfo.PRIV_USER);
        ProcessInfo child = new ProcessInfo("worker", 1002, "/bin/bash", ProcessInfo.PRIV_USER);
        Instant timestamp = Instant.parse("2024-01-01T00:00:00Z");

        ForkEvent forkEvent = new ForkEvent(timestamp, parent, child, "status=success");

        assertEquals(EventType.fork, forkEvent.getType());
        assertEquals(timestamp, forkEvent.getTimestamp());
        assertEquals(parent, forkEvent.getParentProcess());
        assertEquals(child, forkEvent.getChildProcess());
        assertEquals("status=success", forkEvent.getFlags());
    }

    @Test
    public void testForkEvent_PrivilegeEscalation() {
        ProcessInfo userParent = new ProcessInfo("user_proc", 1000, "/bin/user", ProcessInfo.PRIV_USER);
        ProcessInfo rootChild = new ProcessInfo("root_proc", 1001, "/bin/sudo", ProcessInfo.PRIV_ROOT);
        Instant timestamp = Instant.now();

        ForkEvent forkEvent = new ForkEvent(timestamp, userParent, rootChild, "privilege_escalation=true");

        assertFalse(forkEvent.getParentProcess().isRoot());
        assertTrue(forkEvent.getChildProcess().isRoot());
        assertTrue(forkEvent.getFlags().contains("privilege_escalation"));
    }
}