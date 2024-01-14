package org.assessment;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a FeedbackTree to build and display a JTree with feedback comments.
 */
public class FeedbackTree extends JFrame {

    private JTree commentsTree;
    private String path;

    /**
     * Constructs a FeedbackTree with the specified path.
     *
     * @param path The path to the folder containing feedback files.
     */
    public FeedbackTree(String path) {
        super("Feedback Tree");

        // Set up the frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 400);

        this.path = path;
    }

    /**
     * Builds a JTree model based on feedback files in the specified folder.
     *
     * @param splitID The split point identifier.
     * @return The constructed JTree.
     */
    public JTree buildTreeModel(int splitID) {
        String folderPath = path;
        Map<String, DefaultMutableTreeNode> gradeNodes = new HashMap<>();

        try {
            Files.walk(Paths.get(folderPath))
                    .filter(Files::isRegularFile)
                    .forEach(file -> processFile(file, splitID, gradeNodes));

        } catch (IOException e) {
            e.printStackTrace();
        }

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("FEEDBACKS - Split Point " + splitID);

        gradeNodes.forEach((grade, feedbackNode) -> {
            root.add(feedbackNode);
        });

        return new JTree(root);
    }

    /**
     * Processes a feedback file to extract grade and feedback information.
     *
     * @param file      The feedback file to process.
     * @param splitID   The split point identifier.
     * @param gradeNodes A map containing grade nodes for building the JTree.
     */
    private void processFile(Path file, int splitID, Map<String, DefaultMutableTreeNode> gradeNodes) {
        try {
            List<String> lines = Files.readAllLines(file);
            int commentCount = 0;

            for (int i = 0; i < lines.size() - 1; i++) {
                String line = lines.get(i).trim();

                if (line.contains("@grade")) {
                    commentCount++;

                    if (commentCount == splitID) {
                        String gradeComment = line.substring("@grade".length() + 2).trim();
                        String feedbackComment = lines.get(i + 1).substring("@feedback".length() + 3).trim();

                        if (!feedbackComment.trim().isEmpty()) {
                            DefaultMutableTreeNode feedbackNode = new DefaultMutableTreeNode(feedbackComment);

                            if (gradeNodes.containsKey(gradeComment)) {
                                DefaultMutableTreeNode gradeNode = gradeNodes.get(gradeComment);

                                // Check if a child node with the same content already exists
                                boolean nodeExists = false;
                                for (int j = 0; j < gradeNode.getChildCount(); j++) {
                                    DefaultMutableTreeNode existingChild = (DefaultMutableTreeNode) gradeNode.getChildAt(j);
                                    if (existingChild.getUserObject().equals(feedbackNode.getUserObject())) {
                                        nodeExists = true;
                                        break;
                                    }
                                }

                                // If not, add the new feedback node
                                if (!nodeExists) {
                                    gradeNode.add(feedbackNode);
                                }
                            } else {
                                DefaultMutableTreeNode defaulNode = new DefaultMutableTreeNode(gradeComment);
                                gradeNodes.put(gradeComment, defaulNode);
                                gradeNodes.get(gradeComment).add(feedbackNode);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
