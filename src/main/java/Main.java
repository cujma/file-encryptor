
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

        Scene scene = new Scene(view, 440, 640);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setTitle("");
        stage.setMinWidth(420);
        stage.setMinHeight(640);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
