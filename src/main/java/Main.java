
import gui.FileOperationHandler;
import gui.FileOperationService;
import gui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private final FileOperationHandler handler = new FileOperationService();

    @Override
    public void start(Stage stage) {
        MainView view = new MainView(handler);

        Scene scene = new Scene(view, 460, 700);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setTitle("File Encryptor");
        stage.setMinWidth(440);
        stage.setMinHeight(660);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
