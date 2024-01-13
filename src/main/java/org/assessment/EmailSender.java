package org.assessment;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.swing.*;

/**
 * This class consolidates files for students, sends an email with file details,
 * and reads email configuration from a file.
 */
public class EmailSender {

    private final String textFilePath;
    private final String folderPath;
    private final String emailConfigFile;

    private Session session;

    private String smtpUser;
    /**
     * Constructor to initialize file paths and configurations.
     *
     * @param textFilePath      The path to the student data text file
     * @param folderPath        The path to the folders
     * @param emailConfigFile   The path to the email configuration file
     */
    public EmailSender(String textFilePath, String folderPath, String emailConfigFile) {
        this.textFilePath = textFilePath;
        this.folderPath = folderPath;
        this.emailConfigFile = emailConfigFile;
    }

    /**
     * Main method to execute the file consolidation and email sending process.
     */
    public void run(String subject) {
        List<String[]> studentData = readStudentData();

        setEmailConfig();

        for (String[] student : studentData) {
            String studentId = student[0];
            String email = student[1];

            List<String> studentFiles = scanFolders(studentId);
            Map<String, StringBuilder> consolidatedFiles = consolidateFilesInMemory(studentId, studentFiles);
            sendEmailWithConsolidatedContent(subject, studentId, email, consolidatedFiles);
        }

        JOptionPane.showMessageDialog(null, "Emails have been sent to students!");
    }

    /**
     * Reads student data from a text file.
     *
     * @return A list of String arrays containing student data (id and email)
     */
    private List<String[]> readStudentData() {
        List<String[]> studentData = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(textFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] student = line.split(",");
                studentData.add(student);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return studentData;
    }

    /**
     * Scans folders for files related to a student.
     *
     * @param studentId   The ID of the student
     * @return A list of file paths related to the student
     */
    private List<String> scanFolders(String studentId) {
        List<String> studentFiles = new ArrayList<>();
        File folder = new File(folderPath);

        scanFolderRecursively(folder, studentId, studentFiles);

        return studentFiles;
    }

    private void scanFolderRecursively(File folder, String studentId, List<String> studentFiles) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // Recursive call for subdirectories
                        scanFolderRecursively(file, studentId, studentFiles);
                    } else if (file.isFile() && file.getName().toLowerCase().contains(studentId.toLowerCase())) {
                        studentFiles.add(file.getAbsolutePath());
                    }
                }
            } else {
                System.err.println("Error listing files in the folder: " + folder.getAbsolutePath());
            }
        } else {
            System.err.println("The specified path is not a directory: " + folder.getAbsolutePath());
        }
    }

    /**
     * Consolidates files for a student
     *
     * @param studentId     The ID of the student
     * @param files         A list of file paths to consolidate
     */
    private Map<String, StringBuilder> consolidateFilesInMemory(String studentId, List<String> files) {
        Map<String, StringBuilder> consolidatedFiles = new HashMap<>();

        for (String filePath : files) {
            File file = new File(filePath);
            String fileName = file.getName();
            String folderName = file.getParentFile().getName();
            String fileContent = readFileContent(filePath);
            consolidatedFiles.computeIfAbsent(fileName, k -> new StringBuilder()).append(folderName).append("\n").append(fileContent).append("\n\n\n");
        }
        return consolidatedFiles;
    }

    /**
     * Sends an email to a student with consolidated file details.
     *
     * @param studentId         The ID of the student
     * @param email             The email address of the student
     * @param consolidatedFiles A list of file paths to include in the email
     */
    private void sendEmailWithConsolidatedContent(String subject, String studentId, String email, Map<String, StringBuilder> consolidatedFiles) {

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(smtpUser));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(email)
            );
            message.setSubject(subject);

            String body = "Student " + studentId + ":\n\n";

            for (Map.Entry<String, StringBuilder> entry : consolidatedFiles.entrySet()) {
                body += "\n" + entry.getValue() + "\n\n";
            }

            message.setText(body);
            if (body.length() > 30)
                Transport.send(message);

            //System.out.println("Done");

        } catch (MessagingException e) {
            e.printStackTrace();
        }

    }


    private void setEmailConfig()
    {
        String[] emailConfig = readEmailConfig();
        if (emailConfig.length != 3) {
            System.err.println("Invalid email configuration file.");
            return;
        }

        String smtpServer = emailConfig[0];
        smtpUser = emailConfig[1];
        String smtpPassword = emailConfig[2];

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", smtpServer);
        properties.put("mail.smtp.port", "587");
        properties.put("mail.debug", "false");

        session = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(smtpUser, smtpPassword);
                    }
                });

    }

    /**
     * Reads the content of a file.
     *
     * @param filePath The path to the file
     * @return The content of the file as a string
     */
    private String readFileContent(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "Error reading file content.";
        }
    }

    /**
     * Reads email configuration from a file.
     *
     * @return An array containing email configuration values (SMTP server, email, password)
     */
    private String[] readEmailConfig() {
        try (BufferedReader reader = new BufferedReader(new FileReader(emailConfigFile))) {
            return reader.readLine().split(",");
        } catch (IOException e) {
            e.printStackTrace();
            return new String[0];
        }
    }
}
