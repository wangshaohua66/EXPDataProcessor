package com.example;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;

public class DataProcessor {
    private static final String DEFAULT_INPUT_DIR = "/home/user/experiment/raw_data";
    private static final String DEFAULT_OUTPUT_DIR = "/home/user/experiment/processed_data";
    private static final String DATE_SUFFIX_FORMAT = "yyyyMMdd";
    private static final String TIMESTAMP_FORMAT = "[yyyy-MM-dd HH:mm:ss] ";

    private Path inputDir;
    private Path outputDir;
    private String currentDateSuffix;
    private String currentTimestamp;

    public DataProcessor(String inputDirPath, String outputDirPath) {
        this.inputDir = Paths.get(inputDirPath);
        this.outputDir = Paths.get(outputDirPath);
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_SUFFIX_FORMAT);
        SimpleDateFormat timestampFormat = new SimpleDateFormat(TIMESTAMP_FORMAT);
        Date now = new Date();
        this.currentDateSuffix = dateFormat.format(now);
        this.currentTimestamp = timestampFormat.format(now);
    }

    public boolean validateInputDirectory() {
        if (!Files.exists(inputDir) || !Files.isDirectory(inputDir)) {
            System.err.println("Error: Input directory does not exist or is not a directory: " + inputDir);
            return false;
        }
        return true;
    }

    public void createOutputDirectory() throws IOException {
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }
    }

    public void processFiles() throws IOException {
        try (Stream<Path> paths = Files.list(inputDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(this::isValidFileType)
                 .forEach(this::processSingleFile);
        }
    }

    private boolean isValidFileType(Path file) {
        String fileName = file.getFileName().toString();
        return fileName.endsWith(".log") || fileName.endsWith(".data");
    }

    private void processSingleFile(Path file) {
        try {
            String fileName = file.getFileName().toString();
            Path outputFile = generateOutputFilePath(fileName);
            
            if (fileName.endsWith(".log")) {
                processLogFile(file, outputFile);
            } else if (fileName.endsWith(".data")) {
                processDataFile(file, outputFile);
            }
        } catch (IOException e) {
            System.err.println("Error processing file: " + file + " - " + e.getMessage());
        }
    }

    private Path generateOutputFilePath(String fileName) {
        String baseName;
        String extension = "";
        
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        } else {
            baseName = fileName;
        }
        
        String newFileName = baseName + "_processed_" + currentDateSuffix + extension;
        return outputDir.resolve(newFileName);
    }

    private void processLogFile(Path inputFile, Path outputFile) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(inputFile);
             BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("ERROR")) {
                    writer.write(currentTimestamp + line);
                    writer.newLine();
                }
            }
        }
    }

    private void processDataFile(Path inputFile, Path outputFile) throws IOException {
        double sum = 0.0;
        int count = 0;
        
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
                    }
                }
            }
        }
        
        double average = count > 0 ? sum / count : 0.0;
        String averageStr = String.format(java.util.Locale.US, "%.2f", average);
        
        String json = String.format(java.util.Locale.US, "{\"sum\": %s, \"average\": %s}", sum, averageStr);
        Files.write(outputFile, json.getBytes());
    }

    public static void main(String[] args) {
        String inputDirPath = DEFAULT_INPUT_DIR;
        String outputDirPath = DEFAULT_OUTPUT_DIR;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--inputDir") && i + 1 < args.length) {
                inputDirPath = args[i + 1];
                i++;
            } else if (args[i].equals("--outputDir") && i + 1 < args.length) {
                outputDirPath = args[i + 1];
                i++;
            }
        }

        DataProcessor processor = new DataProcessor(inputDirPath, outputDirPath);
        
        if (!processor.validateInputDirectory()) {
            System.exit(1);
        }

        try {
            processor.createOutputDirectory();
            processor.processFiles();
            System.out.println("Processing completed successfully.");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
