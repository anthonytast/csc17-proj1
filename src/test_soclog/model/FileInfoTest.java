package test_soclog.model;

import org.junit.Test;

import edu.hofstra.csc17.proj.soclog.model.entity.FileInfo;

import static org.junit.Assert.*;

public class FileInfoTest {

    @Test
    public void testConstructor_ValidFields() {
        FileInfo file = new FileInfo("/tmp/test.txt", 5, "644");
        assertEquals("/tmp/test.txt", file.getPath());
        assertEquals(Integer.valueOf(5), file.getFileDescriptor());
        assertEquals("644", file.getPermissions());
    }
    
    @Test
    public void testConstructor_ValidPermissions_AllOctalDigits() {
        FileInfo file1 = new FileInfo("/tmp/test", 1, "000");
        assertEquals("000", file1.getPermissions());
        
        FileInfo file2 = new FileInfo("/tmp/test", 2, "777");
        assertEquals("777", file2.getPermissions());
        
        FileInfo file3 = new FileInfo("/tmp/test", 3, "755");
        assertEquals("755", file3.getPermissions());
    }
    
    @Test
    public void testConstructor_ValidFileDescriptor_Zero() {
        FileInfo file = new FileInfo("/tmp/test.txt", 0, "644");
        assertEquals(Integer.valueOf(0), file.getFileDescriptor());
    }

    @Test
    public void testGetDisplayName_WithPath() {
        FileInfo file = new FileInfo("/tmp/test.txt", 5, "644");
        assertEquals("/tmp/test.txt", file.getDisplayName());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_InvalidPermissions_NotOctal() {
        new FileInfo("/tmp/test.txt", 5, "888");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_InvalidPermissions_TooShort() {
        new FileInfo("/tmp/test.txt", 5, "64");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_InvalidPermissions_TooLong() {
        new FileInfo("/tmp/test.txt", 5, "6440");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_InvalidPermissions_NonNumeric() {
        new FileInfo("/tmp/test.txt", 5, "abc");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NullPermissions() {
        new FileInfo("/tmp/test.txt", 5, null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NullPath() {
        new FileInfo(null, 5, "644");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_EmptyPath() {
        new FileInfo("", 5, "644");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NullFileDescriptor() {
        new FileInfo("/tmp/test.txt", null, "644");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NegativeFileDescriptor() {
        new FileInfo("/tmp/test.txt", -1, "644");
    }
    
    @Test
    public void testGetCanonicalId() {
        FileInfo file = new FileInfo("/tmp/test.txt", 5, "644");
        String canonicalId = file.getCanonicalId();
        assertNotNull(canonicalId);
        assertTrue(canonicalId.contains("test.txt"));
    }
    
    @Test
    public void testEquals_SameValues() {
        FileInfo f1 = new FileInfo("/tmp/test.txt", 5, "644");
        FileInfo f2 = new FileInfo("/tmp/test.txt", 5, "644");
        assertEquals(f1, f2);
        assertEquals(f1.hashCode(), f2.hashCode());
    }
    
    @Test
    public void testEquals_DifferentValues() {
        FileInfo f1 = new FileInfo("/tmp/test.txt", 5, "644");
        FileInfo f2 = new FileInfo("/tmp/other.txt", 5, "644");
        assertNotEquals(f1, f2);
    }
}