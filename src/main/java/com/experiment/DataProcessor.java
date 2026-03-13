package com.experiment;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DataProcessor {
    
    private static final String DEFAULT_INPUT_DIR = "/home/user/experiment/raw_data";
    private static final String DEFAULT_OUTPUT_DIR = "/home/user/experiment/processed_data";
    private static final String[] VALID_EXTENSIONS = {".log", ".data"};
    
    private final String inputDir;
    private final String outputDir;
    private final String currentDate;
    private final String currentTimestamp;
    
    public DataProcessor(String inputDir, String outputDir) {
        this.inputDir = inputDir != null ? inputDir : DEFAULT_INPUT_DIR;
        this.outputDir = outputDir != null ? outputDir : DEFAULT_OUTPUT_DIR;
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        this.currentDate = dateFormat.format(now);
        this.currentTimestamp = timestampFormat.format(now);
    }
    
    public static void main(String[] args) {
        Map<String, String> params = parseArgs(args);
        String inputDir = params.get("--inputDir");
        String outputDir = params.get("--outputDir");
        
        DataProcessor processor = new DataProcessor(inputDir, outputDir);
        processor.process();
    }
    
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--") && i + 1 < args.length && !args[i + 1].startsWith("--")) {
                params.put(args[i], args[i + 1]);
                i++;
            }
        }
        return params;
    }
    
    public void process() {
        Path inputPath = Paths.get(inputDir);
        Path outputPath = Paths.get(outputDir);
        
        // 检查输入目录是否存在
        if (!Files.exists(inputPath)) {
            System.err.println("Error: Input directory does not exist: " + inputDir);
            System.exit(1);
        }
        
        if (!Files.isDirectory(inputPath)) {
            System.err.println("Error: Input path is not a directory: " + inputDir);
            System.exit(1);
        }
        
        // 创建输出目录（如果不存在）
        try {
            Files.createDirectories(outputPath);
        } catch (IOException e) {
            System.err.println("Error: Failed to create output directory: " + outputDir);
            e.printStackTrace();
            System.exit(1);
        }
        
        // 处理文件
        try {
            Files.list(inputPath).forEach(this::processFile);
            System.out.println("Processing completed successfully.");
        } catch (IOException e) {
            System.err.println("Error: Failed to list files in input directory: " + inputDir);
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private void processFile(Path filePath) {
        if (!Files.isRegularFile(filePath)) {
            return;
        }
        
        String fileName = filePath.getFileName().toString();
        String extension = getFileExtension(fileName);
        
        // 只处理 .log 和 .data 文件
        if (!isValidExtension(extension)) {
            System.out.println("Skipping file (invalid extension): " + fileName);
            return;
        }
        
        String outputFileName = generateOutputFileName(fileName, extension);
        Path outputFilePath = Paths.get(outputDir, outputFileName);
        
        try {
            if (".log".equalsIgnoreCase(extension)) {
                processLogFile(filePath, outputFilePath);
            } else if (".data".equalsIgnoreCase(extension)) {
                processDataFile(filePath, outputFilePath);
            }
            System.out.println("Processed: " + fileName + " -> " + outputFileName);
        } catch (IOException e) {
            System.err.println("Error processing file: " + fileName);
            e.printStackTrace();
        }
    }
    
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == 0) {
            return "";
        }
        return fileName.substring(lastDotIndex);
    }
    
    private boolean isValidExtension(String extension) {
        for (String validExt : VALID_EXTENSIONS) {
            if (validExt.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
    
    private String generateOutputFileName(String originalFileName, String extension) {
        String baseName;
        if (extension.isEmpty()) {
            baseName = originalFileName;
        } else {
            baseName = originalFileName.substring(0, originalFileName.length() - extension.length());
        }
        return baseName + "_processed_" + currentDate + extension;
    }
    
    private void processLogFile(Path inputFile, Path outputFile) throws IOException {
        // 以只读方式打开输入文件
        try (BufferedReader reader = Files.newBufferedReader(inputFile);
             BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("ERROR")) {
                    writer.write("[" + currentTimestamp + "] " + line);
                    writer.newLine();
                }
            }
        }
    }
    
    private void processDataFile(Path inputFile, Path outputFile) throws IOException {
        double sum = 0.0;
        int count = 0;
        
        // 以只读方式打开输入文件
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
                        System.err.println("Warning: Invalid number format in file " + 
                            inputFile.getFileName() + ": " + line);
                    }
                }
            }
        }
        
        double average = count > 0 ? sum / count : 0.0;
        // 保留两位小数
        average = Math.round(average * 100.0) / 100.0;
        sum = Math.round(sum * 100.0) / 100.0;
        
        // 写入 JSON 格式
        String jsonResult = String.format("{\"sum\": %.2f, \"average\": %.2f}", sum, average);
        
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            writer.write(jsonResult);
        }
    }
}
