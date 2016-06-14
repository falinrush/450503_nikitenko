
package tetris;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class App extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage primaryStage) throws Exception {


        primaryStage.setTitle("Tetris");

        Scene scene = new Scene(new Tetris());

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}