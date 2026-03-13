package com.example;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DataProcessorTest {

    @TempDir
    Path tempDir;

    Path inputDir;
    Path outputDir;

    @BeforeEach
    void setup() throws IOException {
        inputDir = tempDir.resolve("input");
        outputDir = tempDir.resolve("output");
        Files.createDirectories(inputDir);
        Files.createDirectories(outputDir);
    }

    @Test
    @DisplayName("测试输入目录不存在时应报错并退出")
    void testNonExistentInputDirectory() {
        DataProcessor processor = new DataProcessor("/nonexistent/path", outputDir.toString());
        assertFalse(processor.validateInputDirectory());
    }

    @Test
    @DisplayName("测试输出目录不存在时应自动创建")
    void testOutputDirectoryCreation() throws IOException {
        Path nonExistentOutput = tempDir.resolve("new_output");
        DataProcessor processor = new DataProcessor(inputDir.toString(), nonExistentOutput.toString());
        processor.createOutputDirectory();
        assertTrue(Files.exists(nonExistentOutput));
    }

    @Test
    @DisplayName("测试 .log 文件处理：筛选 ERROR 行并添加时间戳")
    void testLogFileProcessing() throws IOException {
        Path logFile = inputDir.resolve("test.log");
        String content = "INFO: Application started\nERROR: Database failed\nDEBUG: Processing\nERROR: Timeout\nINFO: Done";
        Files.write(logFile, content.getBytes());

        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.processFiles();

        Path outputFile = outputDir.resolve("test_processed_" + getDateSuffix() + ".log");
        assertTrue(Files.exists(outputFile), "输出文件应存在");

        List<String> lines = Files.readAllLines(outputFile);
        assertEquals(2, lines.size(), "应只有2行包含ERROR");
        assertTrue(lines.get(0).contains("ERROR: Database failed"), "应包含第一行错误");
        assertTrue(lines.get(1).contains("ERROR: Timeout"), "应包含第二行错误");
        assertTrue(lines.get(0).startsWith("["), "应包含时间戳前缀");
    }

    @Test
    @DisplayName("测试 .log 文件无 ERROR 行时应创建空文件")
    void testLogFileWithoutErrors() throws IOException {
        Path logFile = inputDir.resolve("clean.log");
        String content = "INFO: All good\nDEBUG: Nothing wrong";
        Files.write(logFile, content.getBytes());

        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.processFiles();

        Path outputFile = outputDir.resolve("clean_processed_" + getDateSuffix() + ".log");
        assertTrue(Files.exists(outputFile), "输出文件应存在");
        List<String> lines = Files.readAllLines(outputFile);
        assertEquals(0, lines.size(), "没有ERROR行时文件应为空");
    }

    @Test
    @DisplayName("测试 .data 文件处理：计算总和与平均值")
    void testDataFileProcessing() throws IOException {
        Path dataFile = inputDir.resolve("numbers.data");
        String content = "10\n20\n30.5";
        Files.write(dataFile, content.getBytes());

        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.processFiles();

        Path outputFile = outputDir.resolve("numbers_processed_" + getDateSuffix() + ".data");
        assertTrue(Files.exists(outputFile), "输出文件应存在");

        String result = new String(Files.readAllBytes(outputFile));
        assertTrue(result.contains("\"sum\": 60.5"), "总和应为60.5");
        assertTrue(result.contains("\"average\": 20.17"), "平均值应为20.17");
    }

    @Test
    @DisplayName("测试空 .data 文件处理")
    void testEmptyDataFile() throws IOException {
        Path dataFile = inputDir.resolve("empty.data");
        Files.write(dataFile, new byte[0]);

        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.processFiles();

        Path outputFile = outputDir.resolve("empty_processed_" + getDateSuffix() + ".data");
        String result = new String(Files.readAllBytes(outputFile));
        assertTrue(result.contains("\"sum\": 0.0") || result.contains("\"sum\": 0"), "空文件总和应为0");
        assertTrue(result.contains("\"average\": 0.00"), "空文件平均值应为0.00");
    }

    @Test
    @DisplayName("测试忽略非 .log 和 .data 文件")
    void testIgnoreOtherFileTypes() throws IOException {
        Path tmpFile = inputDir.resolve("ignore.tmp");
        Path bakFile = inputDir.resolve("backup.bak");
        Path noExtFile = inputDir.resolve("noextension");
        Files.write(tmpFile, "temp content".getBytes());
        Files.write(bakFile, "backup content".getBytes());
        Files.write(noExtFile, "no extension".getBytes());

        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.processFiles();

        assertFalse(Files.exists(outputDir.resolve("ignore_processed_" + getDateSuffix() + ".tmp")));
        assertFalse(Files.exists(outputDir.resolve("backup_processed_" + getDateSuffix() + ".bak")));
        assertFalse(Files.exists(outputDir.resolve("noextension_processed_" + getDateSuffix())));
    }

    @Test
    @DisplayName("测试输入文件未被修改")
    void testInputFilesNotModified() throws IOException {
        Path testFile = inputDir.resolve("readonly.log");
        String originalContent = "ERROR: Test error";
        Files.write(testFile, originalContent.getBytes());
        long originalTime = Files.getLastModifiedTime(testFile).toMillis();

        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.processFiles();

        String contentAfter = new String(Files.readAllBytes(testFile));
        assertEquals(originalContent, contentAfter, "原始文件内容不应被修改");
    }

    private String getDateSuffix() {
        return new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
    }

    @AfterEach
    void cleanup() throws IOException {
        if (Files.exists(outputDir)) {
            Files.walk(outputDir)
                .filter(Files::isRegularFile)
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        }
        Files.walk(inputDir)
            .filter(Files::isRegularFile)
            .forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
    }
}
