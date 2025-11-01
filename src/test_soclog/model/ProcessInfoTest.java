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

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_InvalidPrivilege() {
        new ProcessInfo("test", 123, "/bin/test", "invalid_privilege");
    }
}