package com.expdataprocessor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;

public class DataProcessor {
    private static final String DEFAULT_INPUT_DIR = "/home/user/experiment/raw_data";
    private static final String DEFAULT_OUTPUT_DIR = "/home/user/experiment/processed_data";
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat LOG_TIMESTAMP_FORMAT = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ");

    private Path inputDir;
    private Path outputDir;
    private String currentDateStr;
    private String logTimestampPrefix;

    public DataProcessor(String inputDirPath, String outputDirPath) {
        this.inputDir = Paths.get(inputDirPath);
        this.outputDir = Paths.get(outputDirPath);
        Date now = new Date();
        this.currentDateStr = FILE_DATE_FORMAT.format(now);
        this.logTimestampPrefix = LOG_TIMESTAMP_FORMAT.format(now);
    }

    public boolean validateInputDirectory() {
        if (!Files.exists(inputDir) || !Files.isDirectory(inputDir)) {
            System.err.println("Error: Input directory does not exist: " + inputDir.toAbsolutePath());
            return false;
        }
        return true;
    }

    public void createOutputDirectory() throws IOException {
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }
    }

    public void process() throws IOException {
        try (Stream<Path> paths = Files.walk(inputDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(this::isProcessableFile)
                 .forEach(this::processFile);
        }
    }

    private boolean isProcessableFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith(".log") || fileName.endsWith(".data");
    }

    private String generateOutputFileName(String inputFileName) {
        int lastDotIndex = inputFileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return inputFileName + "_processed_" + currentDateStr;
        }
        String name = inputFileName.substring(0, lastDotIndex);
        String ext = inputFileName.substring(lastDotIndex + 1);
        return name + "_processed_" + currentDateStr + "." + ext;
    }

    private void processFile(Path inputFile) {
        try {
            String fileName = inputFile.getFileName().toString();
            String outputFileName = generateOutputFileName(fileName);
            Path outputFile = outputDir.resolve(outputFileName);

            if (fileName.endsWith(".log")) {
                processLogFile(inputFile, outputFile);
            } else if (fileName.endsWith(".data")) {
                processDataFile(inputFile, outputFile);
            }
        } catch (IOException e) {
            System.err.println("Error processing file: " + inputFile + " - " + e.getMessage());
        }
    }

    private void processLogFile(Path inputFile, Path outputFile) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(inputFile, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("ERROR")) {
                    writer.write(logTimestampPrefix + line);
                    writer.newLine();
                }
            }
        }
    }

    private void processDataFile(Path inputFile, Path outputFile) throws IOException {
        double sum = 0.0;
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
        String jsonResult = String.format("{\"sum\": %.2f, \"average\": %.2f}", sum, average);
        
        Files.write(outputFile, jsonResult.getBytes(StandardCharsets.UTF_8));
    }

    public static void main(String[] args) {
        String inputDir = DEFAULT_INPUT_DIR;
        String outputDir = DEFAULT_OUTPUT_DIR;

        for (int i = 0; i < args.length; i++) {
            if ("--inputDir".equals(args[i]) && i + 1 < args.length) {
                inputDir = args[i + 1];
                i++;
            } else if ("--outputDir".equals(args[i]) && i + 1 < args.length) {
                outputDir = args[i + 1];
                i++;
            }
        }

        DataProcessor processor = new DataProcessor(inputDir, outputDir);

        if (!processor.validateInputDirectory()) {
            System.exit(1);
        }

        try {
            processor.createOutputDirectory();
            processor.process();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
