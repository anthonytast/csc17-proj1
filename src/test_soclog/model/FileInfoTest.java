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
    public void testGetDisplayName_WithPath() {
        FileInfo file = new FileInfo("/tmp/test.txt", 5, "644");
        assertEquals("/tmp/test.txt", file.getDisplayName());
    }
}