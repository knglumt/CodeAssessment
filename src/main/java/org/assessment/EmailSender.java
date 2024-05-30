package org.assessment;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.swing.*;

/**
 * This class is designed to send emails to a list of students based on data provided through various files.
 * It reads student data, scans for relevant files, and sends emails with those files attached.
 */
public class EmailSender {

    private final String textFilePath;
    private final String folderPath;
    private final String emailConfigFile;
    private Session session;
    private String smtpUser;
    private int mailCount;

    /**
     * Constructs an EmailSender with specified file paths for student data, folder containing relevant files,
     * and email configuration.
     *
     * @param textFilePath    Path to the text file containing student data.
     * @param folderPath      Path to the folder containing student and reference code files.
     * @param emailConfigFile Path to the file containing email server configuration.
     */
    public EmailSender(String textFilePath, String folderPath, String emailConfigFile) {
        this.textFilePath = textFilePath;
        this.folderPath = folderPath;
        this.emailConfigFile = emailConfigFile;
    }

    /**
     * Starts the process of reading student data, setting email configuration, and sending emails.
     *
     * @param subject The subject line for the emails to be sent.
     */
    public void run(String subject) {
        List<String[]> studentData = readStudentData();
        setEmailConfig();
        mailCount = 0;

        for (String[] student : studentData) {
            String studentId = student[0];
            String email = student[1];

            Map<String, StringBuilder> allFiles = scanAndConsolidate(studentId);
            Map<String, StringBuilder> refCodeFiles = new HashMap<>(allFiles);
            allFiles.keySet().removeIf(key -> key.toLowerCase().contains("refcode"));
            refCodeFiles.keySet().removeIf(key -> !key.toLowerCase().contains("refcode"));

            //System.out.println("Sending: " + studentId );
            //System.out.println("Mail: " + email );

            sendEmail(subject + "-Student Codes", studentId, email, allFiles, " ***STUDENT CODES***");
            sendEmail(subject + "-Reference Codes", "***INSTRUCTOR", email, refCodeFiles, " REFERENCE CODES***");
        }

        JOptionPane.showMessageDialog(null,  mailCount + " emails have been sent to students!");
    }

    /**
     * Reads student data from a specified file and returns a list of student IDs and emails.
     *
     * @return A list of string arrays, each containing a student ID and email.
     */
    private List<String[]> readStudentData() {
        List<String[]> studentData = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(textFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                studentData.add(line.split(","));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return studentData;
    }

    /**
     * Scans a specified folder recursively and consolidates files that contain either the student ID or "refcode".
     *
     * @param studentId The student ID to search for within file names.
     * @return A map containing file names as keys and their content as values.
     */
    private Map<String, StringBuilder> scanAndConsolidate(String studentId) {
        Map<String, StringBuilder> consolidatedFiles = new HashMap<>();
        File folder = new File(folderPath);
        scanFolderRecursively(folder, studentId, consolidatedFiles);
        return consolidatedFiles;
    }

    /**
     * Helper method to recursively scan a folder and accumulate files relevant to a specific student ID or refcode.
     *
     * @param folder The folder to scan.
     * @param studentId The student ID to filter files by.
     * @param consolidatedFiles A map to accumulate file names and content.
     */
    private void scanFolderRecursively(File folder, String studentId, Map<String, StringBuilder> consolidatedFiles) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    scanFolderRecursively(file, studentId, consolidatedFiles);
                } else if (file.isFile() && (file.getName().toLowerCase().contains(studentId.toLowerCase()) || file.getName().toLowerCase().contains("refcode"))) {
                    String fileName = file.getName();
                    String folderName = file.getParentFile().getName();
                    String fileContent = readFileContent(file.getPath());
                    consolidatedFiles.computeIfAbsent(fileName, k -> new StringBuilder())
                            .append(folderName).append("\n").append(fileContent).append("\n\n\n");
                }
            }
        } else {
            System.err.println("Error listing files in the folder: " + folder.getAbsolutePath());
        }
    }

    /**
     * Sends an email to a specified recipient with a set of files as the email body.
     *
     * @param subject     The subject of the email.
     * @param studentId   The student ID to include in the email body.
     * @param email       The recipient's email address.
     * @param files       The files to be included in the email body.
     * @param description A description to prepend to the file contents in the email body.
     */
    private void sendEmail(String subject, String studentId, String email, Map<String, StringBuilder> files, String description) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(smtpUser));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject(subject);

            StringBuilder body = new StringBuilder(studentId).append(description).append(":\n\n");
            files.forEach((fileName, content) -> body.append(fileName).append("\n").append(content).append("\n\n"));
            message.setText(body.toString());

            if (!files.isEmpty()) {
                //System.out.println("Sent :" + studentId );
                Transport.send(message);
                if (subject.toLowerCase().contains("student"))
                    mailCount++;
            }
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Configures the email session using details from the email configuration file.
     */
    private void setEmailConfig() {
        String[] emailConfig = readEmailConfig();
        if (emailConfig.length != 3) {
            System.err.println("Invalid email configuration file.");
            return;
        }

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", emailConfig[0]);
        properties.put("mail.smtp.port", "587");
        properties.put("mail.debug", "false");

        session = Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailConfig[1], emailConfig[2]);
            }
        });
        smtpUser = emailConfig[1];
    }

    /**
     * Reads the content of a file and returns it as a string.
     *
     * @param filePath The path to the file to read.
     * @return The content of the file as a string.
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
     * Reads the email configuration from a file.
     *
     * @return An array of strings containing the SMTP host, user, and password.
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
