package org.assessment;

import org.assessment.tool.JMultiLineToolTip;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Represents a FeedbackTree to build and display a JTree with feedback comments.
 */
public class FeedbackTree extends JFrame {

    private JTree commentsTree;
    private final String path;
    private Popup currentPopup = null;

    /**
     * Constructs a FeedbackTree with the specified path.
     *
     * @param path The path to the folder containing feedback files.
     */
    public FeedbackTree(String path) {
        super("Feedback Tree");

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

        TreeMap<String, DefaultMutableTreeNode> sortedGradeNodes = new TreeMap<>(gradeNodes);

        sortedGradeNodes.forEach((grade, feedbackNode) -> {
            root.add(feedbackNode);
        });

        commentsTree = new JTree(root);

        return commentsTree;
    }

    /**
     * Finds the first code snippet in the current folder that contains the specified feedback comment.
     *
     * @param feedback The feedback comment.
     * @return The code snippet containing the feedback comment, or null if not found.
     */
    public String findCodeSnippet(String feedback) {
        try {
            List<Path> files = Files.walk(Paths.get(path))
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());

            for (Path file : files) {
                List<String> lines = Files.readAllLines(file);
                boolean foundFeedback = false;
                StringBuilder snippetBuilder = new StringBuilder();

                for (String line : lines) {
                    if (foundFeedback && !line.contains("@grade")) {
                        snippetBuilder.append(line).append(" \n ");
                    } else if (!foundFeedback && line.contains(feedback)) {
                        foundFeedback = true;
                        snippetBuilder.append(line).append(" \n ");
                    } else if (foundFeedback && line.contains("@grade")) {
                        //snippetBuilder.append(line).append(" \n ");
                        return snippetBuilder.toString();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Show tooltip with code snippet.
     *
     * @param tree      The JTree.
     * @param x         X coordinate.
     * @param y         Y coordinate.
     * @param codeSnippet  Code snippet.
     */
    public void showFeedbackCodeSnippetTooltip(JComponent tree, int x, int y, String codeSnippet) {
        closeCurrentPopup();

        JToolTip tooltip = new JMultiLineToolTip();
        tooltip.setTipText(codeSnippet);

        Point location = tree.getLocationOnScreen();
        tooltip.setLocation(location.x + x, location.y + y);

        PopupFactory popupFactory = PopupFactory.getSharedInstance();
        currentPopup = popupFactory.getPopup(tree, tooltip, location.x + x, location.y + y);
        currentPopup.show();
    }


    /**
     * Closes the current tooltip popup if it exists.
     */
    public void closeCurrentPopup() {
        if (currentPopup != null) {
            currentPopup.hide(); ;
            currentPopup = null;
        }
    }

    /**
     * Processes a feedback file to extract grade and feedback information.
     *
     * @param file        The feedback file to process.
     * @param splitID     The split point identifier.
     * @param gradeNodes  A map containing grade nodes for building the JTree.
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
                                DefaultMutableTreeNode defaultNode = new DefaultMutableTreeNode(gradeComment);
                                gradeNodes.put(gradeComment, defaultNode);
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
