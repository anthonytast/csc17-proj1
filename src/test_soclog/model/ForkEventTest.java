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
        assertEquals(parent, forkEvent.getSubject());
        assertEquals(child, forkEvent.getObject());
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
    
    @Test
    public void testForkEvent_UserToUser() {
        ProcessInfo parent = new ProcessInfo("parent", 1000, "/bin/parent", ProcessInfo.PRIV_USER);
        ProcessInfo child = new ProcessInfo("child", 1001, "/bin/child", ProcessInfo.PRIV_USER);
        Instant timestamp = Instant.parse("2024-01-01T00:00:00Z");

        ForkEvent forkEvent = new ForkEvent(timestamp, parent, child, "normal");

        assertFalse(forkEvent.getParentProcess().isRoot());
        assertFalse(forkEvent.getChildProcess().isRoot());
    }
    
    @Test
    public void testForkEvent_RootToRoot() {
        ProcessInfo parent = new ProcessInfo("root_parent", 1, "/sbin/init", ProcessInfo.PRIV_ROOT);
        ProcessInfo child = new ProcessInfo("root_child", 2, "/sbin/daemon", ProcessInfo.PRIV_ROOT);
        Instant timestamp = Instant.parse("2024-01-01T00:00:00Z");

        ForkEvent forkEvent = new ForkEvent(timestamp, parent, child, "system");

        assertTrue(forkEvent.getParentProcess().isRoot());
        assertTrue(forkEvent.getChildProcess().isRoot());
    }
    
    @Test
    public void testForkEvent_RootToUser() {
        ProcessInfo parent = new ProcessInfo("root_parent", 1, "/sbin/init", ProcessInfo.PRIV_ROOT);
        ProcessInfo child = new ProcessInfo("user_child", 1000, "/bin/user", ProcessInfo.PRIV_USER);
        Instant timestamp = Instant.parse("2024-01-01T00:00:00Z");

        ForkEvent forkEvent = new ForkEvent(timestamp, parent, child, "drop_privileges");

        assertTrue(forkEvent.getParentProcess().isRoot());
        assertFalse(forkEvent.getChildProcess().isRoot());
    }
    
    @Test
    public void testForkEvent_Equals() {
        ProcessInfo parent = new ProcessInfo("parent", 1000, "/bin/parent", ProcessInfo.PRIV_USER);
        ProcessInfo child = new ProcessInfo("child", 1001, "/bin/child", ProcessInfo.PRIV_USER);
        Instant timestamp = Instant.parse("2024-01-01T00:00:00Z");

        ForkEvent event1 = new ForkEvent(timestamp, parent, child, "flags");
        ForkEvent event2 = new ForkEvent(timestamp, parent, child, "flags");

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }
    
    @Test
    public void testForkEvent_NotEquals_DifferentTimestamp() {
        ProcessInfo parent = new ProcessInfo("parent", 1000, "/bin/parent", ProcessInfo.PRIV_USER);
        ProcessInfo child = new ProcessInfo("child", 1001, "/bin/child", ProcessInfo.PRIV_USER);
        
        ForkEvent event1 = new ForkEvent(Instant.parse("2024-01-01T00:00:00Z"), parent, child, "flags");
        ForkEvent event2 = new ForkEvent(Instant.parse("2024-01-01T00:00:01Z"), parent, child, "flags");

        assertNotEquals(event1, event2);
    }
    
    @Test
    public void testForkEvent_NotEquals_DifferentChild() {
        ProcessInfo parent = new ProcessInfo("parent", 1000, "/bin/parent", ProcessInfo.PRIV_USER);
        ProcessInfo child1 = new ProcessInfo("child1", 1001, "/bin/child", ProcessInfo.PRIV_USER);
        ProcessInfo child2 = new ProcessInfo("child2", 1002, "/bin/child", ProcessInfo.PRIV_USER);
        Instant timestamp = Instant.parse("2024-01-01T00:00:00Z");

        ForkEvent event1 = new ForkEvent(timestamp, parent, child1, "flags");
        ForkEvent event2 = new ForkEvent(timestamp, parent, child2, "flags");

        assertNotEquals(event1, event2);
    }
}