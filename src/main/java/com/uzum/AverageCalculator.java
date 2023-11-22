package com.uzum;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AverageCalculator {

    private static volatile double overallSum = 0.0;
    private static volatile int overallCount = 0;

    public static void main(String[] args) {
        if (args.length != 4 || !args[0].equals("--files") || !args[2].equals("--par")) {
            System.out.println("Usage: java AverageCalculator --files \"file1,file2,...\" --par <parallelism_level_number>");
            System.exit(1);
        }

        int parallelismLevel = Integer.parseInt(args[3]);

        if (parallelismLevel <= 0) {
            System.out.println("Parallelism level should be a positive integer.");
            System.exit(1);
        }

        String[] filePaths = args[1].split(",");
        calculateAverages(filePaths, parallelismLevel);

        double overallAverage = overallSum / overallCount;
        System.out.println("Overall Average: " + overallAverage);
    }

    private static void calculateAverages(String[] filePaths, int parallelismLevel) {
        ExecutorService executorService = Executors.newFixedThreadPool(parallelismLevel);

        for (String filePath : filePaths) {
            executorService.submit(() -> {
                try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                    String line;
                    int sum = 0;
                    int count = 0;

                    while ((line = reader.readLine()) != null) {
                        int number = Integer.parseInt(line.trim());
                        sum += number;
                        count++;
                    }

                    double average = count > 0 ? (double) sum / count : 0;
                    System.out.println("File: " + filePath + ", Average: " + average);

                    // Update overall sum and count in a synchronized manner
                    synchronized (AverageCalculator.class) {
                        overallSum += sum;
                        overallCount += count;
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error occurred while reading numbers from files", e);
                }
            });
        }

        executorService.shutdown();

        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Error occurred while termination executor service", e);
        }
    }
}
