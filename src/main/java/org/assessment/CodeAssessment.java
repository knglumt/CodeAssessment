package org.assessment;

import org.assessment.codesplitter.CodeSplitter;
import org.assessment.codesplitter.LineCalculator;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;



/**
 * This class represents a Code Assessment tool with a graphical user interface (GUI).
 */
public class CodeAssessment {

    private final JFrame frame;
    private final JTextArea textArea;
    private Stack<String> contentStack = new Stack<>();
    private final JFileChooser fileChooser;
    private final File defaultFolder;
    private boolean unsavedChanges = false;
    private int commentCount;
    private static int fileCommentCount;

    Pattern commentPattern = Pattern.compile("@grade");

    private static String refCodeFile;
    private static File currentFile;
    private final LineNumberArea lineNumberArea;
    private final JTextField fileNameLabel;
    private final JTextField commentCountField;
    private final JTextField refCodeField;
    private final JRadioButton readOnlyRadioButton;
    private static boolean hasTextAfterGrade;
    private int currentLineCount;

    private int startIndexofAssesment;
    private FeedbackTree feedbackTree;
    private final JTree commentsTree;

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

        JPanel feedbackPanel = new JPanel(new BorderLayout());
        frame.add(feedbackPanel, BorderLayout.EAST);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(textArea), feedbackPanel);
        splitPane.setResizeWeight(1.0);
        splitPane.setDividerLocation(frame.getWidth() - 300);
        frame.add(splitPane, BorderLayout.CENTER);

        fileNameLabel = new JTextField(30);
        fileNameLabel.setEditable(false);
        fileNameLabel.setFont(fileNameLabel.getFont().deriveFont(Font.BOLD));
        fileNameLabel.setForeground(Color.RED);

        commentCountField = new JTextField(30);
        commentCountField.setEditable(false);
        commentCountField.setFont(commentCountField.getFont().deriveFont(Font.BOLD));

        refCodeField = new JTextField(30);
        refCodeField.setEditable(true);
        refCodeField.setText("RefCode");

        readOnlyRadioButton = new JRadioButton("Code Segmentation");
        JRadioButton editableRadioButton = new JRadioButton("Code Grading");

        ButtonGroup radioButtonGroup = new ButtonGroup();
        radioButtonGroup.add(readOnlyRadioButton);
        radioButtonGroup.add(editableRadioButton);
        radioButtonGroup.setSelected(readOnlyRadioButton.getModel(), true);

        JButton openButton = new JButton("Open");
        JButton saveAndOpenButton = new JButton("Next >>");
        JButton previousButton = new JButton("<< Previous");
        JButton exportCSVButton = new JButton("Export CSV");
        JButton mailButton = new JButton("Send Mails");

        commentsTree = new JTree();
        commentsTree.setVisible(false);

        buttonPanel.add(openButton);
        buttonPanel.add(previousButton);
        buttonPanel.add(saveAndOpenButton);
        buttonPanel.add(exportCSVButton);
        buttonPanel.add(mailButton);
        buttonPanel.add(fileNameLabel);
        buttonPanel.add(commentCountField);
        buttonPanel.add(fileNameLabel);

        paramPanel.add(readOnlyRadioButton);
        paramPanel.add(editableRadioButton);
        paramPanel.add(refCodeField);
        paramPanel.add(commentCountField);

        feedbackPanel.add(new JScrollPane(commentsTree), BorderLayout.CENTER);

        fileChooser = new JFileChooser();
        defaultFolder = new File(System.getProperty("user.dir"));

        //createTreeViewPopupMenu();

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
                feedbackTree.closeCurrentPopup();
                int offset = textArea.viewToModel(e.getPoint());
                if (e.getClickCount() == 2) {
                    if (readOnlyRadioButton.isSelected()) {
                        insertCommentPhrase();
                        saveFile();
                        paintLabels(currentFile.toPath(), commentPattern);
                    } else {
                        setFeedbackTree(offset);
                    }
                }
            }
        });

        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {

                unsavedChanges = true;

                if (((e.getKeyCode() == KeyEvent.VK_Z) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0))
                        || ((e.getKeyCode() == KeyEvent.VK_Z) && ((e.getModifiers() & KeyEvent.META_MASK) != 0))) {
                    undo();
                }
                else if (!(((e.getModifiers() & KeyEvent.META_MASK) != 0) || ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0))) {
                    contentStack.push(textArea.getText());
                    paintLabels(currentFile.toPath(), commentPattern);
                }
            }
        });

        readOnlyRadioButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                textArea.setEditable(e.getStateChange() != ItemEvent.SELECTED);
            }
        });

        exportCSVButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportCSV();
            }
        });

        commentsTree.addMouseListener(new MouseAdapter() {
                                          @Override
                                          public void mouseClicked(MouseEvent e) {
                                              if (e.getClickCount() == 2) {
                                                  TreePath selectedPath = commentsTree.getPathForLocation(e.getX(), e.getY());
                                                  if (selectedPath != null) {
                                                      DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
                                                      if (selectedNode != null) {
                                                          useThisGrade();
                                                      }
                                                  }
                                              }
                                          }
                                      });

        commentsTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JTree tree = (JTree) e.getSource();
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (node != null && node.getUserObject() != null) {
                        String feedback = node.getUserObject().toString();
                        if (!feedback.isEmpty()) {
                            String codeSnippet = feedbackTree.findCodeSnippet(feedback);
                            if (codeSnippet != null) {
                                feedbackTree.showFeedbackCodeSnippetTooltip(tree, e.getX(), e.getY(), codeSnippet);
                            }
                        }
                    }
                }
            }
        });

        mailButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMail();
            }
        });


        frame.setVisible(true);
    }

    private void sendMail() {

        String subject = JOptionPane.showInputDialog(null, "Enter the exam name:", "Email Subject", JOptionPane.QUESTION_MESSAGE );
        if (subject != null && !subject.trim().isEmpty()) {
            int response = JOptionPane.showConfirmDialog(null,
                    "Are you sure to email assessment details to all students?", "Confirmation", JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) {
                EmailSender program = new EmailSender(
                        "student_mails.txt",
                        defaultFolder.getPath(),
                        "app_config.txt"
                );
                program.run(subject);
            }
        }
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
                currentLineCount = textArea.getLineCount();
                lineNumberArea.repaint();
                findRefCode();
                paintLabels(currentFile.toPath(), commentPattern);
                contentStack.clear();
                contentStack.push(textArea.getText());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Saves the content of the current file in the JTextArea.
     */
    private void saveFile() {
        feedbackTree.closeCurrentPopup();
        if (currentLineCount != textArea.getLineCount() ) {
            unsavedChanges = true;
        }
        if (unsavedChanges) {
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
            unsavedChanges = false;
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
                else if (line.contains("@grade")) {
                    // Double-clicked on @grade, add grade zero
                    String newLine = line.replace("@grade", "@grade 0");
                    textArea.getDocument().remove(lineStart, lineEnd - lineStart);
                    textArea.getDocument().insertString(lineStart, newLine, null);
                }
            }
            else {
                // Insert a new JavaDoc comment
                String text = "/** ASSESSMENT\n";
                text += " * @grade \n";
                text += " * @feedback \n";
                text += " */\n";
                textArea.getDocument().insertString(lineStart, text, null);
            }
            lineNumberArea.repaint();
            unsavedChanges = true;
            saveFile();
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
            File[] files = folder.listFiles(getAllFileTypesFilter());

            if (files != null) {
                Arrays.sort(files, Comparator.comparing(File::getName));

                for (int i = 0; i < files.length; i++) {
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
    }

    /**
     * Opens the previous file in the same folder without a file dialog.
     */
    private void openPreviousFileInFolder() {
        if (currentFile != null) {
            File folder = currentFile.getParentFile();
            File[] files = folder.listFiles(getAllFileTypesFilter());

            if (files != null) {
                Arrays.sort(files, Comparator.comparing(File::getName));

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
    }

    private void undo() {
        if (!contentStack.isEmpty()) {
            String previousContent = contentStack.pop();
            textArea.setText(previousContent);
            lineNumberArea.repaint();
            unsavedChanges = true;
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
            currentLineCount = textArea.getLineCount();
            lineNumberArea.repaint();
            paintLabels(currentFile.toPath(), commentPattern);
            contentStack.clear();
            contentStack.push(textArea.getText());
            //findRefCode();
            setFeedbackTree(0);
        } catch (IOException e) {
            e.printStackTrace();
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
        hasTextAfterGrade = true;
        try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = commentPattern.matcher(line);
                if (matcher.find()) {
                    commentCount++;
                    int commentIndex = matcher.end();
                    if (commentIndex >= line.length() - 1) {
                        hasTextAfterGrade = false;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return commentCount;
    }

    /**
     * Helper method to get a file filter for Java and text files.
     */
    public static FileFilter getAllFileTypesFilter() {
        return new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() || file.getName().toLowerCase().endsWith(".java") || file.getName().toLowerCase().endsWith(".txt")
                        || file.getName().toLowerCase().endsWith(".c") || file.getName().toLowerCase().endsWith(".cpp")
                        || file.getName().toLowerCase().endsWith(".py")
                        // Add more file extensions as needed for other programming languages
                        ;
            }
        };
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
                    if (file.isFile() && (file.getName().contains(refCode))) {
                        refCodeFile = file.getPath();
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
        } else {
            feedbackTree = new FeedbackTree(folderPath);
            setFeedbackTree(0);
            commentsTree.setVisible(true);
        }

    }

    /**
     * Paints labels with different colors based on the comparison of comment counts.
     *
     * @param file          The file to check comment counts for.
     * @param commentPattern The regular expression pattern to match comment phrases.
     */
    private void paintLabels(Path file, Pattern commentPattern) {
        fileCommentCount = countComments(file, commentPattern);
        if (fileCommentCount != commentCount) {
            fileNameLabel.setForeground(Color.RED);
            commentCountField.setForeground(Color.RED);
        } else {
            fileNameLabel.setForeground(Color.BLUE);
            commentCountField.setForeground(Color.BLUE);
            if (hasTextAfterGrade) {
                fileNameLabel.setForeground(Color.GREEN);
                commentCountField.setForeground(Color.GREEN);
            }
        }

    }

    /**
     * Invokes the CSVExporter to export CSV based on the current working directory.
     */
    private void exportCSV() {

        CSVExporter csvExporter = new CSVExporter();
        String rootFolder = System.getProperty("user.dir");
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String outputCsv = "Grades_" + timeStamp + ".csv";

        Map<String, Map<String, Map<String, Integer>>> results = csvExporter.processFolder(rootFolder, "student_mails.txt");
        csvExporter.generateCsv(results, outputCsv);

        JOptionPane.showMessageDialog(null, "CSV file generated successfully!");
    }

    /**
     * Finds the order number of the assessment based on the caret position.
     *
     * @param offset The caret position in the text area.
     * @return The order number of the assessment.
     */
    private int findAssessmentOrderNumber(int offset) {
        String text = textArea.getText();
        startIndexofAssesment = text.lastIndexOf("ASSESSMENT", offset);
        if (startIndexofAssesment != -1) {
            return countAssessmentOccurrences(text, startIndexofAssesment);
            } else
                return 0;
    }

    /**
     * Counts the occurrences of the "ASSESSMENT" keyword up to the specified end index.
     *
     * @param text     The text to search for occurrences.
     * @param endIndex The end index for searching occurrences.
     * @return The number of occurrences of the "ASSESSMENT" keyword.
     */
    private int countAssessmentOccurrences(String text, int endIndex) {
        String substring = text.substring(0, endIndex);
        String[] words = substring.split("\\s+");
        int count = 1;

        for (String word : words) {
            if (word.equals("ASSESSMENT")) {
                count++;
            }
        }
        return count;
    }

    /**
     * Uses the selected grade from the comments tree and updates the code accordingly.
     */
    private void useThisGrade() {
        if (startIndexofAssesment != -1) {
            TreePath selectedPath = commentsTree.getSelectionPath();
            if (selectedPath != null) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
                if (selectedNode != null) {
                    removeAndInsertGradeAndFeedback(startIndexofAssesment, selectedNode);
                }
            }
        }
    }

    private void setFeedbackTree(int offset){
        TreeModel treeModel = feedbackTree.buildTreeModel(findAssessmentOrderNumber(offset)).getModel();
        commentsTree.setModel(treeModel);
        expandAllNodes(new TreePath(treeModel.getRoot()), commentsTree);
    }

    private void expandAllNodes(TreePath parent, JTree tree) {
        // Traverse the tree recursively and expand each node
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        for (int i = 0; i < node.getChildCount(); i++) {
            TreeNode child = node.getChildAt(i);
            TreePath path = parent.pathByAddingChild(child);
            expandAllNodes(path, tree);
        }

        // Expand the current node
        tree.expandPath(parent);
    }
    /**
     * Removes the existing JavaDoc block and inserts the grade and feedback at the specified position.
     *
     * @param startIndex    The starting index for removing and inserting.
     * @param selectedNode  The selected node from the comments tree.
     */
    private void removeAndInsertGradeAndFeedback(int startIndex, DefaultMutableTreeNode selectedNode) {
        try {
            // Find the start and end of the existing JavaDoc block
            int commentStart = textArea.getText().lastIndexOf("/**", startIndex);
            int commentEnd = textArea.getText().indexOf("*/", commentStart) + 2;

            // Remove the existing JavaDoc block
            textArea.getDocument().remove(commentStart, commentEnd - commentStart);

            // Insert the new JavaDoc block at the position of the specified assessment number
            insertGradeAndFeedback(startIndex - 4, selectedNode);

            unsavedChanges = true;
            saveFile();

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Inserts the grade and feedback at the specified position in the code.
     *
     * @param startIndex    The starting index for inserting.
     * @param commentNode   The selected node from the comments tree.
     */
    private void insertGradeAndFeedback(int startIndex, DefaultMutableTreeNode commentNode) {
        String feedback = "@feedback " + commentNode.toString();
        String grade = "@grade " + commentNode.getParent().toString();
        String newText = String.format("/** ASSESSMENT\n * %s\n * %s\n */", grade, feedback);

        try {
            // Insert the new JavaDoc block at the position of the specified assessment number
            textArea.getDocument().insertString(startIndex, newText, null);
            lineNumberArea.repaint();
        } catch (BadLocationException e) {
            e.printStackTrace();
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

            ArrayList<ArrayList<Integer>> lists = null;

            if ((fileCommentCount < 1) && (currentFile != null)) {
                try {
                    lists = CodeSplitter.calculateBestSplitsforFile(currentFile, new LineCalculator(new File(refCodeFile)));
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }

            for (int i = 0; i <= lineHeight / fontHeight; i++) {
                int y = i * fontHeight + baseline;
                String lineText = getLineText(i);

                if (lineText != null && lineText.contains("ASSESSMENT")) {
                    g.setColor(Color.RED);
                    g.setFont(g.getFont().deriveFont(Font.BOLD));
                } else if ((fileCommentCount < 1 ) && (lists != null) && (lists.get(0).contains(i))) {
                    g.setColor(Color.RED);
                    g.setFont(g.getFont().deriveFont(Font.BOLD));
                } else {
                    g.setColor(Color.BLACK);
                    g.setFont(g.getFont().deriveFont(Font.PLAIN));
                }

                g.drawString(String.valueOf(i + 1), 5, y);
            }
            textArea.repaint();
        }

        private String getLineText(int lineIndex) {
            try {
                int startOffset = textArea.getLineStartOffset(lineIndex);
                int endOffset = textArea.getLineEndOffset(lineIndex);
                return textArea.getText(startOffset, endOffset - startOffset);
            } catch (BadLocationException e) {
                return null;
            }
        }
    }
}
