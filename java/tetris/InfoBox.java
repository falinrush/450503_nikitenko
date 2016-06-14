
package tetris;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.concurrent.Callable;



final class InfoBox extends VBox {
    public InfoBox(final GameController gameController) {
        setPadding(new Insets(200, 20, 20, 20));
        setSpacing(10);

        setId("infoBox");


        Button btnStart = new Button("New Game");
        btnStart.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                gameController.start();
            }
        });

        Button btnBot = new Button("Auto mode ");
        btnBot.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                gameController.startBot();
            }
        });
        Button btnReplay = new Button("Replay        ");
        btnReplay.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                gameController.startReplay();
            }
        });

        Button btnStop = new Button("Clear" );
        btnStop.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                gameController.stop();
            }
        });
        btnStop.setMaxWidth(Double.MAX_VALUE);
        btnStop.setAlignment(Pos.CENTER_LEFT);

        btnStart.setMaxWidth(Double.MAX_VALUE);
        btnStart.setAlignment(Pos.CENTER_LEFT);
        Button btnPause = new Button("Pause");
        btnPause.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (gameController.pausedProperty().get()) {
                    gameController.pausedProperty().set(false);
                } else {
                    gameController.pausedProperty().set(true);

                }
            }
        });
        btnPause.setMaxWidth(Double.MAX_VALUE);
        btnPause.setAlignment(Pos.CENTER_LEFT);


        Label lblPoints = new Label();
        lblPoints.getStyleClass().add("score");
        lblPoints.textProperty().bind(Bindings.createStringBinding(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return String.valueOf(gameController.getScoreManager().scoreProperty().get());
            }
        }, gameController.getScoreManager().scoreProperty()));
        lblPoints.setAlignment(Pos.CENTER_RIGHT);
        lblPoints.setMaxWidth(Double.MAX_VALUE);
        //lblPoints.setEffect(new Reflection());

        getChildren().add(btnStart);
        getChildren().add(btnBot);
        getChildren().add(btnPause);
        getChildren().add(btnStop);
        getChildren().add(btnReplay);


        Label lblInfo = new Label("Score:");

        getChildren().add(lblInfo);

        getChildren().addAll(lblPoints);


    }
}
