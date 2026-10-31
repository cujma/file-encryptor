
package gui;

import cipher.SaveMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public class MainView extends VBox {

    private final FileOperationHandler handler;

    private final VBox formView = new VBox(10);
    private final VBox progressView = new VBox(16);

    private final ToggleGroup operationGroup = new ToggleGroup();
    private final RadioButton encryptButton = new RadioButton("Encrypt");
    private final RadioButton decryptButton = new RadioButton("Decrypt");

    private final TextField inputField = new TextField();
    private final TextField outputDir = new TextField();

    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmPassword = new PasswordField();
    private final TextField shownPassword = new TextField();
    private final TextField shownConfirmPassword = new TextField();

    private final CheckBox showPassword = new CheckBox("Show password");

    private final ToggleGroup saveGroup = new ToggleGroup();
    private final RadioButton replaceOriginal = new RadioButton("Replace original file");
    private final RadioButton saveNew = new RadioButton("Save as a new file");

    private final ProgressBar progressBar = new ProgressBar();
    private final Label progressTitle = new Label("Processing file...");
    private final Label status = new Label("Preparing operation...");
    private final TextArea messagesArea = new TextArea();

    private final Button actionButton = new Button("Encrypt file");
    private final Button backButton = new Button("Back to file selection");

    private Button browseDirectoryButton;
    private Button browseInputFolderButton;

    public MainView(FileOperationHandler handler) {
        this.handler = handler;

        getStyleClass().add("main-view");
        setPadding(new Insets(18, 20, 4, 20));
        setAlignment(Pos.TOP_CENTER);
        setFillWidth(true);

        buildFormView();
        buildProgressView();

        getChildren().setAll(formView, progressView);
    }

    private void buildFormView() {
        formView.setAlignment(Pos.TOP_CENTER);
        formView.setFillWidth(true);

        VBox heading = heading("Protect your data", "");
        HBox operationSelector = operationSelector();
        VBox fileSection = fileSection();
        VBox saveSection = saveSection();
        VBox passwordSection = passwordSection();

        actionButton.getStyleClass().add("primary-button");
        actionButton.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(actionButton, new Insets(8, 0, 2, 0));
        actionButton.setOnAction(event -> performOperation());

        addListeners();

        formView.getChildren().setAll(
                heading,
                operationSelector,
                fileSection,
                saveSection,
                passwordSection,
                actionButton
        );

        updateOperationMode();
        updateSaveMode();
    }

    private void buildProgressView() {
        progressView.setAlignment(Pos.TOP_CENTER);
        progressView.setFillWidth(true);
        progressView.setVisible(false);
        progressView.setManaged(false);

        VBox heading = heading("Protect your data", "Your operation is in progress");

        progressTitle.getStyleClass().add("section-title");
        status.getStyleClass().add("status-label");
        status.setWrapText(true);

        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(14);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        messagesArea.setEditable(false);
        messagesArea.setWrapText(true);
        messagesArea.setPromptText("Errors and messages will appear here...");
        messagesArea.setPrefRowCount(10);
        messagesArea.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(messagesArea, Priority.ALWAYS);

        Label messagesTitle = new Label("Errors and messages");
        messagesTitle.getStyleClass().add("section-title");

        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setDisable(true);
        backButton.setOnAction(event -> showForm());

        progressView.getChildren().setAll(
                heading,
                progressTitle,
                progressBar,
                status,
                messagesTitle,
                messagesArea,
                backButton
        );

        VBox.setVgrow(messagesArea, Priority.ALWAYS);
        getChildren().add(progressView);
    }

    private VBox heading(String titleText, String subtitleText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("title");

        VBox heading = new VBox(6);
        heading.getChildren().add(title);
        if (!subtitleText.isBlank()) {
            Label subtitle = new Label(subtitleText);
            subtitle.getStyleClass().add("subtitle");
            heading.getChildren().add(subtitle);
        }
        heading.setAlignment(Pos.CENTER);
        return heading;
    }

    private HBox operationSelector() {
        encryptButton.setToggleGroup(operationGroup);
        decryptButton.setToggleGroup(operationGroup);
        encryptButton.setSelected(true);

        HBox selector = new HBox(28, encryptButton, decryptButton);
        selector.getStyleClass().add("form-section");
        selector.setAlignment(Pos.CENTER);
        selector.setMaxWidth(Double.MAX_VALUE);
        return selector;
    }

    private VBox fileSection() {
        inputField.setEditable(false);
        inputField.setPromptText("Choose a file...");

        Button browse = new Button("File");
        browse.setOnAction(event -> chooseInputFile());

        browseInputFolderButton = new Button("Folder");
        browseInputFolderButton.setOnAction(event -> chooseInputDirectory());

        outputDir.setEditable(false);
        outputDir.setPromptText("Output directory...");

        browseDirectoryButton = new Button("Browse");
        browseDirectoryButton.setOnAction(event -> chooseOutputDirectory());

        HBox inputRow = new HBox(8, inputField, browse, browseInputFolderButton);
        inputRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        HBox outputRow = new HBox(8, outputDir, browseDirectoryButton);
        outputRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(outputDir, Priority.ALWAYS);

        VBox inputGroup = fieldGroup("Input file", inputRow);
        VBox outputGroup = fieldGroup("Output directory", outputRow);
        return section("File locations", inputGroup, outputGroup);
    }

    private VBox saveSection() {
        replaceOriginal.setToggleGroup(saveGroup);
        saveNew.setToggleGroup(saveGroup);
        saveNew.setSelected(true);

        VBox choices = new VBox(10, replaceOriginal, saveNew);
        return section("Save options", choices);
    }

    private VBox passwordSection() {
        passwordField.setPromptText("Enter password");
        confirmPassword.setPromptText("Confirm password");
        shownPassword.setPromptText("Enter password");
        shownConfirmPassword.setPromptText("Confirm password");

        shownPassword.setVisible(false);
        shownPassword.setManaged(false);
        shownConfirmPassword.setVisible(false);
        shownConfirmPassword.setManaged(false);

        StackPane password = new StackPane(passwordField, shownPassword);
        StackPane confirmation = new StackPane(confirmPassword, shownConfirmPassword);
        VBox fields = new VBox(9, password, confirmation);
        return section("Password", fields, showPassword);
    }

    private void addListeners() {
        operationGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> updateOperationMode());

        saveGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> updateSaveMode());

        showPassword.selectedProperty().addListener((observable, oldValue, show) -> togglePasswordVisibility(show));

        passwordField.textProperty().bindBidirectional(shownPassword.textProperty());
        confirmPassword.textProperty().bindBidirectional(shownConfirmPassword.textProperty());
    }

    private VBox section(String title, Node... content) {
        Label label = new Label(title);
        label.getStyleClass().add("section-title");

        VBox section = new VBox(8);
        section.getStyleClass().add("form-section");
        section.getChildren().add(label);
        section.getChildren().addAll(content);
        section.setMaxWidth(Double.MAX_VALUE);
        return section;
    }

    private VBox fieldGroup(String title, Node content) {
        Label label = new Label(title);
        label.getStyleClass().add("field-label");

        VBox group = new VBox(5, label, content);
        group.setFillWidth(true);
        return group;
    }

    private void updateOperationMode() {
        boolean encrypt = operationGroup.getSelectedToggle() == encryptButton;

        if (!encrypt && !inputField.getText().isBlank()) {
            try {
                if (Files.isDirectory(Path.of(inputField.getText()))) {
                    inputField.clear();
                    outputDir.clear();
                }
            } catch (RuntimeException exception) {
                inputField.clear();
                outputDir.clear();
            }
        }

        actionButton.setText(encrypt ? "Encrypt file" : "Decrypt file");
        confirmPassword.setDisable(!encrypt);
        shownConfirmPassword.setDisable(!encrypt);
        browseInputFolderButton.setDisable(!encrypt);
    }

    private void updateSaveMode() {
        boolean separate = saveGroup.getSelectedToggle() == saveNew;
        outputDir.setDisable(!separate);
        browseDirectoryButton.setDisable(!separate);
    }

    private void togglePasswordVisibility(boolean show) {
        passwordField.setVisible(!show);
        passwordField.setManaged(!show);
        shownPassword.setVisible(show);
        shownPassword.setManaged(show);

        confirmPassword.setVisible(!show);
        confirmPassword.setManaged(!show);
        shownConfirmPassword.setVisible(show);
        shownConfirmPassword.setManaged(show);
    }

    private void chooseInputFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select input file");

        File selected = chooser.showOpenDialog(getStage());
        if (selected == null) {
            return;
        }

        Path input = selected.toPath().toAbsolutePath().normalize();
        inputField.setText(input.toString());

        Path parent = input.getParent();
        if (parent != null) {
            outputDir.setText(parent.toString());
        }
    }

    private void chooseInputDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select input directory");

        File selected = chooser.showDialog(getStage());
        if (selected == null) {
            return;
        }

        Path input = selected.toPath().toAbsolutePath().normalize();
        inputField.setText(input.toString());
        outputDir.setText(input.getParent() == null ? input.toString() : input.getParent().toString());
    }

    private void chooseOutputDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select output directory");

        String current = outputDir.getText();
        if (!current.isBlank()) {
            File directory = new File(current);
            if (directory.isDirectory()) {
                chooser.setInitialDirectory(directory);
            }
        }

        File selected = chooser.showDialog(getStage());
        if (selected != null) {
            outputDir.setText(selected.getAbsolutePath());
        }
    }

    private void performOperation() {
        boolean encrypt = operationGroup.getSelectedToggle() == encryptButton;
        boolean separate = saveGroup.getSelectedToggle() == saveNew;

        Path input = getInputPath();
        if (input == null) {
            return;
        }

        String password = getPassword();
        if (password.isEmpty()) {
            showValidationError("Please enter a password.");
            return;
        }

        if (encrypt && !password.equals(getConfirmation())) {
            showValidationError("Passwords do not match.");
            return;
        }

        Path output = getOutputPath(input, encrypt, separate);
        if (output == null) {
            return;
        }

        if (encrypt && !separate && !confirmReplacement(input)) {
            return;
        }

        char[] passwordChars = password.toCharArray();
        SaveMode saveMode = separate
                ? SaveMode.SEPARATE_FILE
                : SaveMode.REPLACE_ORIGINAL;

        showProgress(encrypt ? "Encrypting file..." : "Decrypting file...");

        try {
            OperationCallbacks callbacks = new OperationCallbacks(
                    this::appendMessage, this::appendError, this::updateProgress, this::operationFinished
            );
            if (encrypt) {
                handler.encrypt(
                        input,
                        output,
                        passwordChars,
                        saveMode,
                        callbacks
                );
            } else {
                handler.decrypt(
                        input,
                        output,
                        passwordChars,
                        saveMode,
                        callbacks
                );
            }
        } catch (RuntimeException exception) {
            appendError("Could not start operation: " + safeMessage(exception));
            operationFinished();
        } finally {
            Arrays.fill(passwordChars, '\0');
        }
    }

    private Path getInputPath() {
        if (inputField.getText().isBlank()) {
            showValidationError("Please select an input file.");
            return null;
        }

        try {
            Path input = Path.of(inputField.getText()).toAbsolutePath().normalize();
            if ((!Files.isRegularFile(input) && !Files.isDirectory(input))
                    || !Files.isReadable(input)) {
                showValidationError("The input file or directory does not exist or cannot be read.");
                return null;
            }
            if (operationGroup.getSelectedToggle() == decryptButton && Files.isDirectory(input)) {
                showValidationError("To decrypt a directory, select its encrypted .enc file.");
                return null;
            }
            return input;
        } catch (RuntimeException exception) {
            showValidationError("The input file path is invalid.");
            return null;
        }
    }

    private Path getOutputPath(Path input, boolean encrypt, boolean separate) {
        if (!separate) {
            if (Files.isDirectory(input)) {
                Path output = input.resolveSibling(input.getFileName() + ".enc");
                if (Files.exists(output)) {
                    showValidationError("The encrypted output already exists.");
                    return null;
                }
                return output;
            }
            return input;
        }

        if (outputDir.getText().isBlank()) {
            showValidationError("Please select an output directory.");
            return null;
        }

        Path directory;
        try {
            directory = Path.of(outputDir.getText()).toAbsolutePath().normalize();
        } catch (RuntimeException exception) {
            showValidationError("The output directory path is invalid.");
            return null;
        }

        if (!Files.isDirectory(directory)) {
            showValidationError("The output directory does not exist.");
            return null;
        }

        String outputName = encrypt
                ? OutputNameResolver.forEncryption(input.getFileName().toString())
                : OutputNameResolver.forDecryption(input.getFileName().toString());
        Path output = directory.resolve(outputName).normalize();

        if (output.equals(input)) {
            showValidationError("Input and output files must be different.");
            return null;
        }

        if (!encrypt && Files.isDirectory(output)) {
            output = OutputNameResolver.nextRestoredDirectory(output);
        }

        if (Files.exists(output)) {
            showValidationError("The output file already exists. Choose another directory.");
            return null;
        }
        return output;
    }

    private void showProgress(String title) {
        progressTitle.setText(title);
        status.setText("Please wait...");
        messagesArea.clear();
        messagesArea.appendText("Operation started.\n");

        progressBar.setProgress(0);
        backButton.setDisable(true);

        formView.setVisible(false);
        formView.setManaged(false);

        progressView.setVisible(true);
        progressView.setManaged(true);
    }

    private void updateProgress(double progress) {
        if (Double.isFinite(progress)) {
            progressBar.setProgress(Math.max(0, Math.min(1, progress)));
            status.setText(String.format("Progress: %.0f%%", progress * 100));
        }
    }

    private void appendMessage(String message) {
        status.setText(message);
        messagesArea.appendText("[INFO] " + message + "\n");
    }

    private void appendError(String message) {
        messagesArea.appendText("[ERROR] " + message + "\n");
        status.setText("Operation failed.");
    }

    private void operationFinished() {
        progressBar.setProgress(1.0);
        backButton.setDisable(false);
        inputField.clear();
        outputDir.clear();
    }

    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("");
        alert.setHeaderText("Cannot start operation");
        alert.setContentText(message);
        alert.initOwner(getStage());
        alert.getDialogPane().getStylesheets()
                .add(getClass().getResource("/style.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("error-alert");
        alert.showAndWait();
    }

    private boolean confirmReplacement(Path input) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("");
        alert.setHeaderText("Original data will be replaced");
        alert.setContentText(
                "After successful encryption, the original "
                        + (Files.isDirectory(input) ? "folder and its contents" : "file")
                        + " will be permanently deleted. Continue?"
        );
        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        alert.initOwner(getStage());
        alert.getDialogPane().getStylesheets()
                .add(getClass().getResource("/style.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("warning-alert");

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showForm() {
        progressView.setVisible(false);
        progressView.setManaged(false);

        formView.setVisible(true);
        formView.setManaged(true);
    }

    private String getPassword() {
        return showPassword.isSelected()
                ? shownPassword.getText()
                : passwordField.getText();
    }

    private String getConfirmation() {
        return showPassword.isSelected()
                ? shownConfirmPassword.getText()
                : confirmPassword.getText();
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName()
                : message;
    }

    private Stage getStage() {
        return (Stage) getScene().getWindow();
    }
}
