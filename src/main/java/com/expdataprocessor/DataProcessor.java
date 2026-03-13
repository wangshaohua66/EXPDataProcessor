package com.expdataprocessor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public class DataProcessor {
    private static final String DEFAULT_INPUT_DIR = "/home/user/experiment/raw_data";
    private static final String DEFAULT_OUTPUT_DIR = "/home/user/experiment/processed_data";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    private String inputDir;
    private String outputDir;
    private String currentDateStr;

    public DataProcessor(String inputDir, String outputDir) {
        this.inputDir = (inputDir != null) ? inputDir : DEFAULT_INPUT_DIR;
        this.outputDir = (outputDir != null) ? outputDir : DEFAULT_OUTPUT_DIR;
        this.currentDateStr = LocalDate.now().format(DATE_FORMATTER);
    }

    public static void main(String[] args) {
        String inputDir = parseArgument(args, "--inputDir", DEFAULT_INPUT_DIR);
        String outputDir = parseArgument(args, "--outputDir", DEFAULT_OUTPUT_DIR);

        DataProcessor processor = new DataProcessor(inputDir, outputDir);
        try {
            processor.run();
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String parseArgument(String[] args, String argName, String defaultValue) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(argName) && i + 1 < args.length) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }

    public void run() throws IllegalArgumentException, IOException {
        Path inputPath = Paths.get(inputDir);
        if (!Files.exists(inputPath) || !Files.isDirectory(inputPath)) {
            throw new IllegalArgumentException("Input directory does not exist: " + inputDir);
        }

        Path outputPath = Paths.get(outputDir);
        Files.createDirectories(outputPath);

        processFiles(inputPath, outputPath);
    }

    private void processFiles(Path inputPath, Path outputPath) throws IOException {
        try (Stream<Path> stream = Files.list(inputPath)) {
            stream.filter(Files::isRegularFile)
                  .filter(this::isValidFile)
                  .forEach(file -> processSingleFile(file, outputPath));
        }
    }

    private boolean isValidFile(Path file) {
        String fileName = file.getFileName().toString();
        return fileName.endsWith(".log") || fileName.endsWith(".data");
    }

    private void processSingleFile(Path inputFile, Path outputDirPath) {
        String fileName = inputFile.getFileName().toString();
        String outputFileName = generateOutputFileName(fileName);
        Path outputFile = outputDirPath.resolve(outputFileName);

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

    String generateOutputFileName(String originalFileName) {
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            String name = originalFileName.substring(0, dotIndex);
            String ext = originalFileName.substring(dotIndex + 1);
            return name + "_processed_" + currentDateStr + "." + ext;
        } else {
            return originalFileName + "_processed_" + currentDateStr;
        }
    }

    private void processLogFile(Path inputFile, Path outputFile) throws IOException {
        String timestamp = "[" + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + "] ";

        try (BufferedReader reader = Files.newBufferedReader(inputFile, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("ERROR")) {
                    writer.write(timestamp + line);
                    writer.newLine();
                }
            }
        }
    }

    private void processDataFile(Path inputFile, Path outputFile) throws IOException {
        double sum = 0;
        int count = 0;

        try (BufferedReader reader = Files.newBufferedReader(inputFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    try {
                        double value = Double.parseDouble(line);
                        sum += value;
                        count++;
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }

        double average = count > 0 ? sum / count : 0.0;
        String jsonResult = String.format("{\"sum\": %s, \"average\": %.2f}",
                formatNumber(sum), average);

        Files.write(outputFile, jsonResult.getBytes(StandardCharsets.UTF_8));
    }

    private String formatNumber(double number) {
        if (number == (long) number) {
            return String.format("%d", (long) number);
        } else {
            String formatted = DECIMAL_FORMAT.format(number);
            if (formatted.endsWith(".00")) {
                return formatted.substring(0, formatted.length() - 3);
            }
            if (formatted.endsWith("0")) {
                return formatted.substring(0, formatted.length() - 1);
            }
            return formatted;
        }
    }

    public String getInputDir() {
        return inputDir;
    }

    public String getOutputDir() {
        return outputDir;
    }
}
