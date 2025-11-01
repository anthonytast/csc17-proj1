package test_soclog.ingest;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

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

    @Test
    public void testIngest_SingleValidFile() throws IOException {
        // TODO: Test ingestion of single valid CSV file
        Path testFile = testDataDir.resolve("valid.csv");
        String csvContent = "read,2024-01-01T00:00:00Z,mode=r,name=test;pid=123;path=/bin/test;privilege=user,path=/tmp/file.txt;fd=5;permissions=644";
        Files.write(testFile, csvContent.getBytes());

        IngestionResult result = ingestor.ingest(Arrays.asList(testFile));
        assertNotNull(result);
    }

    @Test
    public void testIngest_RealDataFile() throws IOException {
        Path dataFile = Paths.get("data/sample_logs.csv");

        IngestionResult result = ingestor.ingest(Arrays.asList(dataFile));
        assertNotNull(result);

        // Verify some events were ingested
        assertTrue(result.getEvents().size() > 0);

        // Print summary to see ingestion statistics
        result.printIngestionSummary();
    }
}