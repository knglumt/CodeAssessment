# Code Assessment Tool

## Overview

The Code Assessment Tool is a user-friendly Java application designed for assessing and grading code files. This tool provides a set of features to make code evaluation, navigation, and result management easier.

## Features

### 1. User-Friendly Interface

- The application has an intuitive interface for a seamless user experience.
- The main window displays code in an editable text area, accompanied by a line number area for easy navigation.
- Various buttons and options are provided for simple interaction.

### 2. File Operations

- **Open Code Files:**
  - Open code files effortlessly using the file chooser.
  - The tool keeps track of the current file, displaying its content in the text area.

- **Save and Open Next:**
  - Save changes and move to the next file in the same folder seamlessly.

- **Navigate Files:**
  - Easily navigate to the previous file in the same folder.

### 3. Code Grading

- **Insert Comment Phrases:**
  - Double-click to insert comment phrases for assessment and grading.
  - The tool automatically counts the number of comment phrases in the code for grading.

- **Read-Only and Editable Modes:**
  - Choose between read-only and editable modes for different tasks.

- **Undo Changes:**
  - Use the Ctrl+Z shortcut to undo changes and revert the text area to its previous state.

### 4. Export Results to CSV

- Export assessment results to a CSV file effortlessly.
- The tool processes folders and generates CSV files based on the current working directory.

### 5. Send Assessment Details via Email

- Send assessment details to all students with a simple click.
- The tool utilizes the EmailSender class to accomplish this task.

### 6. Assessment Feedback

- **Tree View Display:**
  - View a tree view of assessment comments for a structured overview.
  - Select a comment in the tree view to view details and make changes.

- **Use Selected Grade:**
  - Apply the selected grade from the comments tree to update the code.

- **Grade Modification:**
  - Double-click on the feedback on the tree view to modify grading information in a user-friendly manner.

### 7. Line Number Area Enhancements

- **Color-Coded Labels:**
  - Line number labels are color-coded to provide visual feedback on comment counts.

### 8. Grade Insertion

- **Insert Grades and Feedback:**
  - Add grades and feedback at specific positions in the code for an improved grading process.

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
- Use the tree view to navigate assessment comments and apply grades.
- Modify grades and feedback by double-clicking on `ASSESSMENT` or `@grade` phrases.

## Contributing

Contributions to the Code Assessment Tool are welcome. Feel free to open issues, submit feature requests, or contribute to the codebase.

## License

This Code Assessment Tool is licensed under the [MIT License](LICENSE). Feel free to use, modify, and distribute the code according to the terms of the license.
