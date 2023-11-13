package org.assessment;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;

/**
 * This class represents a Code Assessment tool with a graphical user interface (GUI).
 */
public class CodeAssessment {

    private final JFrame frame;
    private final JTextArea textArea;
    private final JFileChooser fileChooser;
    private final File defaultFolder;
    int commentCount;

    Pattern commentPattern = Pattern.compile("@grade");
    private File currentFile;
    private final LineNumberArea lineNumberArea;
    private final JTextField fileNameLabel;
    private final JTextField commentCountField;
    private final JTextField refCodeField;
    private final JRadioButton readOnlyRadioButton;

    /**
     * Constructor for the CodeAssessment class, sets up the GUI and initializes components.
     */
    public CodeAssessment() {
        frame = new JFrame("Code Assessment");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        textArea = new JTextArea();
        frame.add(new JScrollPane(textArea), BorderLayout.CENTER);
        textArea.setEditable(false);

        lineNumberArea = new LineNumberArea(textArea);
        frame.add(lineNumberArea, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        frame.add(buttonPanel, BorderLayout.NORTH);

        JPanel paramPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        frame.add(paramPanel, BorderLayout.SOUTH);

        fileNameLabel = new JTextField(30);
        fileNameLabel.setEditable(false);
        fileNameLabel.setFont(fileNameLabel.getFont().deriveFont(Font.BOLD));
        fileNameLabel.setForeground(Color.RED);

        commentCountField = new JTextField(30);
        commentCountField.setEditable(false);
        commentCountField.setFont(commentCountField.getFont().deriveFont(Font.BOLD));

        refCodeField = new JTextField(30);
        refCodeField.setEditable(true);
        refCodeField.setText("RefCode.java");

        readOnlyRadioButton = new JRadioButton("Code Segmentation");
        JRadioButton editableRadioButton = new JRadioButton("Code Grading");

        ButtonGroup radioButtonGroup = new ButtonGroup();
        radioButtonGroup.add(readOnlyRadioButton);
        radioButtonGroup.add(editableRadioButton);
        radioButtonGroup.setSelected(readOnlyRadioButton.getModel(), true);

        JButton openButton = new JButton("Open");
        JButton reopenButton = new JButton("Reopen");
        JButton saveAndOpenButton = new JButton("Next >>");
        JButton saveAsButton = new JButton("Save");
        JButton previousButton = new JButton("<< Previous");

        buttonPanel.add(openButton);
        buttonPanel.add(previousButton);
        buttonPanel.add(saveAndOpenButton);
        buttonPanel.add(fileNameLabel);
        buttonPanel.add(commentCountField);
        buttonPanel.add(fileNameLabel);

        paramPanel.add(readOnlyRadioButton);
        paramPanel.add(editableRadioButton);
        paramPanel.add(refCodeField);
        paramPanel.add(commentCountField);

        fileChooser = new JFileChooser();
        defaultFolder = new File(System.getProperty("user.dir"));

        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFile();
            }
        });

        saveAndOpenButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAndOpenFile();
            }
        });

        saveAsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFileAs();
            }
        });

        reopenButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reopenFile();
            }
        });

        previousButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFile();
                openPreviousFileInFolder();
            }
        });

        textArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (readOnlyRadioButton.isSelected()) {
                    if (e.getClickCount() == 2) {
                        insertCommentPhrase();
                        saveFile();
                        paintLabels(currentFile.toPath(), commentPattern);
                    }
                }
            }
        });

        readOnlyRadioButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                textArea.setEditable(e.getStateChange() != ItemEvent.SELECTED);
            }
        });

        frame.setVisible(true);
    }

    /**
     * Opens a file selected by the user and displays its content in the JTextArea.
     */
    private void openFile() {
        fileChooser.setCurrentDirectory(defaultFolder);
        int returnVal = fileChooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            fileNameLabel.setText(currentFile.getName());
            try {
                BufferedReader reader = new BufferedReader(new FileReader(currentFile));
                textArea.read(reader, null);
                reader.close();
                lineNumberArea.repaint();
                findRefCode();
                paintLabels(currentFile.toPath(), commentPattern);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Saves the content of the current file in the JTextArea.
     */
    private void saveFile() {
        if (currentFile == null) {
            fileChooser.setCurrentDirectory(defaultFolder);
            int returnVal = fileChooser.showSaveDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                currentFile = fileChooser.getSelectedFile();
            } else {
                return;
            }
        }

        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(currentFile), StandardCharsets.UTF_8));
            textArea.write(writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the content of the current file in the JTextArea with a new name.
     */
    private void saveFileAs() {
        if (currentFile != null) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile));
                textArea.write(writer);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Inserts a comment phrase into the code where the user double-clicked.
     */
    private void insertCommentPhrase() {
        int caretPosition = textArea.getCaretPosition();
        try {
            int selectedRowIndex = textArea.getLineOfOffset(caretPosition);
            int lineStart = textArea.getLineStartOffset(selectedRowIndex);
            int lineEnd = textArea.getLineEndOffset(selectedRowIndex);

            String line = textArea.getText(lineStart, lineEnd - lineStart);

            if (line.contains("ASSESSMENT") || line.contains("@grade") || line.contains("@feedback") || line.contains("*/")) {
                if (line.contains("ASSESSMENT")) {

                    int lineStartGrade = textArea.getLineStartOffset(selectedRowIndex + 1);
                    int lineEndGrade = textArea.getLineEndOffset(selectedRowIndex + 1);
                    String lineGrade = textArea.getText(lineStartGrade, lineEndGrade - lineStartGrade);
                    boolean deleteComment = true;
                    if (lineGrade.contains("@grade") && lineGrade.trim().length() > 8) {
                        int response = JOptionPane.showConfirmDialog(null,
                                "Grading has been done, deleted anyway?", "Confirmation", JOptionPane.YES_NO_OPTION);
                        if (response == JOptionPane.NO_OPTION) {
                            deleteComment = false;
                        }
                    }

                    if (deleteComment) {
                        for (int i = 0; i < 4; i++) {
                            if (selectedRowIndex < textArea.getLineCount()) {
                                int currentLineStart = textArea.getLineStartOffset(selectedRowIndex);
                                int currentLineEnd = textArea.getLineEndOffset(selectedRowIndex);
                                textArea.getDocument().remove(currentLineStart, currentLineEnd - currentLineStart);
                            }
                        }
                    }

                }
            } else {
                // Insert a new JavaDoc comment
                String text = "/** ASSESSMENT\n";
                text += " * @grade \n";
                text += " * @feedback \n";
                text += " */\n";
                textArea.getDocument().insertString(lineStart, text, null);
            }
            lineNumberArea.repaint();
            saveFile(); // Save the file after removing the comment
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the current file and opens the next file in the same folder.
     */
    private void saveAndOpenFile() {
        saveFile();
        openNextFileInFolder();
    }

    /**
     * Opens the next file in the same folder without a file dialog.
     */
    private void openNextFileInFolder() {
        if (currentFile != null) {
            File folder = currentFile.getParentFile();
            File[] files = folder.listFiles();

            for (int i = 0; i < Objects.requireNonNull(files).length; i++) {
                if (files[i].equals(currentFile)) {
                    if (i < files.length - 1) {
                        currentFile = files[i + 1];
                        openFileWithoutDialog();
                    }
                    break;
                }
            }
        }
    }

    /**
     * Opens the next file in the same folder without a file dialog.
     */
    private void openFileWithoutDialog() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(currentFile));
            textArea.read(reader, null);
            reader.close();
            fileNameLabel.setText(currentFile.getName());
            lineNumberArea.repaint();
            paintLabels(currentFile.toPath(), commentPattern);
            //findRefCode();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens the previous file in the same folder without a file dialog.
     */
    private void openPreviousFileInFolder() {
        if (currentFile != null) {
            File folder = currentFile.getParentFile();
            File[] files = folder.listFiles();

            for (int i = 0; i < files.length; i++) {
                if (files[i].equals(currentFile)) {
                    if (i > 0) {
                        currentFile = files[i - 1];
                        openFileWithoutDialog();
                    }
                    break;
                }
            }
        }
    }

    /**
     * Reopens the current file.
     */
    private void reopenFile() {
        if (currentFile != null) {
            openFileWithoutDialog();
        }
    }

    /**
     * Finds the reference code file and counts the number of comment phrases in it.
     *
     * @throws IOException If an error occurs while searching for the reference code or reading it.
     */
    private void findRefCode() throws IOException {

        String folderPath = String.valueOf(currentFile.getParentFile());
        String refCode = refCodeField.getText();

        commentCount = -1;

        File folder = new File(folderPath);

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();

            if (files != null) {
                Arrays.sort(files, Comparator.comparing(File::getName));
                for (File file : files) {
                    if (file.isFile() && (file.getName().equals(refCode))) {
                        commentCount = countComments(Paths.get((file.getPath())), commentPattern);
                        commentCountField.setText("Number of Segments: " + commentCount);
                        break;
                    }
                }
            }
        }
        if (commentCount == -1) {
            JOptionPane.showMessageDialog(null, refCodeField.getText() + " not found!");
            commentCountField.setText("Number of Segments: Not found!");
        }
    }

    /**
     * Counts the number of comment phrases in a specified file.
     *
     * @param file          The file to count comment phrases in.
     * @param commentPattern The regular expression pattern to match comment phrases.
     * @return The number of comment phrases found in the file.
     */
    private int countComments(Path file, Pattern commentPattern) {
        int commentCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (commentPattern.matcher(line).find()) {
                    commentCount++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return commentCount;
    }

    /**
     * Paints labels with different colors based on the comparison of comment counts.
     *
     * @param file          The file to check comment counts for.
     * @param commentPattern The regular expression pattern to match comment phrases.
     */
    private void paintLabels(Path file, Pattern commentPattern) {
        int fileCommentCount = countComments(file, commentPattern);
        if (fileCommentCount != commentCount) {
            // Set the text color of fileNameLabel and commentCountField to red
            fileNameLabel.setForeground(Color.RED);
            commentCountField.setForeground(Color.RED);
        } else {
            // Reset the text color to the default
            fileNameLabel.setForeground(Color.GREEN);
            commentCountField.setForeground(Color.GREEN);
        }
    }

    /**
     * Main method to start the CodeAssessment application.
     *
     * @param args The command-line arguments.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new CodeAssessment();
            }
        });
    }

    /**
     * A nested class that represents the LineNumberArea for displaying line numbers.
     */
    static class LineNumberArea extends JPanel {
        private final JTextArea textArea;

        public LineNumberArea(JTextArea textArea) {
            this.textArea = textArea;
            setPreferredSize(new Dimension(30, 0));
            setBorder(new MatteBorder(0, 0, 0, 1, Color.GRAY));
            textArea.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    repaint();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    repaint();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Rectangle clip = g.getClipBounds();
            FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
            int fontHeight = fm.getHeight();
            int baseline = fm.getAscent();
            int lineHeight = textArea.getLineCount() * fontHeight;
            for (int i = 0; i <= lineHeight / fontHeight; i++) {
                int y = i * fontHeight + baseline;
                g.drawString(String.valueOf(i + 1), 5, y);
            }
        }
    }
}
