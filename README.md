# Code Assessment Tool

## Overview

The Code Assessment Tool is a Java application with a graphical user interface (GUI) designed for assessing and grading code files. This tool provides a set of features to facilitate code evaluation, navigation, and result management.

## Features

### 1. Graphical User Interface (GUI)

- The application is built using Swing, providing an intuitive GUI for users.
- The main window (`JFrame`) contains a text area (`JTextArea`) for displaying and editing code, accompanied by a line number area for easier navigation.
- Various buttons, radio buttons, and text fields are provided for user interaction.

### 2. File Operations

- **Open File:**
    - Users can open code files using a file chooser.
    - The application keeps track of the current file, displaying its content in the text area.

- **Save and Open Next:**
    - Users can save the changes made to the current file and automatically open the next file in the same folder.

- **Previous File:**
    - Users can navigate to the previous file in the same folder.

### 3. Code Grading

- **Comment Phrases:**
    - Users can double-click to insert comment phrases related to assessment and grading.
    - The tool counts the number of comment phrases in the code for grading purposes.

- **Modes:**
    - The application supports both read-only and editable modes, allowing users to choose the appropriate mode for their tasks.

### 4. Undo Feature

- Users can undo changes using the Ctrl+Z shortcut, reverting the text area to its previous state.

### 5. CSV Export

- The application can export assessment results to a CSV file.
- It uses a `CSVExporter` class for processing folders and generating CSV files based on the current working directory.

### 6. Email Sending

- Users can send assessment details to all students via email.
- The tool uses the `EmailSender` class to accomplish this task.

## Getting Started

To run the Code Assessment Tool, follow these steps:

1. Ensure you have Java installed on your system.
2. Compile the Java file: `javac CodeAssessment.java`.
3. Run the application: `java CodeAssessment` or `java -jar CodeAssessment.jar`.

## Usage

- Open a code file using the "Open" button.
- Navigate through files using the "Next," "Previous," and "Save and Open Next" buttons.
- Edit code in either read-only or editable mode.
- Double-click to insert comment phrases related to assessment and grading.
- Utilize the undo feature with Ctrl+Z.
- Export assessment results to a CSV file using the "Export CSV" button.
- Send assessment details via email using the "Send Mails" button.

## Contributing

Contributions to the Code Assessment Tool are welcome. Feel free to open issues, submit feature requests, or contribute to the codebase.

## License

This Code Assessment Tool is licensed under the [MIT License](LICENSE). Feel free to use, modify, and distribute the code according to the terms of the license.
