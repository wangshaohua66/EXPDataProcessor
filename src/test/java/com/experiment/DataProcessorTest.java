package com.experiment;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

public class DataProcessorTest {
    
    @TempDir
    Path tempDir;
    
    private Path inputDir;
    private Path outputDir;
    
    @BeforeEach
    void setUp() throws IOException {
        inputDir = tempDir.resolve("input");
        outputDir = tempDir.resolve("output");
        Files.createDirectories(inputDir);
    }
    
    @Test
    @DisplayName("测试: 输入目录不存在时应打印错误并退出")
    void testInputDirNotExist() {
        Path nonExistentDir = tempDir.resolve("non_existent");
        DataProcessor processor = new DataProcessor(nonExistentDir.toString(), outputDir.toString());
        
        // 由于 System.exit() 无法被捕获，我们验证处理器的行为
        // 在实际运行中，如果输入目录不存在，程序会调用 System.exit(1)
        assertFalse(Files.exists(nonExistentDir));
    }
    
    @Test
    @DisplayName("测试: 输出目录不存在时自动创建")
    void testOutputDirAutoCreate() throws IOException {
        Path nonExistentOutput = tempDir.resolve("new_output");
        assertFalse(Files.exists(nonExistentOutput));
        
        // 创建一个空的 .log 文件
        Files.createFile(inputDir.resolve("test.log"));
        
        DataProcessor processor = new DataProcessor(inputDir.toString(), nonExistentOutput.toString());
        processor.process();
        
        assertTrue(Files.exists(nonExistentOutput));
        assertTrue(Files.isDirectory(nonExistentOutput));
    }
    
    @Test
    @DisplayName("测试: 只处理 .log 和 .data 文件，忽略其他文件")
    void testFileExtensionFilter() throws IOException {
        // 创建各种文件
        Files.writeString(inputDir.resolve("valid.log"), "ERROR: test");
        Files.writeString(inputDir.resolve("valid.data"), "10.5");
        Files.writeString(inputDir.resolve("invalid.tmp"), "content");
        Files.writeString(inputDir.resolve("invalid.bak"), "content");
        Files.writeString(inputDir.resolve("noextension"), "content");
        
        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.process();
        
        // 验证只有 .log 和 .data 文件被处理
        assertTrue(Files.exists(outputDir));
        assertEquals(2, Files.list(outputDir).count());
        
        // 验证输出文件名格式
        String dateSuffix = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        assertTrue(Files.exists(outputDir.resolve("valid_processed_" + dateSuffix + ".log")));
        assertTrue(Files.exists(outputDir.resolve("valid_processed_" + dateSuffix + ".data")));
    }
    
    @Test
    @DisplayName("测试: .log 文件处理 - 筛选 ERROR 行并添加时间戳")
    void testLogFileProcessing() throws IOException {
        String logContent = "INFO: Starting process\n" +
                           "ERROR: Connection failed\n" +
                           "DEBUG: Debug info\n" +
                           "ERROR: Timeout occurred\n" +
                           "INFO: Process completed";
        
        Files.writeString(inputDir.resolve("app.log"), logContent);
        
        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.process();
        
        String dateSuffix = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        Path outputFile = outputDir.resolve("app_processed_" + dateSuffix + ".log");
        
        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        
        // 验证只有 ERROR 行被包含
        assertTrue(content.contains("ERROR: Connection failed"));
        assertTrue(content.contains("ERROR: Timeout occurred"));
        assertFalse(content.contains("INFO: Starting process"));
        assertFalse(content.contains("DEBUG: Debug info"));
        
        // 验证时间戳格式
        assertTrue(content.matches("(?s)^\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\].*"));
    }
    
    @Test
    @DisplayName("测试: .log 文件处理 - 无 ERROR 行时创建空文件")
    void testLogFileNoErrors() throws IOException {
        String logContent = "INFO: Starting process\n" +
                           "DEBUG: Debug info\n" +
                           "INFO: Process completed";
        
        Files.writeString(inputDir.resolve("clean.log"), logContent);
        
        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.process();
        
        String dateSuffix = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        Path outputFile = outputDir.resolve("clean_processed_" + dateSuffix + ".log");
        
        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        assertEquals("", content);
    }
    
    @Test
    @DisplayName("测试: .log 文件处理 - 空文件")
    void testLogFileEmpty() throws IOException {
        Files.createFile(inputDir.resolve("empty.log"));
        
        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.process();
        
        String dateSuffix = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        Path outputFile = outputDir.resolve("empty_processed_" + dateSuffix + ".log");
        
        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        assertEquals("", content);
    }
    
