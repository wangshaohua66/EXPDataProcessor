package com.experiment;

import java.io.*;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DataProcessor {
    private static final String DEFAULT_INPUT_DIR = "/home/user/experiment/raw_data";
    private static final String DEFAULT_OUTPUT_DIR = "/home/user/experiment/processed_data";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    private Path inputDir;
    private Path outputDir;

    public static void main(String[] args) {
        DataProcessor processor = new DataProcessor();
        processor.parseArguments(args);
        int exitCode = processor.processWithExitCode();
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public void parseArguments(String[] args) {
        String inputPath = DEFAULT_INPUT_DIR;
        String outputPath = DEFAULT_OUTPUT_DIR;

        for (int i = 0; i < args.length; i++) {
            if ("--inputDir".equals(args[i]) && i + 1 < args.length) {
                inputPath = args[++i];
            } else if ("--outputDir".equals(args[i]) && i + 1 < args.length) {
                outputPath = args[++i];
            }
        }

        this.inputDir = Paths.get(inputPath);
        this.outputDir = Paths.get(outputPath);
    }

    public void process() {
        int exitCode = processWithExitCode();
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public int processWithExitCode() {
        if (!Files.exists(inputDir)) {
            System.err.println("Error: Input directory does not exist: " + inputDir);
            return 1;
        }

        try {
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }
        } catch (IOException e) {
            System.err.println("Error: Failed to create output directory: " + outputDir);
            return 1;
        }

        try {
            Files.walk(inputDir)
                    .filter(Files::isRegularFile)
                    .filter(this::isValidFileExtension)
                    .forEach(this::processFile);
        } catch (IOException e) {
            System.err.println("Error: Failed to process files: " + e.getMessage());
            return 1;
        }
        
        return 0;
    }

    private boolean isValidFileExtension(Path file) {
        String fileName = file.getFileName().toString();
        return fileName.endsWith(".log") || fileName.endsWith(".data");
    }

    private void processFile(Path inputFile) {
        String fileName = inputFile.getFileName().toString();
        String outputFileName = generateOutputFileName(fileName);
        Path outputFile = outputDir.resolve(outputFileName);

        try {
            if (fileName.endsWith(".log")) {
                processLogFile(inputFile, outputFile);
            } else if (fileName.endsWith(".data")) {
                processDataFile(inputFile, outputFile);
            }
        } catch (IOException e) {
            System.err.println("Error processing file " + fileName + ": " + e.getMessage());
        }
    }

    private String generateOutputFileName(String originalFileName) {
        String currentDate = LocalDateTime.now().format(DATE_FORMATTER);
        
        if (originalFileName.endsWith(".log")) {
            String baseName = originalFileName.substring(0, originalFileName.length() - 4);
            return baseName + "_processed_" + currentDate + ".log";
        } else if (originalFileName.endsWith(".data")) {
            String baseName = originalFileName.substring(0, originalFileName.length() - 5);
            return baseName + "_processed_" + currentDate + ".data";
        } else {
            return originalFileName + "_processed_" + currentDate;
        }
    }

    private void processLogFile(Path inputFile, Path outputFile) throws IOException {
        List<String> errorLines = new ArrayList<>();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

        try (BufferedReader reader = Files.newBufferedReader(inputFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("ERROR")) {
                    errorLines.add("[" + timestamp + "] " + line);
                }
            }
        }

        Files.write(outputFile, errorLines);
    }

    private void processDataFile(Path inputFile, Path outputFile) throws IOException {
        List<Double> numbers = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(inputFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    try {
                        double number = Double.parseDouble(line);
                        numbers.add(number);
                    } catch (NumberFormatException e) {
                        System.err.println("Warning: Invalid number format in file " + 
                                inputFile.getFileName() + ": " + line);
                    }
                }
            }
        }

        double sum = numbers.stream().mapToDouble(Double::doubleValue).sum();
        double average = numbers.isEmpty() ? 0.0 : sum / numbers.size();

        String jsonContent = "{\"sum\": " + DECIMAL_FORMAT.format(sum) + 
                ", \"average\": " + DECIMAL_FORMAT.format(average) + "}";

        Files.write(outputFile, jsonContent.getBytes());
    }

    public Path getInputDir() {
        return inputDir;
    }

    public Path getOutputDir() {
        return outputDir;
    }
}
