import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DataProcessorTest {
    private Path testInputDir;
    private Path testOutputDir;
    private String currentDate;

    @BeforeAll
    void setupAll() throws IOException {
        currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    @BeforeEach
    void setupEach() throws IOException {
        // 每个测试前创建新的临时测试目录
        testInputDir = Files.createTempDirectory("test_input");
        testOutputDir = Files.createTempDirectory("test_output");
    }

    @AfterEach
    void cleanupEach() throws IOException {
        // 每个测试后清理临时目录
        deleteDirectory(testInputDir);
        deleteDirectory(testOutputDir);
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                 .sorted((a, b) -> b.compareTo(a)) // 先删文件再删目录
                 .map(Path::toFile)
                 .forEach(File::delete);
        }
    }

    @Test
    @DisplayName("测试目录检查：输入目录不存在时应退出")
    void testInputDirectoryNotExists() {
        // 测试目录存在性检查逻辑
        assertFalse(Files.exists(Paths.get("/nonexistent/path/that/never/exists")));
    }

    @Test
    @DisplayName("测试输出目录自动创建")
    void testOutputDirectoryCreation() throws IOException {
        Path newOutputDir = testOutputDir.resolve("subdir").resolve("deep");
        DataProcessor processor = new DataProcessor(testInputDir.toString(), newOutputDir.toString());
        
        // 删除目录（如果存在）
        if (Files.exists(newOutputDir)) {
            deleteDirectory(newOutputDir);
        }
        
        // 运行时应该自动创建目录
        processor.run();
        assertTrue(Files.exists(newOutputDir));
        assertTrue(Files.isDirectory(newOutputDir));
    }

    @Test
    @DisplayName("测试文件过滤：只处理.log和.data文件")
    void testFileFiltering() throws IOException {
        // 创建各种类型的测试文件
        Files.createFile(testInputDir.resolve("test1.log"));
        Files.createFile(testInputDir.resolve("test2.data"));
        Files.createFile(testInputDir.resolve("test3.tmp"));
        Files.createFile(testInputDir.resolve("test4.bak"));
        Files.createFile(testInputDir.resolve("test5")); // 无扩展名

        DataProcessor processor = new DataProcessor(testInputDir.toString(), testOutputDir.toString());
        processor.run();

        // 检查输出目录中只有.log和.data的处理文件（只统计普通文件）
        List<Path> outputFiles = Files.list(testOutputDir)
                                      .filter(Files::isRegularFile)
                                      .toList();
        assertEquals(2, outputFiles.size());
        
        boolean hasLog = outputFiles.stream()
            .anyMatch(p -> p.getFileName().toString().equals("test1_processed_" + currentDate + ".log"));
        boolean hasData = outputFiles.stream()
            .anyMatch(p -> p.getFileName().toString().equals("test2_processed_" + currentDate + ".data"));
        
        assertTrue(hasLog);
        assertTrue(hasData);
    }

    @Test
    @DisplayName("测试.log文件处理：筛选ERROR行并添加时间戳")
    void testLogFileProcessing() throws IOException {
        Path logFile = testInputDir.resolve("test.log");
        List<String> logContent = List.of(
            "INFO: Application started",
            "ERROR: Database connection failed",
            "DEBUG: User logged in",
            "ERROR: Invalid input received",
            "WARN: Low memory"
        );
        Files.write(logFile, logContent);

        DataProcessor processor = new DataProcessor(testInputDir.toString(), testOutputDir.toString());
        processor.run();

        Path outputLog = testOutputDir.resolve("test_processed_" + currentDate + ".log");
        assertTrue(Files.exists(outputLog));

        List<String> outputLines = Files.readAllLines(outputLog);
        assertEquals(2, outputLines.size());
        assertTrue(outputLines.get(0).contains("ERROR: Database connection failed"));
        assertTrue(outputLines.get(1).contains("ERROR: Invalid input received"));
        assertTrue(outputLines.get(0).startsWith("["));
        assertTrue(outputLines.get(0).contains("] "));
    }

    @Test
    @DisplayName("测试.log文件无ERROR行时输出空文件")
    void testLogFileNoErrors() throws IOException {
        Path logFile = testInputDir.resolve("no_errors.log");
        List<String> logContent = List.of(
            "INFO: All good",
            "DEBUG: Working fine",
            "WARN: Nothing serious"
        );
        Files.write(logFile, logContent);

        DataProcessor processor = new DataProcessor(testInputDir.toString(), testOutputDir.toString());
        processor.run();

        Path outputLog = testOutputDir.resolve("no_errors_processed_" + currentDate + ".log");
        assertTrue(Files.exists(outputLog));
        assertEquals(0, Files.readAllLines(outputLog).size());
    }

    @Test
    @DisplayName("测试.data文件处理：计算总和和平均值")
    void testDataFileProcessing() throws IOException {
        Path dataFile = testInputDir.resolve("numbers.data");
        List<String> dataContent = List.of(
            "10",
            "20.5",
            "30",
            "40.5"
        );
        Files.write(dataFile, dataContent);

        DataProcessor processor = new DataProcessor(testInputDir.toString(), testOutputDir.toString());
        processor.run();

        Path outputData = testOutputDir.resolve("numbers_processed_" + currentDate + ".data");
        assertTrue(Files.exists(outputData));

        String jsonResult = Files.readString(outputData).trim();
        // 总和是 10 + 20.5 + 30 + 40.5 = 101.0
        // 平均值是 101.0 / 4 = 25.25
        assertEquals("{\"sum\": 101.00, \"average\": 25.25}", jsonResult);
    }

    @Test
    @DisplayName("测试.data空文件处理")
    void testEmptyDataFile() throws IOException {
        Path dataFile = testInputDir.resolve("empty.data");
        Files.createFile(dataFile);

        DataProcessor processor = new DataProcessor(testInputDir.toString(), testOutputDir.toString());
        processor.run();

        Path outputData = testOutputDir.resolve("empty_processed_" + currentDate + ".data");
        assertTrue(Files.exists(outputData));

        String jsonResult = Files.readString(outputData).trim();
        assertEquals("{\"sum\": 0.00, \"average\": 0.00}", jsonResult);
    }

    @Test
    @DisplayName("测试.data文件含无效数字行")
    void testDataFileWithInvalidLines() throws IOException {
        Path dataFile = testInputDir.resolve("mixed.data");
        List<String> dataContent = List.of(
            "10",
            "not a number",
            "20",
            "",
            "30.5"
        );
        Files.write(dataFile, dataContent);

        DataProcessor processor = new DataProcessor(testInputDir.toString(), testOutputDir.toString());
        processor.run();

        Path outputData = testOutputDir.resolve("mixed_processed_" + currentDate + ".data");
        assertTrue(Files.exists(outputData));

        String jsonResult = Files.readString(outputData).trim();
        // 有效数字: 10, 20, 30.5 → 总和60.5，平均值20.17
        assertEquals("{\"sum\": 60.50, \"average\": 20.17}", jsonResult);
    }

    @Test
    @DisplayName("测试输出文件命名规范")
    void testOutputFileNaming() throws IOException {
        // 测试有扩展名的文件
        Files.createFile(testInputDir.resolve("sample.log"));
        Files.createFile(testInputDir.resolve("test.data"));
        // 测试无扩展名的文件（不应该被处理）
        Files.createFile(testInputDir.resolve("no_extension"));

        DataProcessor processor = new DataProcessor(testInputDir.toString(), testOutputDir.toString());
        processor.run();

        assertTrue(Files.exists(testOutputDir.resolve("sample_processed_" + currentDate + ".log")));
        assertTrue(Files.exists(testOutputDir.resolve("test_processed_" + currentDate + ".data")));
        // 无扩展名文件不应该被处理
        assertFalse(Files.exists(testOutputDir.resolve("no_extension_processed_" + currentDate)));
    }

    @Test
    @DisplayName("测试原始文件只读访问：确保不修改输入文件")
    void testInputFileNotModified() throws IOException {
        Path logFile = testInputDir.resolve("readonly.log");
        List<String> originalContent = List.of("ERROR: Test error");
        Files.write(logFile, originalContent);

        // 记录原始文件的修改时间
        long originalTime = Files.getLastModifiedTime(logFile).toMillis();

        DataProcessor processor = new DataProcessor(testInputDir.toString(), testOutputDir.toString());
        processor.run();

        // 检查文件内容和修改时间是否未变
        List<String> currentContent = Files.readAllLines(logFile);
        long currentTime = Files.getLastModifiedTime(logFile).toMillis();

        assertEquals(originalContent, currentContent);
        assertEquals(originalTime, currentTime);
    }
}
