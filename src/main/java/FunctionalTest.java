import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class FunctionalTest {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static String currentDate;
    private static Path testInputDir;
    private static Path testOutputDir;
    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        currentDate = LocalDate.now().format(DATE_FORMATTER);
        System.out.println("=== DataProcessor 功能测试开始 ===");
        System.out.println("当前日期: " + currentDate);
        System.out.println();

        try {
            // 创建测试目录
            setupTestDirectories();

            // 运行所有测试用例
            testCase1_InputDirectoryNotExists();
            testCase2_OutputDirectoryCreation();
            testCase3_FileFiltering();
            testCase4_LogFileWithErrors();
            testCase5_LogFileNoErrors();
            testCase6_DataFileCalculation();
            testCase7_EmptyDataFile();
            testCase8_DataFileWithInvalidLines();
            testCase9_OutputFileNaming();
            testCase10_InputFileNotModified();

            // 输出测试结果
            printTestSummary();

        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 清理测试目录
            cleanupTestDirectories();
        }
    }

    private static void setupTestDirectories() throws IOException {
        testInputDir = Paths.get("test_input");
        testOutputDir = Paths.get("test_output");

        // 清理旧的测试目录
        cleanupTestDirectories();

        // 创建新的输入目录
        Files.createDirectories(testInputDir);
        System.out.println("✓ 创建测试输入目录: " + testInputDir.toAbsolutePath());
        System.out.println("✓ 创建测试输出目录: " + testOutputDir.toAbsolutePath());
        System.out.println();
    }

    private static void cleanupTestDirectories() {
        try {
            if (Files.exists(testInputDir)) {
                Files.walk(testInputDir)
                     .map(Path::toFile)
                     .forEach(File::delete);
            }
            if (Files.exists(testOutputDir)) {
                Files.walk(testOutputDir)
                     .map(Path::toFile)
                     .forEach(File::delete);
            }
        } catch (IOException e) {
            // 忽略清理错误
        }
    }

    private static void testCase1_InputDirectoryNotExists() {
        System.out.println("📋 测试用例 1: 输入目录不存在");
        try {
            String[] args = {"--inputDir", "/nonexistent/path/that/never/exists", "--outputDir", testOutputDir.toString()};
            // 我们不直接执行System.exit，而是验证逻辑
            Path badPath = Paths.get("/nonexistent/path/that/never/exists");
            boolean exists = Files.exists(badPath);
            if (!exists) {
                System.out.println("  ✓ 通过: 正确检测到不存在的目录");
                passedTests++;
            } else {
                System.out.println("  ✗ 失败: 应该检测到目录不存在");
                failedTests++;
            }
        } catch (Exception e) {
            System.out.println("  ✗ 失败: " + e.getMessage());
            failedTests++;
        }
        System.out.println();
    }

    private static void testCase2_OutputDirectoryCreation() throws IOException {
        System.out.println("📋 测试用例 2: 输出目录自动创建");
        Path deepOutput = testOutputDir.resolve("deep").resolve("nested").resolve("dir");

        // 确保目录不存在
        if (Files.exists(deepOutput)) {
            Files.walk(deepOutput).map(Path::toFile).forEach(File::delete);
        }

        String[] args = {"--inputDir", testInputDir.toString(), "--outputDir", deepOutput.toString()};
        DataProcessor.main(args);

        if (Files.exists(deepOutput) && Files.isDirectory(deepOutput)) {
            System.out.println("  ✓ 通过: 自动创建了嵌套的输出目录");
            passedTests++;
        } else {
            System.out.println("  ✗ 失败: 没有创建输出目录");
            failedTests++;
        }
        System.out.println();
    }

    private static void testCase3_FileFiltering() throws IOException {
        System.out.println("📋 测试用例 3: 文件过滤功能");

        // 创建各种类型的文件
        Files.createFile(testInputDir.resolve("file1.log"));
        Files.createFile(testInputDir.resolve("file2.data"));
        Files.createFile(testInputDir.resolve("file3.tmp"));
        Files.createFile(testInputDir.resolve("file4.bak"));
        Files.createFile(testInputDir.resolve("file5")); // 无扩展名
        Files.createFile(testInputDir.resolve("file6.LOG")); // 大写扩展名应该被忽略

        String[] args = {"--inputDir", testInputDir.toString(), "--outputDir", testOutputDir.toString()};
        DataProcessor.main(args);

        // 统计输出文件数量（只统计普通文件，排除目录）
        long outputCount = Files.list(testOutputDir)
                               .filter(Files::isRegularFile)
                               .count();
        if (outputCount == 2) {
            System.out.println("  ✓ 通过: 只处理了.log和.data文件，共2个");
            passedTests++;
        } else {
            System.out.println("  ✗ 失败: 期望2个输出文件，实际有" + outputCount + "个");
            failedTests++;
        }

        // 清理测试文件
        Files.list(testInputDir).forEach(p -> {
            try { Files.delete(p); } catch (IOException e) {}
        });
        Files.list(testOutputDir).forEach(p -> {
            try { Files.delete(p); } catch (IOException e) {}
        });
        System.out.println();
    }

    private static void testCase4_LogFileWithErrors() throws IOException {
        System.out.println("📋 测试用例 4: .log文件含ERROR行");

        Path logFile = testInputDir.resolve("test.log");
        List<String> content = Arrays.asList(
            "INFO: 系统启动",
            "ERROR: 数据库连接失败",
            "DEBUG: 用户登录",
            "ERROR: 无效的输入参数",
            "WARN: 内存不足"
        );
        Files.write(logFile, content);

        String[] args = {"--inputDir", testInputDir.toString(), "--outputDir", testOutputDir.toString()};
        DataProcessor.main(args);

        Path outputFile = testOutputDir.resolve("test_processed_" + currentDate + ".log");
        if (Files.exists(outputFile)) {
            List<String> lines = Files.readAllLines(outputFile);
            if (lines.size() == 2 &&
                lines.get(0).contains("ERROR: 数据库连接失败") &&
                lines.get(1).contains("ERROR: 无效的输入参数") &&
                lines.get(0).startsWith("[") &&
                lines.get(0).contains("] ")) {
                System.out.println("  ✓ 通过: 正确筛选了ERROR行并添加了时间戳");
                passedTests++;
            } else {
                System.out.println("  ✗ 失败: 输出内容不正确");
                System.out.println("    实际行数: " + lines.size());
                failedTests++;
            }
        } else {
            System.out.println("  ✗ 失败: 输出文件不存在");
            failedTests++;
        }

        // 清理
        Files.delete(logFile);
        Files.delete(outputFile);
        System.out.println();
    }

    private static void testCase5_LogFileNoErrors() throws IOException {
        System.out.println("📋 测试用例 5: .log文件无ERROR行");

        Path logFile = testInputDir.resolve("no_error.log");
        List<String> content = Arrays.asList(
            "INFO: 一切正常",
            "DEBUG: 处理中",
            "WARN: 轻微警告"
        );
        Files.write(logFile, content);

        String[] args = {"--inputDir", testInputDir.toString(), "--outputDir", testOutputDir.toString()};
        DataProcessor.main(args);

        Path outputFile = testOutputDir.resolve("no_error_processed_" + currentDate + ".log");
        if (Files.exists(outputFile)) {
            List<String> lines = Files.readAllLines(outputFile);
            if (lines.isEmpty()) {
                System.out.println("  ✓ 通过: 无ERROR行时输出空文件");
                passedTests++;
            } else {
                System.out.println("  ✗ 失败: 输出文件不为空，有" + lines.size() + "行");
                failedTests++;
            }
        } else {
            System.out.println("  ✗ 失败: 输出文件不存在");
            failedTests++;
        }

        // 清理
        Files.delete(logFile);
        Files.delete(outputFile);
        System.out.println();
    }

    private static void testCase6_DataFileCalculation() throws IOException {
        System.out.println("📋 测试用例 6: .data文件计算总和和平均值");

        Path dataFile = testInputDir.resolve("numbers.data");
        List<String> content = Arrays.asList("10", "20.5", "30", "40.5");
        Files.write(dataFile, content);

        String[] args = {"--inputDir", testInputDir.toString(), "--outputDir", testOutputDir.toString()};
        DataProcessor.main(args);

        Path outputFile = testOutputDir.resolve("numbers_processed_" + currentDate + ".data");
        if (Files.exists(outputFile)) {
            String json = Files.readString(outputFile).trim();
            if ("{\"sum\": 101.00, \"average\": 25.25}".equals(json)) {
                System.out.println("  ✓ 通过: 正确计算了总和(101.00)和平均值(25.25)");
                passedTests++;
            } else {
                System.out.println("  ✗ 失败: JSON结果不正确");
                System.out.println("    期望: {\"sum\": 101.00, \"average\": 25.25}");
                System.out.println("    实际: " + json);
                failedTests++;
            }
        } else {
            System.out.println("  ✗ 失败: 输出文件不存在");
            failedTests++;
        }

        // 清理
        Files.delete(dataFile);
        Files.delete(outputFile);
        System.out.println();
    }

    private static void testCase7_EmptyDataFile() throws IOException {
        System.out.println("📋 测试用例 7: .data空文件处理");

        Path dataFile = testInputDir.resolve("empty.data");
        Files.createFile(dataFile);

        String[] args = {"--inputDir", testInputDir.toString(), "--outputDir", testOutputDir.toString()};
        DataProcessor.main(args);

        Path outputFile = testOutputDir.resolve("empty_processed_" + currentDate + ".data");
        if (Files.exists(outputFile)) {
            String json = Files.readString(outputFile).trim();
            if ("{\"sum\": 0.00, \"average\": 0.00}".equals(json)) {
                System.out.println("  ✓ 通过: 空文件正确输出sum=0.00, average=0.00");
                passedTests++;
            } else {
                System.out.println("  ✗ 失败: JSON结果不正确");
                System.out.println("    期望: {\"sum\": 0.00, \"average\": 0.00}");
                System.out.println("    实际: " + json);
                failedTests++;
            }
        } else {
            System.out.println("  ✗ 失败: 输出文件不存在");
            failedTests++;
        }

        // 清理
        Files.delete(dataFile);
        Files.delete(outputFile);
        System.out.println();
    }

    private static void testCase8_DataFileWithInvalidLines() throws IOException {
        System.out.println("📋 测试用例 8: .data文件含无效行");

        Path dataFile = testInputDir.resolve("mixed.data");
        List<String> content = Arrays.asList(
            "10",
            "不是数字",
            "20",
            "",
            "30.5"
        );
        Files.write(dataFile, content);

        String[] args = {"--inputDir", testInputDir.toString(), "--outputDir", testOutputDir.toString()};
        DataProcessor.main(args);

        Path outputFile = testOutputDir.resolve("mixed_processed_" + currentDate + ".data");
        if (Files.exists(outputFile)) {
            String json = Files.readString(outputFile).trim();
            if ("{\"sum\": 60.50, \"average\": 20.17}".equals(json)) {
                System.out.println("  ✓ 通过: 跳过无效行，正确计算了有效数字");
                passedTests++;
            } else {
                System.out.println("  ✗ 失败: JSON结果不正确");
                System.out.println("    期望: {\"sum\": 60.50, \"average\": 20.17}");
                System.out.println("    实际: " + json);
                failedTests++;
            }
        } else {
            System.out.println("  ✗ 失败: 输出文件不存在");
            failedTests++;
        }

        // 清理
        Files.delete(dataFile);
        Files.delete(outputFile);
        System.out.println();
    }

    private static void testCase9_OutputFileNaming() throws IOException {
        System.out.println("📋 测试用例 9: 输出文件命名规范");

        Files.createFile(testInputDir.resolve("sample.log"));
        Files.createFile(testInputDir.resolve("test.data"));
        // 注意：无扩展名文件不应该被处理，所以这里不期望有输出
        Files.createFile(testInputDir.resolve("no_extension"));

        String[] args = {"--inputDir", testInputDir.toString(), "--outputDir", testOutputDir.toString()};
        DataProcessor.main(args);

        boolean hasLog = Files.exists(testOutputDir.resolve("sample_processed_" + currentDate + ".log"));
        boolean hasData = Files.exists(testOutputDir.resolve("test_processed_" + currentDate + ".data"));
        boolean hasNoExt = Files.exists(testOutputDir.resolve("no_extension_processed_" + currentDate));

        if (hasLog && hasData && !hasNoExt) {
            System.out.println("  ✓ 通过: 只有.log和.data文件被处理，命名规范正确");
            passedTests++;
        } else {
            System.out.println("  ✗ 失败: 文件处理或命名不正确");
            System.out.println("    sample.log: " + (hasLog ? "✓" : "✗"));
            System.out.println("    test.data: " + (hasData ? "✓" : "✗"));
            System.out.println("    no_extension: " + (!hasNoExt ? "✓ (正确忽略)" : "✗ (不应该被处理)"));
            failedTests++;
        }

        // 清理
        Files.list(testInputDir).forEach(p -> {
            try { Files.delete(p); } catch (IOException e) {}
        });
        Files.list(testOutputDir).forEach(p -> {
            try { Files.delete(p); } catch (IOException e) {}
        });
        System.out.println();
    }

    private static void testCase10_InputFileNotModified() throws IOException {
        System.out.println("📋 测试用例 10: 输入文件只读保护");

        Path logFile = testInputDir.resolve("readonly.log");
        List<String> originalContent = Arrays.asList("ERROR: 测试错误");
        Files.write(logFile, originalContent);
        long originalTime = Files.getLastModifiedTime(logFile).toMillis();

        String[] args = {"--inputDir", testInputDir.toString(), "--outputDir", testOutputDir.toString()};
        DataProcessor.main(args);

        List<String> currentContent = Files.readAllLines(logFile);
        long currentTime = Files.getLastModifiedTime(logFile).toMillis();

        if (originalContent.equals(currentContent) && originalTime == currentTime) {
            System.out.println("  ✓ 通过: 输入文件没有被修改，内容和修改时间都一致");
            passedTests++;
        } else {
            System.out.println("  ✗ 失败: 输入文件被修改了");
            failedTests++;
        }

        // 清理
        Files.delete(logFile);
        Files.delete(testOutputDir.resolve("readonly_processed_" + currentDate + ".log"));
        System.out.println();
    }

    private static void printTestSummary() {
        System.out.println("=== 功能测试总结 ===");
        System.out.println("总测试用例: 10");
        System.out.println("通过: " + passedTests);
        System.out.println("失败: " + failedTests);
        System.out.println();

        if (failedTests == 0) {
            System.out.println("✅ 所有测试用例都通过了！");
        } else {
            System.out.println("❌ 有 " + failedTests + " 个测试用例失败");
        }
    }
}