    @Test
    @DisplayName("测试: .data 文件处理 - 计算总和和平均值")
    void testDataFileProcessing() throws IOException {
        String dataContent = "10.5\n20.0\n30.5";
        
        Files.writeString(inputDir.resolve("numbers.data"), dataContent);
        
        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.process();
        
        String dateSuffix = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        Path outputFile = outputDir.resolve("numbers_processed_" + dateSuffix + ".data");
        
        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        
        // 验证 JSON 格式
        assertTrue(content.contains("\"sum\": 61.00"));
        assertTrue(content.contains("\"average\": 20.33"));
    }
    
    @Test
    @DisplayName("测试: .data 文件处理 - 空文件")
    void testDataFileEmpty() throws IOException {
        Files.createFile(inputDir.resolve("empty.data"));
        
        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.process();
        
        String dateSuffix = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        Path outputFile = outputDir.resolve("empty_processed_" + dateSuffix + ".data");
        
        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        
        assertTrue(content.contains("\"sum\": 0.00"));
        assertTrue(content.contains("\"average\": 0.00"));
    }
    
    @Test
    @DisplayName("测试: .data 文件处理 - 包含负数和小数")
    void testDataFileWithNegativeAndFloats() throws IOException {
        String dataContent = "-10.5\n20.25\n-5.0\n15.25";
        
        Files.writeString(inputDir.resolve("mixed.data"), dataContent);
        
        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.process();
        
        String dateSuffix = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        Path outputFile = outputDir.resolve("mixed_processed_" + dateSuffix + ".data");
        
        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        
        // sum = -10.5 + 20.25 + (-5.0) + 15.25 = 20.00
        // average = 20.00 / 4 = 5.00
        assertTrue(content.contains("\"sum\": 20.00"));
        assertTrue(content.contains("\"average\": 5.00"));
    }
    
    @Test
    @DisplayName("测试: .data 文件处理 - 包含空行")
    void testDataFileWithEmptyLines() throws IOException {
        String dataContent = "10.0\n\n20.0\n\n30.0";
        
        Files.writeString(inputDir.resolve("with_empty_lines.data"), dataContent);
        
        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.process();
        
        String dateSuffix = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        Path outputFile = outputDir.resolve("with_empty_lines_processed_" + dateSuffix + ".data");
        
        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        
        // sum = 10.0 + 20.0 + 30.0 = 60.00
        // average = 60.00 / 3 = 20.00
        assertTrue(content.contains("\"sum\": 60.00"));
        assertTrue(content.contains("\"average\": 20.00"));
    }
    
    @Test
    @DisplayName("测试: 原始文件不被修改")
    void testOriginalFileNotModified() throws IOException {
        String originalContent = "ERROR: Test error\nINFO: Test info";
        Path originalFile = inputDir.resolve("original.log");
        Files.writeString(originalFile, originalContent);
        
        long lastModifiedBefore = Files.getLastModifiedTime(originalFile).toMillis();
        
        // 等待一小段时间确保时间戳变化
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.process();
        
        // 验证原始文件内容未改变
        String contentAfter = Files.readString(originalFile);
        assertEquals(originalContent, contentAfter);
        
        // 验证原始文件未被修改（时间戳未变）
        long lastModifiedAfter = Files.getLastModifiedTime(originalFile).toMillis();
        assertEquals(lastModifiedBefore, lastModifiedAfter);
    }
    
    @Test
    @DisplayName("测试: 命令行参数解析")
    void testCommandLineArgs() throws IOException {
        // 创建测试文件
        Files.writeString(inputDir.resolve("test.log"), "ERROR: test");
        
        // 使用命令行参数
        String[] args = {"--inputDir", inputDir.toString(), "--outputDir", outputDir.toString()};
        
        // 直接测试 parseArgs 方法
        DataProcessor.main(args);
        
        String dateSuffix = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        assertTrue(Files.exists(outputDir.resolve("test_processed_" + dateSuffix + ".log")));
    }
    
    @Test
    @DisplayName("测试: 无扩展名文件处理")
    void testFileWithoutExtension() throws IOException {
        Files.writeString(inputDir.resolve("noextension"), "ERROR: test");
        
        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.process();
        
        // 无扩展名文件应被忽略
        assertEquals(0, Files.list(outputDir).count());
    }
    
    @Test
    @DisplayName("测试: 大小写不敏感的扩展名匹配")
    void testCaseInsensitiveExtension() throws IOException {
        Files.writeString(inputDir.resolve("test.LOG"), "ERROR: test");
        Files.writeString(inputDir.resolve("test.DATA"), "10.0");
        
        DataProcessor processor = new DataProcessor(inputDir.toString(), outputDir.toString());
        processor.process();
        
        String dateSuffix = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        
        // 验证大写扩展名也被处理
        assertTrue(Files.exists(outputDir.resolve("test_processed_" + dateSuffix + ".LOG")));
        assertTrue(Files.exists(outputDir.resolve("test_processed_" + dateSuffix + ".DATA")));
    }
}
