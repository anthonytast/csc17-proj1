package test_soclog.model;

import org.junit.Test;

import edu.hofstra.csc17.proj.soclog.model.entity.ProcessInfo;

import static org.junit.Assert.*;

public class ProcessInfoTest {

    @Test
    public void testConstructor_ValidPrivilegeUser() {
        ProcessInfo process = new ProcessInfo("test", 123, "/bin/test", ProcessInfo.PRIV_USER);
        assertEquals("test", process.getName());
        assertEquals(Integer.valueOf(123), process.getPid());
        assertEquals("/bin/test", process.getModulePath());
        assertEquals(ProcessInfo.PRIV_USER, process.getPrivilege());
        assertFalse(process.isRoot());
    }
    
    @Test
    public void testConstructor_ValidPrivilegeRoot() {
        ProcessInfo process = new ProcessInfo("root_proc", 1, "/sbin/init", ProcessInfo.PRIV_ROOT);
        assertEquals("root_proc", process.getName());
        assertEquals(Integer.valueOf(1), process.getPid());
        assertEquals("/sbin/init", process.getModulePath());
        assertEquals(ProcessInfo.PRIV_ROOT, process.getPrivilege());
        assertTrue(process.isRoot());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_InvalidPrivilege() {
        new ProcessInfo("test", 123, "/bin/test", "invalid_privilege");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NullName() {
        new ProcessInfo(null, 123, "/bin/test", ProcessInfo.PRIV_USER);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_EmptyName() {
        new ProcessInfo("", 123, "/bin/test", ProcessInfo.PRIV_USER);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NullPid() {
        new ProcessInfo("test", null, "/bin/test", ProcessInfo.PRIV_USER);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_ZeroPid() {
        new ProcessInfo("test", 0, "/bin/test", ProcessInfo.PRIV_USER);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NegativePid() {
        new ProcessInfo("test", -1, "/bin/test", ProcessInfo.PRIV_USER);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NullPath() {
        new ProcessInfo("test", 123, null, ProcessInfo.PRIV_USER);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_EmptyPath() {
        new ProcessInfo("test", 123, "", ProcessInfo.PRIV_USER);
    }
    
    @Test
    public void testGetCanonicalId() {
        ProcessInfo process = new ProcessInfo("test", 123, "/bin/test", ProcessInfo.PRIV_USER);
        String canonicalId = process.getCanonicalId();
        assertNotNull(canonicalId);
        assertTrue(canonicalId.contains("123"));
    }
    
    @Test
    public void testGetDisplayName() {
        ProcessInfo process = new ProcessInfo("test", 123, "/bin/test", ProcessInfo.PRIV_USER);
        String displayName = process.getDisplayName();
        assertNotNull(displayName);
        assertEquals("test", displayName);
    }
    
    @Test
    public void testEquals_SameValues() {
        ProcessInfo p1 = new ProcessInfo("test", 123, "/bin/test", ProcessInfo.PRIV_USER);
        ProcessInfo p2 = new ProcessInfo("test", 123, "/bin/test", ProcessInfo.PRIV_USER);
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }
    
    @Test
    public void testEquals_DifferentValues() {
        ProcessInfo p1 = new ProcessInfo("test", 123, "/bin/test", ProcessInfo.PRIV_USER);
        ProcessInfo p2 = new ProcessInfo("test", 456, "/bin/test", ProcessInfo.PRIV_USER);
        assertNotEquals(p1, p2);
    }
}