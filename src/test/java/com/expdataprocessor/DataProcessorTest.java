package com.expdataprocessor;

import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DataProcessorTest {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private String testInputDir;
    private String testOutputDir;
    private String currentDateStr;

    @BeforeAll
    void setup() {
        testInputDir = System.getProperty("java.io.tmpdir") + "/test_input";
        testOutputDir = System.getProperty("java.io.tmpdir") + "/test_output";
        currentDateStr = LocalDate.now().format(DATE_FORMATTER);
    }

    @BeforeEach
    void beforeEach() throws IOException {
        Files.createDirectories(Paths.get(testInputDir));
        Files.createDirectories(Paths.get(testOutputDir));
    }

    @AfterEach
    void afterEach() throws IOException {
        deleteDirectory(Paths.get(testInputDir));
        deleteDirectory(Paths.get(testOutputDir));
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                 .map(Path::toFile)
                 .sorted((a, b) -> b.compareTo(a))
                 .forEach(File::delete);
        }
    }

    @Test
    void testDefaultDirectories() {
        DataProcessor processor = new DataProcessor(null, null);
        assertEquals("/home/user/experiment/raw_data", processor.getInputDir());
        assertEquals("/home/user/experiment/processed_data", processor.getOutputDir());
    }

    @Test
    void testGenerateOutputFileNameWithExt() {
        DataProcessor processor = new DataProcessor(testInputDir, testOutputDir);
        String result = processor.generateOutputFileName("sample.log");
        assertEquals("sample_processed_" + currentDateStr + ".log", result);
    }

    @Test
    void testGenerateOutputFileNameWithoutExt() {
        DataProcessor processor = new DataProcessor(testInputDir, testOutputDir);
        String result = processor.generateOutputFileName("readme");
        assertEquals("readme_processed_" + currentDateStr, result);
    }

    @Test
    void testLogFileProcessing() throws IOException {
        Path logFile = Paths.get(testInputDir, "test.log");
        Files.write(logFile, 
            ("INFO: Application started\n" +
             "ERROR: Null pointer exception\n" +
             "DEBUG: Processing data\n" +
             "ERROR: Database connection failed\n" +
             "WARN: Low memory").getBytes());

        DataProcessor processor = new DataProcessor(testInputDir, testOutputDir);
        processor.run();

        Path outputFile = Paths.get(testOutputDir, "test_processed_" + currentDateStr + ".log");
        assertTrue(Files.exists(outputFile));

        String content = Files.readString(outputFile);
        assertTrue(content.contains("ERROR: Null pointer exception"));
        assertTrue(content.contains("ERROR: Database connection failed"));
        assertFalse(content.contains("INFO: Application started"));
        assertFalse(content.contains("DEBUG: Processing data"));
        assertTrue(content.matches("(?s)\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\].*"));
    }

    @Test
    void testLogFileWithNoErrors() throws IOException {
        Path logFile = Paths.get(testInputDir, "clean.log");
        Files.write(logFile, 
            ("INFO: All good\n" +
             "DEBUG: Working fine").getBytes());

        DataProcessor processor = new DataProcessor(testInputDir, testOutputDir);
        processor.run();

        Path outputFile = Paths.get(testOutputDir, "clean_processed_" + currentDateStr + ".log");
        assertTrue(Files.exists(outputFile));
        assertEquals("", Files.readString(outputFile));
    }

    @Test
    void testDataFileProcessing() throws IOException {
        Path dataFile = Paths.get(testInputDir, "numbers.data");
        Files.write(dataFile, 
            ("10\n" +
             "20\n" +
             "30.5").getBytes());

        DataProcessor processor = new DataProcessor(testInputDir, testOutputDir);
        processor.run();

        Path outputFile = Paths.get(testOutputDir, "numbers_processed_" + currentDateStr + ".data");
        assertTrue(Files.exists(outputFile));

        String content = Files.readString(outputFile);
        assertTrue(content.contains("\"sum\": 60.5"));
        assertTrue(content.contains("\"average\": 20.17"));
    }

    @Test
    void testEmptyDataFile() throws IOException {
        Path dataFile = Paths.get(testInputDir, "empty.data");
        Files.write(dataFile, new byte[0]);

        DataProcessor processor = new DataProcessor(testInputDir, testOutputDir);
        processor.run();

        Path outputFile = Paths.get(testOutputDir, "empty_processed_" + currentDateStr + ".data");
        assertTrue(Files.exists(outputFile));

        String content = Files.readString(outputFile);
        assertEquals("{\"sum\": 0, \"average\": 0.00}", content);
    }

    @Test
    void testIgnoreNonTargetFiles() throws IOException {
        Path logFile = Paths.get(testInputDir, "valid.log");
        Path tmpFile = Paths.get(testInputDir, "temp.tmp");
        Path bakFile = Paths.get(testInputDir, "backup.bak");
        Path noExtFile = Paths.get(testInputDir, "noextension");
        
        Files.write(logFile, "ERROR: Test error".getBytes());
        Files.write(tmpFile, "some content".getBytes());
        Files.write(bakFile, "backup content".getBytes());
        Files.write(noExtFile, "no ext content".getBytes());

        DataProcessor processor = new DataProcessor(testInputDir, testOutputDir);
        processor.run();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(testOutputDir))) {
            int count = 0;
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                assertTrue(name.startsWith("valid_processed_") && name.endsWith(".log"), 
                    "Only valid.log should be processed, found: " + name);
                count++;
            }
            assertEquals(1, count, "Should only have 1 output file");
        }
    }

    @Test
    void testInputDirNotExist() {
        String nonExistentDir = "/this/path/does/not/exist";
        DataProcessor processor = new DataProcessor(nonExistentDir, testOutputDir);
        assertThrows(IllegalArgumentException.class, () -> processor.run());
    }

    @Test
    void testOutputDirAutoCreated() throws IOException {
        String nestedOutputDir = testOutputDir + "/nested/deep/dir";
        Path nestedPath = Paths.get(nestedOutputDir);
        deleteDirectory(nestedPath.getParent());

        Path logFile = Paths.get(testInputDir, "test.log");
        Files.write(logFile, "ERROR: Test".getBytes());

        DataProcessor processor = new DataProcessor(testInputDir, nestedOutputDir);
        processor.run();

        assertTrue(Files.exists(nestedPath));
    }

    @Test
    void testErrorCaseSensitive() throws IOException {
        Path logFile = Paths.get(testInputDir, "case.log");
        Files.write(logFile, 
            ("error: lowercase\n" +
             "ERROR: uppercase\n" +
             "Error: mixed case").getBytes());

        DataProcessor processor = new DataProcessor(testInputDir, testOutputDir);
        processor.run();

        Path outputFile = Paths.get(testOutputDir, "case_processed_" + currentDateStr + ".log");
        String content = Files.readString(outputFile);
        
        assertTrue(content.contains("ERROR: uppercase"));
        assertFalse(content.contains("error: lowercase"));
        assertFalse(content.contains("Error: mixed case"));
    }

    @Test
    void testDataFileWithIntegersOnly() throws IOException {
        Path dataFile = Paths.get(testInputDir, "integers.data");
        Files.write(dataFile, ("5\n10\n15\n20").getBytes());

        DataProcessor processor = new DataProcessor(testInputDir, testOutputDir);
        processor.run();

        Path outputFile = Paths.get(testOutputDir, "integers_processed_" + currentDateStr + ".data");
        String content = Files.readString(outputFile);
        
        assertTrue(content.contains("\"sum\": 50"));
        assertTrue(content.contains("\"average\": 12.5"));
    }

    @Test
    void testDataFileWithFloats() throws IOException {
        Path dataFile = Paths.get(testInputDir, "floats.data");
        Files.write(dataFile, ("1.1\n2.2\n3.3").getBytes());

        DataProcessor processor = new DataProcessor(testInputDir, testOutputDir);
        processor.run();

        Path outputFile = Paths.get(testOutputDir, "floats_processed_" + currentDateStr + ".data");
        String content = Files.readString(outputFile);
        
        assertTrue(content.contains("\"sum\": 6.6"));
        assertTrue(content.contains("\"average\": 2.2"));
    }

    @Test
    void testLogFileTimestampFormat() throws IOException {
        Path logFile = Paths.get(testInputDir, "timestamp.log");
        Files.write(logFile, "ERROR: Test message".getBytes());

        DataProcessor processor = new DataProcessor(testInputDir, testOutputDir);
        processor.run();

        Path outputFile = Paths.get(testOutputDir, "timestamp_processed_" + currentDateStr + ".log");
        String content = Files.readString(outputFile);
        
        assertTrue(content.matches("^\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\] ERROR: Test message\\s*$"));
    }
}
