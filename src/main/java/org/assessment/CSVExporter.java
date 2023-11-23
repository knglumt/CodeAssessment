package org.assessment;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The CSVExporter class provides methods for processing folders, extracting grades, and generating a CSV file.
 */
public class CSVExporter {

    /**
     * Processes the specified root folder, extracts student grades from files, and organizes the data.
     *
     * @param rootFolder The root folder to process.
     * @return A map containing subfolder names as keys and a map of student IDs and their total grades as values.
     */
    static Map<String, Map<String, Integer>> processFolder(String rootFolder) {
        Map<String, Map<String, Integer>> results = new HashMap<>();

        File folder = new File(rootFolder);
        File[] subfolders = folder.listFiles(File::isDirectory);

        if (subfolders != null) {
            for (File subfolder : subfolders) {
                String folderName = subfolder.getName();
                Map<String, Integer> studentGrades = new HashMap<>();

                for (File file : subfolder.listFiles(File::isFile)) {
                    String fileName = file.getName();
                    String studentId = getStudentId(fileName);

                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        int sum = 0;
                        while ((line = reader.readLine()) != null) {
                            if (line.contains("@grade")) {
                                sum += extractAndSumGrades(line);
                            }
                        }
                        studentGrades.put(studentId, sum);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                results.put(folderName, studentGrades);
            }
        }

        return results;
    }

    /**
     * Extracts the student ID from a given file name.
     *
     * @param fileName The name of the file.
     * @return The extracted student ID.
     */
    private static String getStudentId(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex != -1) ? fileName.substring(0, dotIndex) : fileName;
    }

    /**
     * Extracts and sums up grades from a line containing "@grade" comments.
     *
     * @param line The line containing grade information.
     * @return The sum of grades extracted from the line.
     */
    private static int extractAndSumGrades(String line) {
        String[] grades = line.replaceAll("[^0-9]+", " ").trim().split(" ");
        int sum = 0;

        for (String grade : grades) {
            if (!grade.isEmpty()) {
                sum += Integer.parseInt(grade);
            }
        }

        return sum;
    }

    /**
     * Generates a CSV file based on the processed data.
     *
     * @param results   The processed data containing subfolder names and student grades.
     * @param outputCsv The path to the output CSV file.
     */
    static void generateCsv(Map<String, Map<String, Integer>> results, String outputCsv) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputCsv))) {
            writer.print("Student ID,");
            for (String folderName : results.keySet()) {
                writer.print(folderName + ",");
            }
            writer.println("Grade");

            Set<String> allStudentIds = results.values().stream()
                    .flatMap(folderGrades -> folderGrades.keySet().stream())
                    .collect(Collectors.toSet());

            for (String studentId : allStudentIds) {
                writer.print(studentId + ",");

                int totalGrade = 0;
                for (Map.Entry<String, Map<String, Integer>> entry : results.entrySet()) {
                    Map<String, Integer> folderGrades = entry.getValue();
                    int grade = folderGrades.getOrDefault(studentId, 0);
                    writer.print(grade + ",");
                    totalGrade += grade;
                }
                writer.println(totalGrade);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
