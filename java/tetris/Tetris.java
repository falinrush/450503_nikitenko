
package tetris;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.HorizontalDirection;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;


public final class Tetris extends HBox {
    /**
     * Stores if the arrow down key was pressed, to prevent repeated events.
     * Магазины, если стрелка вниз клавишу прессовали, чтобы предотвратить повторные события.
     */
    private boolean movingDown = false;

    public Tetris() {

        setId("tetris");

        sceneProperty().addListener(new ChangeListener<Scene>() {
            public void changed(ObservableValue<? extends Scene> observableValue, Scene scene, Scene scene2) {
                if (scene2 != null) {
                    scene2.getStylesheets().add("tetris/styles.css");
                }
            }
        });
        final GameController gameController = new GameController();

        StackPane stackPane = new StackPane();
        stackPane.getChildren().add(gameController.getBoard());

        stackPane.getChildren().add(gameController.getNotificationOverlay());
        stackPane.setAlignment(Pos.TOP_CENTER);

        getChildren().add(stackPane);

        InfoBox infoBox = new InfoBox(gameController);
        infoBox.setMaxHeight(Double.MAX_VALUE);

        HBox.setHgrow(infoBox, Priority.ALWAYS);
        getChildren().add(infoBox);

        setOnKeyPressed(new EventHandler<KeyEvent>() {
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.P) {
                    gameController.pausedProperty().set(!gameController.pausedProperty().get());
                    keyEvent.consume();
                }
            }
        });
        setOnMouseClicked(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent mouseEvent) {
                gameController.getBoard().requestFocus();

            }
        });

        // возможно в этом месте поставить флаг, который будет проверять юзалась ли кнопка БОТа.
        //если да, то сделать выдачу рандомных нажатий.
        setOnKeyPressed(new EventHandler<KeyEvent>() {
            public void handle(KeyEvent keyEvent) {

                if (keyEvent.getCode() == KeyCode.LEFT && !gameController.pausedProperty().get()) {
                    gameController.getBoard().move(HorizontalDirection.LEFT);
                    long br = System.nanoTime();
                    new Action(20, Action.MOVE_LEFT);
                    keyEvent.consume();
                }

                if (keyEvent.getCode() == KeyCode.RIGHT && !gameController.pausedProperty().get()) {
                    gameController.getBoard().move(HorizontalDirection.RIGHT);
                    new Action(100, Action.MOVE_RIGHT);
                    keyEvent.consume();
                }

                if (keyEvent.getCode() == KeyCode.UP && !gameController.pausedProperty().get()) {
                    gameController.getBoard().rotate(HorizontalDirection.LEFT);
                    new Action(100, Action.ROTATE);
                    keyEvent.consume();
                }

                if (keyEvent.getCode() == KeyCode.DOWN) {
                    if (!movingDown) {
                        if (!gameController.pausedProperty().get()) {
                            gameController.getBoard().moveDownFast();
                        }
                        movingDown = true;
                        keyEvent.consume();
                    }
                }
                if (keyEvent.getCode() == KeyCode.SPACE && !gameController.pausedProperty().get()) {
                    gameController.getBoard().dropDown();
                    new Action(100, Action.DROP_DOWN);
                    keyEvent.consume();
                }
            }
        });

        setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.DOWN) {
                    movingDown = false;
                    gameController.getBoard().moveDown();
                }
            }
        });

    }
}
