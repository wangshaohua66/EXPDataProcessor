import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class DataProcessor {
    private static final String DEFAULT_INPUT_DIR = "/home/user/experiment/raw_data";
    private static final String DEFAULT_OUTPUT_DIR = "/home/user/experiment/processed_data";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Path inputDir;
    private Path outputDir;
    private String currentDate;
    private String currentTimestamp;

    public DataProcessor(String inputDirPath, String outputDirPath) {
        this.inputDir = Paths.get(inputDirPath);
        this.outputDir = Paths.get(outputDirPath);
        this.currentDate = LocalDate.now().format(DATE_FORMATTER);
        this.currentTimestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    }

    public static void main(String[] args) {
        String inputDir = DEFAULT_INPUT_DIR;
        String outputDir = DEFAULT_OUTPUT_DIR;

        // 解析命令行参数
        for (int i = 0; i < args.length; i++) {
            if ("--inputDir".equals(args[i]) && i + 1 < args.length) {
                inputDir = args[++i];
            } else if ("--outputDir".equals(args[i]) && i + 1 < args.length) {
                outputDir = args[++i];
            }
        }

        DataProcessor processor = new DataProcessor(inputDir, outputDir);
        processor.run();
    }

    public void run() {
        // 检查输入目录是否存在
        if (!Files.exists(inputDir) || !Files.isDirectory(inputDir)) {
            System.err.println("错误：输入目录不存在或不是目录 - " + inputDir);
            System.exit(1);
        }

        // 创建输出目录（包括父目录）
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            System.err.println("错误：无法创建输出目录 - " + outputDir);
            e.printStackTrace();
            System.exit(1);
        }

        // 处理所有符合条件的文件
        try (Stream<Path> files = Files.list(inputDir)) {
            files.filter(this::isValidFile)
                 .forEach(this::processFile);
        } catch (IOException e) {
            System.err.println("错误：读取输入目录失败");
            e.printStackTrace();
            System.exit(1);
        }
    }

    // 检查文件是否符合处理条件（.log或.data扩展名）
    private boolean isValidFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        String fileName = path.getFileName().toString();
        return fileName.endsWith(".log") || fileName.endsWith(".data");
    }

    // 处理单个文件
    private void processFile(Path inputFile) {
        String fileName = inputFile.getFileName().toString();
        Path outputFile = generateOutputPath(fileName);

        try {
            if (fileName.endsWith(".log")) {
                processLogFile(inputFile, outputFile);
            } else if (fileName.endsWith(".data")) {
                processDataFile(inputFile, outputFile);
            }
        } catch (IOException e) {
            System.err.println("错误：处理文件失败 - " + fileName);
            e.printStackTrace();
        }
    }

    // 生成输出文件路径
    private Path generateOutputPath(String originalFileName) {
        String baseName;
        String extension;

        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = originalFileName.substring(0, dotIndex);
            extension = originalFileName.substring(dotIndex);
        } else {
            baseName = originalFileName;
            extension = "";
        }

        String newFileName = baseName + "_processed_" + currentDate + extension;
        return outputDir.resolve(newFileName);
    }

    // 处理.log文件：筛选ERROR行并添加时间戳
    private void processLogFile(Path inputFile, Path outputFile) throws IOException {
        List<String> errorLines = new ArrayList<>();

        // 只读方式读取文件
        try (BufferedReader reader = Files.newBufferedReader(inputFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("ERROR")) {
                    errorLines.add("[" + currentTimestamp + "] " + line);
                }
            }
        }

        // 写入输出文件
        Files.write(outputFile, errorLines);
    }

    // 处理.data文件：计算总和和平均值并输出JSON
    private void processDataFile(Path inputFile, Path outputFile) throws IOException {
        double sum = 0.0;
        int count = 0;

        // 只读方式读取文件
        try (BufferedReader reader = Files.newBufferedReader(inputFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    try {
                        double value = Double.parseDouble(line);
                        sum += value;
                        count++;
                    } catch (NumberFormatException e) {
                        // 跳过无效数字行
                        System.err.println("警告：跳过无效数字行 - " + line);
                    }
                }
            }
        }

        double average = count > 0 ? sum / count : 0.0;
        String jsonResult = String.format("{\"sum\": %.2f, \"average\": %.2f}", sum, average);

        // 写入输出文件
        Files.write(outputFile, jsonResult.getBytes());
    }

    // Getters for testing
    public Path getInputDir() {
        return inputDir;
    }

    public Path getOutputDir() {
        return outputDir;
    }

    public String getCurrentDate() {
        return currentDate;
    }
}
