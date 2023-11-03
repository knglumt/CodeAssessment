package org.assessment;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.regex.Pattern;

public class CodeAssessment {

    private JFrame frame;
    private JTextArea textArea;
    private JFileChooser fileChooser;
    private File defaultFolder;
    private String commentPhrase;
    private Pattern commentPattern;
    private File currentFile;
    private LineNumberArea lineNumberArea;
    private JTextField fileNameLabel;
    private JTextField commentCountField;
    private JRadioButton readOnlyRadioButton; // Radio button for "Read-Only"
    private JRadioButton editableRadioButton; // Radio button for "Editable"
    private ButtonGroup radioButtonGroup; // Button group for radio buttons

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

        // Radio buttons for controlling read-only state
        readOnlyRadioButton = new JRadioButton("Code Segmentation");
        editableRadioButton = new JRadioButton("Code Grading");

        // Add radio buttons to a button group
        radioButtonGroup = new ButtonGroup();
        radioButtonGroup.add(readOnlyRadioButton);
        radioButtonGroup.add(editableRadioButton);
        radioButtonGroup.setSelected(readOnlyRadioButton.getModel(), true); // Set "Read-Only" as default

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

        loadConfiguration();

        textArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (readOnlyRadioButton.isSelected()){
                    if (e.getClickCount() == 2) {
                        insertCommentPhrase();
                        saveFile();
                    }
                }
            }
        });

        readOnlyRadioButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    textArea.setEditable(false); // Set textArea to non-editable
                } else {
                    textArea.setEditable(true);
                }
            }
        });

        frame.setVisible(true);
    }

    private void loadConfiguration() {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
            defaultFolder = new File(properties.getProperty("defaultFolder"));
            commentPhrase = properties.getProperty("commentPhrase");
        } catch (IOException e) {
            commentPhrase = "/***SEPARATOR***/";
            commentPattern = Pattern.compile("/\\*\\*\\*SEPARATOR\\*\\*\\*/");
        }
    }

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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

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
            BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile));
            textArea.write(writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    private void insertCommentPhrase() {
        int caretPosition = textArea.getCaretPosition();
        try {
            int selectedRowIndex = textArea.getLineOfOffset(caretPosition);
            int lineStart = textArea.getLineStartOffset(selectedRowIndex);
            int lineEnd = textArea.getLineEndOffset(selectedRowIndex);

            String line = textArea.getText(lineStart, lineEnd - lineStart);
            String regex = "/\\*\\* ASSESSMENT.*?@grade .*?@feedback .*?\\*/";
            if (line.matches(regex)) {
                // Remove the existing Javadoc comment
                textArea.getDocument().remove(lineStart, lineEnd - lineStart);
            } else {
                String text = "/** ASSESSMENT\n";
                text += " * @grade \n";
                text += " * @feedback \n";
                text += " */\n";
                textArea.getDocument().insertString(lineStart, text, null);
            }
            lineNumberArea.repaint();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void saveAndOpenFile() {
        saveFile();
        openNextFileInFolder();
    }

    private void openNextFileInFolder() {
        if (currentFile != null) {
            File folder = currentFile.getParentFile();
            File[] files = folder.listFiles();

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

    private void openFileWithoutDialog() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(currentFile));
            textArea.read(reader, null);
            reader.close();
            fileNameLabel.setText(currentFile.getName());
            lineNumberArea.repaint();
            //findRefCode();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    private void reopenFile() {
        if (currentFile != null) {
            openFileWithoutDialog();
        }
    }

    private void findRefCode() throws IOException {
        int commentCount = 0;

        String folderPath = String.valueOf(currentFile.getParentFile());
        String refCodeJava = "RefCode.java";
        String refCodeCPP = "RefCode.cpp";

        File folder = new File(folderPath);

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && (file.getName().equals(refCodeJava) || file.getName().equals(refCodeCPP))) {
                        commentCount = countComments(Path.of(file.getPath()), commentPattern);
                        commentCountField.setText("Separator Count: " + commentCount);
                        break;
                    }
                }
            }
        }
        if (commentCount == 0) {
            JOptionPane.showMessageDialog(null, "RefCode.java or RefCode.cpp not found!");
            //System.exit(0);
        }
    }

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new CodeAssessment();
            }
        });
    }

    class LineNumberArea extends JPanel {
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
