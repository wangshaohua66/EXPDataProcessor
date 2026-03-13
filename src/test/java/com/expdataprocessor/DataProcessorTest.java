package com.expdataprocessor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataProcessorTest {
    @TempDir
    Path tempDir;
    
    private Path inputDir;
    private Path outputDir;
    private String dateSuffix;

    @BeforeEach
    void setUp() throws IOException {
        inputDir = tempDir.resolve("input");
        outputDir = tempDir.resolve("output");
        Files.createDirectories(inputDir);
        dateSuffix = new SimpleDateFormat("yyyyMMdd").format(new Date());
    }

    @Test
    void testValidateInputDirectory_Exists() {
        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        assertTrue(processor.validateInputDirectory());
    }

    @Test
    void testValidateInputDirectory_NotExists() {
        Path nonExistent = tempDir.resolve("nonexistent");
        DataProcessor processor = new DataProcessor(nonExistent.toString(), outputDir.toString());
        assertFalse(processor.validateInputDirectory());
    }

    @Test
    void testCreateOutputDirectory() throws IOException {
        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.createOutputDirectory();
        assertTrue(Files.exists(outputDir));
        assertTrue(Files.isDirectory(outputDir));
    }

    @Test
    void testCreateOutputDirectory_Nested() throws IOException {
        Path nestedOutput = tempDir.resolve("nested").resolve("deep").resolve("output");
        DataProcessor processor = new DataProcessor(inputDir.toString(), nestedOutput.toString());
        processor.createOutputDirectory();
        assertTrue(Files.exists(nestedOutput));
    }

    @Test
    void testProcessLogFile_WithErrors() throws IOException {
        Path logFile = inputDir.resolve("test.log");
        Files.write(logFile, List.of(
                "INFO Application started",
                "ERROR Connection failed",
                "DEBUG Processing",
                "ERROR Authentication failed",
                "INFO Done"
        ));

        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.createOutputDirectory();
        processor.process();

        Path outputFile = outputDir.resolve("test_processed_" + dateSuffix + ".log");
        assertTrue(Files.exists(outputFile));
        
        List<String> lines = Files.readAllLines(outputFile, StandardCharsets.UTF_8);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("ERROR Connection failed"));
        assertTrue(lines.get(1).contains("ERROR Authentication failed"));
        assertTrue(lines.get(0).startsWith("[20"));
        assertTrue(lines.get(0).contains("] "));
    }

    @Test
    void testProcessLogFile_WithoutErrors() throws IOException {
        Path logFile = inputDir.resolve("noerror.log");
        Files.write(logFile, List.of("INFO Only info", "DEBUG Only debug"));

        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.createOutputDirectory();
        processor.process();

        Path outputFile = outputDir.resolve("noerror_processed_" + dateSuffix + ".log");
        assertTrue(Files.exists(outputFile));
        assertEquals(0, Files.size(outputFile));
    }

    @Test
    void testProcessLogFile_EmptyFile() throws IOException {
        Path logFile = inputDir.resolve("empty.log");
        Files.createFile(logFile);

        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.createOutputDirectory();
        processor.process();

        Path outputFile = outputDir.resolve("empty_processed_" + dateSuffix + ".log");
        assertTrue(Files.exists(outputFile));
        assertEquals(0, Files.size(outputFile));
    }

    @Test
    void testProcessLogFile_CaseSensitive() throws IOException {
        Path logFile = inputDir.resolve("case.log");
        Files.write(logFile, List.of("error lowercase", "ERROR uppercase"));

        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.createOutputDirectory();
        processor.process();

        Path outputFile = outputDir.resolve("case_processed_" + dateSuffix + ".log");
        List<String> lines = Files.readAllLines(outputFile, StandardCharsets.UTF_8);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("ERROR uppercase"));
    }

    @Test
    void testProcessDataFile_Integers() throws IOException {
        Path dataFile = inputDir.resolve("ints.data");
        Files.write(dataFile, List.of("10", "20", "30"));

        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.createOutputDirectory();
        processor.process();

        Path outputFile = outputDir.resolve("ints_processed_" + dateSuffix + ".data");
        String content = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertEquals("{\"sum\": 60.00, \"average\": 20.00}", content);
    }

    @Test
    void testProcessDataFile_Floats() throws IOException {
        Path dataFile = inputDir.resolve("floats.data");
        Files.write(dataFile, List.of("10.5", "20.5", "30.0"));

        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.createOutputDirectory();
        processor.process();

        Path outputFile = outputDir.resolve("floats_processed_" + dateSuffix + ".data");
        String content = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertEquals("{\"sum\": 61.00, \"average\": 20.33}", content);
    }

    @Test
    void testProcessDataFile_EmptyFile() throws IOException {
        Path dataFile = inputDir.resolve("empty.data");
        Files.createFile(dataFile);

        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.createOutputDirectory();
        processor.process();

        Path outputFile = outputDir.resolve("empty_processed_" + dateSuffix + ".data");
        String content = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertEquals("{\"sum\": 0.00, \"average\": 0.00}", content);
    }

    @Test
    void testProcessDataFile_MixedNumbers() throws IOException {
        Path dataFile = inputDir.resolve("mixed.data");
        Files.write(dataFile, List.of("10", "20.5", "30"));

        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.createOutputDirectory();
        processor.process();

        Path outputFile = outputDir.resolve("mixed_processed_" + dateSuffix + ".data");
        String content = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertEquals("{\"sum\": 60.50, \"average\": 20.17}", content);
    }

    @Test
    void testIgnoreNonProcessableFiles() throws IOException {
        Files.write(inputDir.resolve("ignore.tmp"), List.of("test"));
        Files.write(inputDir.resolve("noextension"), List.of("test"));
        Files.write(inputDir.resolve("file.bak"), List.of("test"));
        Files.write(inputDir.resolve("valid.log"), List.of("ERROR test"));

        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.createOutputDirectory();
        processor.process();

        assertFalse(Files.exists(outputDir.resolve("ignore_processed_" + dateSuffix + ".tmp")));
        assertFalse(Files.exists(outputDir.resolve("noextension_processed_" + dateSuffix)));
        assertFalse(Files.exists(outputDir.resolve("file_processed_" + dateSuffix + ".bak")));
        assertTrue(Files.exists(outputDir.resolve("valid_processed_" + dateSuffix + ".log")));
    }

    @Test
    void testOutputFileNameFormat_WithExtension() {
        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        String expected = "test_processed_" + dateSuffix + ".log";
        try {
            java.lang.reflect.Method method = DataProcessor.class.getDeclaredMethod("generateOutputFileName", String.class);
            method.setAccessible(true);
            String result = (String) method.invoke(processor, "test.log");
            assertEquals(expected, result);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    void testOutputFileNameFormat_WithoutExtension() {
        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        String expected = "noext_processed_" + dateSuffix;
        try {
            java.lang.reflect.Method method = DataProcessor.class.getDeclaredMethod("generateOutputFileName", String.class);
            method.setAccessible(true);
            String result = (String) method.invoke(processor, "noext");
            assertEquals(expected, result);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    void testInputDirectoryNotModified() throws IOException {
        Path logFile = inputDir.resolve("test.log");
        long originalSize = Files.size(Files.write(logFile, List.of("ERROR test")));
        long originalModified = Files.getLastModifiedTime(logFile).toMillis();

        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.createOutputDirectory();
        processor.process();

        assertEquals(originalSize, Files.size(logFile));
        assertEquals(originalModified, Files.getLastModifiedTime(logFile).toMillis());
    }

    @AfterEach
    void tearDown() {
    }
}
