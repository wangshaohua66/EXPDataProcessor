package com.experiment;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataProcessorTest {

    @TempDir
    Path tempInputDir;

    @TempDir
    Path tempOutputDir;

    private DataProcessor processor;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @BeforeEach
    void setUp() {
        processor = new DataProcessor();
    }

    @Test
    @Order(1)
    @DisplayName("TC-001: 测试默认参数解析")
    void testDefaultArguments() {
        processor.parseArguments(new String[]{});
        assertEquals(Paths.get("/home/user/experiment/raw_data"), processor.getInputDir());
        assertEquals(Paths.get("/home/user/experiment/processed_data"), processor.getOutputDir());
    }

    @Test
    @Order(2)
    @DisplayName("TC-002: 测试自定义输入目录参数")
    void testCustomInputDir() {
        processor.parseArguments(new String[]{"--inputDir", "/custom/input"});
        assertEquals(Paths.get("/custom/input"), processor.getInputDir());
        assertEquals(Paths.get("/home/user/experiment/processed_data"), processor.getOutputDir());
    }

    @Test
    @Order(3)
    @DisplayName("TC-003: 测试自定义输出目录参数")
    void testCustomOutputDir() {
        processor.parseArguments(new String[]{"--outputDir", "/custom/output"});
        assertEquals(Paths.get("/home/user/experiment/raw_data"), processor.getInputDir());
        assertEquals(Paths.get("/custom/output"), processor.getOutputDir());
    }

    @Test
    @Order(4)
    @DisplayName("TC-004: 测试同时自定义输入和输出目录")
    void testCustomBothDirs() {
        processor.parseArguments(new String[]{
                "--inputDir", "/custom/input",
                "--outputDir", "/custom/output"
        });
        assertEquals(Paths.get("/custom/input"), processor.getInputDir());
        assertEquals(Paths.get("/custom/output"), processor.getOutputDir());
    }

    @Test
    @Order(5)
    @DisplayName("TC-005: 测试输入目录不存在时退出")
    void testInputDirNotExist() {
        Path nonExistentDir = Paths.get("/non/existent/directory");
        processor.parseArguments(new String[]{
                "--inputDir", nonExistentDir.toString(),
                "--outputDir", tempOutputDir.toString()
        });

        int exitCode = processor.processWithExitCode();
        assertEquals(1, exitCode);
    }

    @Test
    @Order(6)
    @DisplayName("TC-006: 测试输出目录自动创建")
    void testOutputDirCreation() throws IOException {
        Path newOutputDir = tempOutputDir.resolve("new_output_dir");
        processor.parseArguments(new String[]{
                "--inputDir", tempInputDir.toString(),
                "--outputDir", newOutputDir.toString()
        });

        processor.process();

        assertTrue(Files.exists(newOutputDir));
    }

    @Test
    @Order(7)
    @DisplayName("TC-007: 测试处理.log文件 - 包含ERROR行")
    void testProcessLogFileWithErrors() throws IOException {
        Path logFile = tempInputDir.resolve("test.log");
        Files.write(logFile, List.of(
                "INFO: Application started",
                "ERROR: Database connection failed",
                "INFO: Retrying connection",
                "ERROR: Connection timeout",
                "DEBUG: Debug message"
        ));

        processor.parseArguments(new String[]{
                "--inputDir", tempInputDir.toString(),
                "--outputDir", tempOutputDir.toString()
        });
        processor.process();

        String expectedOutputName = "test_processed_" + LocalDateTime.now().format(DATE_FORMATTER) + ".log";
        Path outputFile = tempOutputDir.resolve(expectedOutputName);

        assertTrue(Files.exists(outputFile));

        List<String> lines = Files.readAllLines(outputFile);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("ERROR: Database connection failed"));
        assertTrue(lines.get(1).contains("ERROR: Connection timeout"));
        assertTrue(lines.get(0).matches("\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\].*"));
    }

    @Test
    @Order(8)
    @DisplayName("TC-008: 测试处理.log文件 - 不包含ERROR行")
    void testProcessLogFileWithoutErrors() throws IOException {
        Path logFile = tempInputDir.resolve("clean.log");
        Files.write(logFile, List.of(
                "INFO: Application started",
                "INFO: Processing complete",
                "DEBUG: Debug message"
        ));

        processor.parseArguments(new String[]{
                "--inputDir", tempInputDir.toString(),
                "--outputDir", tempOutputDir.toString()
        });
        processor.process();

        String expectedOutputName = "clean_processed_" + LocalDateTime.now().format(DATE_FORMATTER) + ".log";
        Path outputFile = tempOutputDir.resolve(expectedOutputName);

        assertTrue(Files.exists(outputFile));
        List<String> lines = Files.readAllLines(outputFile);
        assertTrue(lines.isEmpty());
    }

    @Test
    @Order(9)
    @DisplayName("TC-009: 测试处理.log文件 - 区分大小写")
    void testProcessLogFileCaseSensitive() throws IOException {
        Path logFile = tempInputDir.resolve("case.log");
        Files.write(logFile, List.of(
                "error: lowercase error",
                "ERROR: uppercase ERROR",
                "Error: mixed case Error"
        ));

        processor.parseArguments(new String[]{
                "--inputDir", tempInputDir.toString(),
                "--outputDir", tempOutputDir.toString()
        });
        processor.process();

        String expectedOutputName = "case_processed_" + LocalDateTime.now().format(DATE_FORMATTER) + ".log";
        Path outputFile = tempOutputDir.resolve(expectedOutputName);

        List<String> lines = Files.readAllLines(outputFile);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("ERROR: uppercase ERROR"));
    }

    @Test
    @Order(10)
    @DisplayName("TC-010: 测试处理.data文件 - 包含数字")
    void testProcessDataFileWithNumbers() throws IOException {
        Path dataFile = tempInputDir.resolve("numbers.data");
        Files.write(dataFile, List.of("10.5", "20.3", "30.2"));

        processor.parseArguments(new String[]{
                "--inputDir", tempInputDir.toString(),
                "--outputDir", tempOutputDir.toString()
        });
        processor.process();

        String expectedOutputName = "numbers_processed_" + LocalDateTime.now().format(DATE_FORMATTER) + ".data";
        Path outputFile = tempOutputDir.resolve(expectedOutputName);

        assertTrue(Files.exists(outputFile));

        String content = Files.readString(outputFile);
        assertTrue(content.contains("\"sum\": 61.00"));
        assertTrue(content.contains("\"average\": 20.33"));
    }

    @Test
    @Order(11)
    @DisplayName("TC-011: 测试处理.data文件 - 空文件")
    void testProcessEmptyDataFile() throws IOException {
        Path dataFile = tempInputDir.resolve("empty.data");
        Files.createFile(dataFile);

        processor.parseArguments(new String[]{
                "--inputDir", tempInputDir.toString(),
                "--outputDir", tempOutputDir.toString()
        });
        processor.process();

        String expectedOutputName = "empty_processed_" + LocalDateTime.now().format(DATE_FORMATTER) + ".data";
        Path outputFile = tempOutputDir.resolve(expectedOutputName);

        assertTrue(Files.exists(outputFile));

        String content = Files.readString(outputFile);
        assertTrue(content.contains("\"sum\": 0.00"));
        assertTrue(content.contains("\"average\": 0.00"));
    }

    @Test
    @Order(12)
    @DisplayName("TC-012: 测试处理.data文件 - 整数")
    void testProcessDataFileWithIntegers() throws IOException {
        Path dataFile = tempInputDir.resolve("integers.data");
        Files.write(dataFile, List.of("10", "20", "30"));

        processor.parseArguments(new String[]{
                "--inputDir", tempInputDir.toString(),
                "--outputDir", tempOutputDir.toString()
        });
        processor.process();

        String expectedOutputName = "integers_processed_" + LocalDateTime.now().format(DATE_FORMATTER) + ".data";
        Path outputFile = tempOutputDir.resolve(expectedOutputName);

        String content = Files.readString(outputFile);
        assertTrue(content.contains("\"sum\": 60.00"));
        assertTrue(content.contains("\"average\": 20.00"));
    }

    @Test
    @Order(13)
    @DisplayName("TC-013: 测试忽略非指定扩展名文件")
    void testIgnoreOtherFileExtensions() throws IOException {
        Path logFile = tempInputDir.resolve("valid.log");
        Path tmpFile = tempInputDir.resolve("ignore.tmp");
        Path bakFile = tempInputDir.resolve("ignore.bak");
        Path noExtFile = tempInputDir.resolve("noextension");

        Files.write(logFile, List.of("ERROR: test error"));
        Files.write(tmpFile, List.of("ERROR: should be ignored"));
        Files.write(bakFile, List.of("ERROR: should be ignored"));
        Files.write(noExtFile, List.of("ERROR: should be ignored"));

        processor.parseArguments(new String[]{
                "--inputDir", tempInputDir.toString(),
                "--outputDir", tempOutputDir.toString()
        });
        processor.process();

        long processedFileCount = Files.list(tempOutputDir).count();
        assertEquals(1, processedFileCount);
    }

    @Test
    @Order(14)
    @DisplayName("TC-014: 测试输出文件命名规范")
    void testOutputFileNameFormat() throws IOException {
        Path logFile = tempInputDir.resolve("sample.log");
        Files.write(logFile, List.of("ERROR: test"));

        processor.parseArguments(new String[]{
                "--inputDir", tempInputDir.toString(),
                "--outputDir", tempOutputDir.toString()
        });
        processor.process();

        String expectedOutputName = "sample_processed_" + LocalDateTime.now().format(DATE_FORMATTER) + ".log";
        Path outputFile = tempOutputDir.resolve(expectedOutputName);

        assertTrue(Files.exists(outputFile));
    }

    @Test
    @Order(15)
    @DisplayName("TC-015: 测试处理子目录中的文件")
    void testProcessFilesInSubdirectories() throws IOException {
        Path subDir = tempInputDir.resolve("subdir");
        Files.createDirectories(subDir);
        Path logFile = subDir.resolve("nested.log");
        Files.write(logFile, List.of("ERROR: nested error"));

        processor.parseArguments(new String[]{
                "--inputDir", tempInputDir.toString(),
                "--outputDir", tempOutputDir.toString()
        });
        processor.process();

        String expectedOutputName = "nested_processed_" + LocalDateTime.now().format(DATE_FORMATTER) + ".log";
        Path outputFile = tempOutputDir.resolve(expectedOutputName);

        assertTrue(Files.exists(outputFile));
    }

    @Test
    @Order(16)
    @DisplayName("TC-016: 测试输入目录只读保护")
    void testInputDirReadOnlyProtection() throws IOException {
        Path logFile = tempInputDir.resolve("readonly.log");
        Files.write(logFile, List.of("ERROR: test"));

        processor.parseArguments(new String[]{
                "--inputDir", tempInputDir.toString(),
                "--outputDir", tempOutputDir.toString()
        });
        processor.process();

        Path[] inputFiles = Files.list(tempInputDir).toArray(Path[]::new);
        assertEquals(1, inputFiles.length);
        assertEquals("readonly.log", inputFiles[0].getFileName().toString());

        long inputFileCount = Files.walk(tempInputDir)
                .filter(Files::isRegularFile)
                .count();
        assertEquals(1, inputFileCount);
    }

    @Test
    @Order(17)
    @DisplayName("TC-017: 测试处理多个文件")
    void testProcessMultipleFiles() throws IOException {
        Path logFile1 = tempInputDir.resolve("file1.log");
        Path logFile2 = tempInputDir.resolve("file2.log");
        Path dataFile1 = tempInputDir.resolve("data1.data");

        Files.write(logFile1, List.of("ERROR: error1"));
        Files.write(logFile2, List.of("ERROR: error2"));
        Files.write(dataFile1, List.of("100", "200"));

        processor.parseArguments(new String[]{
                "--inputDir", tempInputDir.toString(),
                "--outputDir", tempOutputDir.toString()
        });
        processor.process();

        long processedFileCount = Files.list(tempOutputDir).count();
        assertEquals(3, processedFileCount);
    }

    @Test
    @Order(18)
    @DisplayName("TC-018: 测试.data文件包含负数")
    void testProcessDataFileWithNegativeNumbers() throws IOException {
        Path dataFile = tempInputDir.resolve("negative.data");
        Files.write(dataFile, List.of("-10.5", "20.5", "-5"));

        processor.parseArguments(new String[]{
                "--inputDir", tempInputDir.toString(),
                "--outputDir", tempOutputDir.toString()
        });
        processor.process();

        String expectedOutputName = "negative_processed_" + LocalDateTime.now().format(DATE_FORMATTER) + ".data";
        Path outputFile = tempOutputDir.resolve(expectedOutputName);

        String content = Files.readString(outputFile);
        assertTrue(content.contains("\"sum\": 5.00"));
        assertTrue(content.contains("\"average\": 1.67"));
    }

    @Test
    @Order(19)
    @DisplayName("TC-019: 测试.log文件空行处理")
    void testProcessLogFileWithEmptyLines() throws IOException {
        Path logFile = tempInputDir.resolve("emptylines.log");
        Files.write(logFile, List.of(
                "INFO: start",
                "",
                "ERROR: error message",
                "   ",
                "ERROR: another error"
        ));

        processor.parseArguments(new String[]{
                "--inputDir", tempInputDir.toString(),
                "--outputDir", tempOutputDir.toString()
        });
        processor.process();

        String expectedOutputName = "emptylines_processed_" + LocalDateTime.now().format(DATE_FORMATTER) + ".log";
        Path outputFile = tempOutputDir.resolve(expectedOutputName);

        List<String> lines = Files.readAllLines(outputFile);
        assertEquals(2, lines.size());
    }

    @Test
    @Order(20)
    @DisplayName("TC-020: 测试JSON格式正确性")
    void testJsonFormatCorrectness() throws IOException {
        Path dataFile = tempInputDir.resolve("json.data");
        Files.write(dataFile, List.of("123.456", "789.012"));

        processor.parseArguments(new String[]{
                "--inputDir", tempInputDir.toString(),
                "--outputDir", tempOutputDir.toString()
        });
        processor.process();

        String expectedOutputName = "json_processed_" + LocalDateTime.now().format(DATE_FORMATTER) + ".data";
        Path outputFile = tempOutputDir.resolve(expectedOutputName);

        String content = Files.readString(outputFile);
        assertTrue(content.startsWith("{"));
        assertTrue(content.endsWith("}"));
        assertTrue(content.contains("\"sum\""));
        assertTrue(content.contains("\"average\""));
    }
}
