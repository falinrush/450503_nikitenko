
package tetris;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

final class GameController {

    private final Board board;

    private final NotificationOverlay notificationOverlay;

    private final ScoreManager scoreManager;

    private final BooleanProperty paused = new SimpleBooleanProperty();

    public GameController() {
        this.board = new Board();
        this.scoreManager = new ScoreManager(this);

        notificationOverlay = new NotificationOverlay(this);
        paused.addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean aBoolean2) {
                if (aBoolean2) {
                    pause();
                } else {
                    play();
                }
            }
        });
    }

    public BooleanProperty pausedProperty() {
        return paused;
    }

    public void start() {
        board.start();
        scoreManager.scoreProperty().set(0);
        paused.set(false);
    }

    public void startBot() {
        board.startBot();
        scoreManager.scoreProperty().set(0);
        paused.set(false);
    }

    public void startReplay() {
        board.startReplay();
        scoreManager.scoreProperty().set(0);
        paused.set(false);
    }
    private void pause() {
        board.pause();
    }

    public void stop() {
        board.clear();
        scoreManager.scoreProperty().set(0);
        paused.set(false);
    }

    public Board getBoard() {
        return board;
    }


    public void play() {
        paused.set(false);
        board.play();
    }

    public NotificationOverlay getNotificationOverlay() {
        return notificationOverlay;
    }

    public ScoreManager getScoreManager() {
        return scoreManager;
    }
}
