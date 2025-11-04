package test_soclog.ingest;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ArrayList;

import edu.hofstra.csc17.proj.soclog.ingest.LogIngestor;
import edu.hofstra.csc17.proj.soclog.ingest.LogIngestor.IngestionResult;
import edu.hofstra.csc17.proj.soclog.ingest.parser.EventParser;

public class LogIngestorTest {

    private LogIngestor ingestor;
    private EventParser parser;
    private Path testDataDir;

    @Before
    public void setUp() throws IOException {
        parser = new EventParser();
        ingestor = new LogIngestor(parser);
        testDataDir = Files.createTempDirectory("soclog-ingest-test");
    }
    
    @After
    public void tearDown() throws IOException {
        // Clean up test directory
        if (testDataDir != null && Files.exists(testDataDir)) {
            Files.walk(testDataDir)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        // Ignore
                    }
                });
        }
    }

    // Valid Data Tests
    
    @Test
    public void testIngest_SingleValidFile() throws IOException {
        Path testFile = testDataDir.resolve("valid.csv");
        String csvContent = "read,2024-01-01T00:00:00Z,mode=r,name=test;pid=123;path=/bin/test;privilege=user,path=/tmp/file.txt;fd=5;permissions=644";
        Files.write(testFile, csvContent.getBytes());

        IngestionResult result = ingestor.ingest(Arrays.asList(testFile));
        assertNotNull(result);
        assertEquals(1, result.getEvents().size());
        assertEquals(0, result.getErrors().size());
    }
    
    @Test
    public void testIngest_MultipleValidFiles() throws IOException {
        Path file1 = testDataDir.resolve("file1.csv");
        Path file2 = testDataDir.resolve("file2.csv");
        
        String content1 = "read,2024-01-01T00:00:00Z,mode=r,name=test;pid=123;path=/bin/test;privilege=user,path=/tmp/file.txt;fd=5;permissions=644";
        String content2 = "write,2024-01-01T00:00:01Z,mode=w,name=test;pid=123;path=/bin/test;privilege=user,path=/tmp/file.txt;fd=5;permissions=644";
        
        Files.write(file1, content1.getBytes());
        Files.write(file2, content2.getBytes());

        IngestionResult result = ingestor.ingest(Arrays.asList(file1, file2));
        assertNotNull(result);
        assertEquals(2, result.getEvents().size());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void testIngest_RealDataFile() throws IOException {
        Path dataFile = Paths.get("data/sample_logs.csv");
        if (!Files.exists(dataFile)) {
            return; // Skip if file doesn't exist
        }

        IngestionResult result = ingestor.ingest(Arrays.asList(dataFile));
        assertNotNull(result);
        assertTrue(result.getEvents().size() > 0);
        result.printIngestionSummary();
    }
    
    @Test
    public void testIngest_AllEventTypes() throws IOException {
        Path testFile = testDataDir.resolve("all_types.csv");
        StringBuilder content = new StringBuilder();
        content.append("read,2024-01-01T00:00:00Z,mode=r,name=test;pid=123;path=/bin/test;privilege=user,path=/tmp/file.txt;fd=5;permissions=644\n");
        content.append("write,2024-01-01T00:00:01Z,mode=w,name=test;pid=123;path=/bin/test;privilege=user,path=/tmp/file.txt;fd=5;permissions=644\n");
        content.append("execute,2024-01-01T00:00:02Z,exit=0,name=test;pid=123;path=/bin/test;privilege=user,path=/usr/bin/script.sh;fd=10;permissions=755\n");
        content.append("open,2024-01-01T00:00:03Z,mode=r,name=test;pid=123;path=/bin/test;privilege=user,path=/tmp/file.txt;fd=5;permissions=644\n");
        content.append("close,2024-01-01T00:00:04Z,mode=r,name=test;pid=123;path=/bin/test;privilege=user,path=/tmp/file.txt;fd=5;permissions=644\n");
        content.append("fork,2024-01-01T00:00:05Z,status=success,name=shell;pid=1001;path=/bin/bash;privilege=user,name=worker;pid=1002;path=/bin/bash;privilege=user\n");
        content.append("sendto,2024-01-01T00:00:06Z,protocol=TCP,name=httpd;pid=8080;path=/usr/bin/httpd;privilege=root,ip=192.168.1.100;port=80;protocol=TCP\n");
        content.append("receivefrom,2024-01-01T00:00:07Z,protocol=TCP,name=httpd;pid=8080;path=/usr/bin/httpd;privilege=root,ip=192.168.1.100;port=80;protocol=TCP\n");
        
        Files.write(testFile, content.toString().getBytes());

        IngestionResult result = ingestor.ingest(Arrays.asList(testFile));
        assertNotNull(result);
        assertEquals(8, result.getEvents().size());
        assertEquals(0, result.getErrors().size());
    }

    // Error Handling Tests
    
    @Test
    public void testIngest_MissingFile() throws IOException {
        Path missingFile = testDataDir.resolve("missing.csv");
        
        IngestionResult result = ingestor.ingest(Arrays.asList(missingFile));
        assertNotNull(result);
        assertEquals(0, result.getEvents().size());
        assertTrue(result.getErrors().size() > 0);
        assertTrue(result.getErrors().get(0).contains("Missing file"));
    }
    
    @Test
    public void testIngest_EmptyFile() throws IOException {
        Path emptyFile = testDataDir.resolve("empty.csv");
        Files.write(emptyFile, "".getBytes());
        
        IngestionResult result = ingestor.ingest(Arrays.asList(emptyFile));
        assertNotNull(result);
        assertEquals(0, result.getEvents().size());
        assertEquals(0, result.getErrors().size()); // Empty files are skipped with no errs
    }
    
    @Test
    public void testIngest_InvalidEventType() throws IOException {
        Path testFile = testDataDir.resolve("invalid_type.csv");
        String content = "invalid_type,2024-01-01T00:00:00Z,mode=r,name=test;pid=123;path=/bin/test;privilege=user,path=/tmp/file.txt;fd=5;permissions=644";
        Files.write(testFile, content.getBytes());

        IngestionResult result = ingestor.ingest(Arrays.asList(testFile));
        assertNotNull(result);
        assertEquals(0, result.getEvents().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Invalid event type"));
    }
    
    @Test
    public void testIngest_MalformedTimestamp() throws IOException {
        Path testFile = testDataDir.resolve("invalid_timestamp.csv");
        String content = "read,invalid-timestamp,mode=r,name=test;pid=123;path=/bin/test;privilege=user,path=/tmp/file.txt;fd=5;permissions=644";
        Files.write(testFile, content.getBytes());

        IngestionResult result = ingestor.ingest(Arrays.asList(testFile));
        assertNotNull(result);
        assertEquals(0, result.getEvents().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Malformed timestamp"));
    }
    
    @Test
    public void testIngest_OutOfOrderTimestamps() throws IOException {
        Path testFile = testDataDir.resolve("out_of_order.csv");
        StringBuilder content = new StringBuilder();
        content.append("read,2024-01-01T00:00:05Z,mode=r,name=test;pid=123;path=/bin/test;privilege=user,path=/tmp/file.txt;fd=5;permissions=644\n");
        content.append("write,2024-01-01T00:00:02Z,mode=w,name=test;pid=123;path=/bin/test;privilege=user,path=/tmp/file.txt;fd=5;permissions=644\n");
        Files.write(testFile, content.toString().getBytes());

        IngestionResult result = ingestor.ingest(Arrays.asList(testFile));
        assertNotNull(result);
        assertEquals(1, result.getEvents().size()); // First succeeds
        assertEquals(1, result.getErrors().size()); // Second fails
        assertTrue(result.getErrors().get(0).contains("before previous timestamp"));
    }
    
    @Test
    public void testIngest_InvalidPrivilege() throws IOException {
        Path testFile = testDataDir.resolve("invalid_privilege.csv");
        String content = "read,2024-01-01T00:00:00Z,mode=r,name=test;pid=123;path=/bin/test;privilege=invalid,path=/tmp/file.txt;fd=5;permissions=644";
        Files.write(testFile, content.getBytes());

        IngestionResult result = ingestor.ingest(Arrays.asList(testFile));
        assertNotNull(result);
        assertEquals(0, result.getEvents().size());
        assertEquals(1, result.getErrors().size());
    }
    
    @Test
    public void testIngest_InvalidPermissions() throws IOException {
        Path testFile = testDataDir.resolve("invalid_permissions.csv");
        String content = "read,2024-01-01T00:00:00Z,mode=r,name=test;pid=123;path=/bin/test;privilege=user,path=/tmp/file.txt;fd=5;permissions=999";
        Files.write(testFile, content.getBytes());

        IngestionResult result = ingestor.ingest(Arrays.asList(testFile));
        assertNotNull(result);
        assertEquals(0, result.getEvents().size());
        assertEquals(1, result.getErrors().size());
    }
    
    @Test
    public void testIngest_InvalidPort() throws IOException {
        Path testFile = testDataDir.resolve("invalid_port.csv");
        String content = "sendto,2024-01-01T00:00:00Z,protocol=TCP,name=test;pid=123;path=/bin/test;privilege=user,ip=192.168.1.1;port=99999;protocol=TCP";
        Files.write(testFile, content.getBytes());

        IngestionResult result = ingestor.ingest(Arrays.asList(testFile));
        assertNotNull(result);
        assertEquals(0, result.getEvents().size());
        assertEquals(1, result.getErrors().size());
    }
    
    @Test
    public void testIngest_InvalidIPAddress() throws IOException {
        Path testFile = testDataDir.resolve("invalid_ip.csv");
        String content = "sendto,2024-01-01T00:00:00Z,protocol=TCP,name=test;pid=123;path=/bin/test;privilege=user,ip=999.999.999.999;port=80;protocol=TCP";
        Files.write(testFile, content.getBytes());

        IngestionResult result = ingestor.ingest(Arrays.asList(testFile));
        assertNotNull(result);
        assertEquals(0, result.getEvents().size());
        assertEquals(1, result.getErrors().size());
    }
    
    @Test
    public void testIngest_MixedValidAndInvalid() throws IOException {
        Path testFile = testDataDir.resolve("mixed.csv");
        StringBuilder content = new StringBuilder();
        content.append("read,2024-01-01T00:00:00Z,mode=r,name=test;pid=123;path=/bin/test;privilege=user,path=/tmp/file.txt;fd=5;permissions=644\n");
        content.append("invalid_type,2024-01-01T00:00:01Z,mode=r,name=test;pid=123;path=/bin/test;privilege=user,path=/tmp/file.txt;fd=5;permissions=644\n");
        content.append("write,2024-01-01T00:00:02Z,mode=w,name=test;pid=123;path=/bin/test;privilege=user,path=/tmp/file.txt;fd=5;permissions=644\n");
        Files.write(testFile, content.toString().getBytes());

        IngestionResult result = ingestor.ingest(Arrays.asList(testFile));
        assertNotNull(result);
        assertEquals(2, result.getEvents().size());
        assertEquals(1, result.getErrors().size());
    }

    // IngestionResult Tests
    
    @Test
    public void testIngestionResult_StartAndEndTime() throws IOException {
        Path testFile = testDataDir.resolve("time_test.csv");
        StringBuilder content = new StringBuilder();
        content.append("read,2024-01-01T00:00:00Z,mode=r,name=test;pid=123;path=/bin/test;privilege=user,path=/tmp/file.txt;fd=5;permissions=644\n");
        content.append("write,2024-01-01T00:00:30Z,mode=w,name=test;pid=123;path=/bin/test;privilege=user,path=/tmp/file.txt;fd=5;permissions=644\n");
        Files.write(testFile, content.toString().getBytes());

        IngestionResult result = ingestor.ingest(Arrays.asList(testFile));
        assertNotNull(result.startTime());
        assertNotNull(result.endTime());
        assertTrue(result.startTime().isBefore(result.endTime()) || result.startTime().equals(result.endTime()));
    }
    
    @Test
    public void testIngestionResult_EmptyDataset() throws IOException {
        Path emptyFile = testDataDir.resolve("empty.csv");
        Files.write(emptyFile, "".getBytes());

        IngestionResult result = ingestor.ingest(Arrays.asList(emptyFile));
        assertNull(result.startTime());
        assertNull(result.endTime());
    }
    
    @Test
    public void testIngestionResult_DatasetSummary() throws IOException {
        Path testFile = testDataDir.resolve("summary_test.csv");
        StringBuilder content = new StringBuilder();
        content.append("read,2024-01-01T00:00:00Z,mode=r,name=test;pid=123;path=/bin/test;privilege=user,path=/tmp/file.txt;fd=5;permissions=644\n");
        content.append("write,2024-01-01T00:00:01Z,mode=w,name=test;pid=123;path=/bin/test;privilege=user,path=/tmp/file.txt;fd=5;permissions=644\n");
        Files.write(testFile, content.toString().getBytes());

        IngestionResult result = ingestor.ingest(Arrays.asList(testFile));
        // Should not throw exception
        result.datasetSummary();
    }
}