
package tetris;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HorizontalDirection;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.effect.Light;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

final class Board extends StackPane {

    private boolean bot = false;

    private long startTime;


    public void setBot(boolean bot) {
        this.bot = bot;
    }

    private static final byte HIDDEN_ROWS = 2;

    private static final byte BLOCKS_PER_ROW = 10;

    private static final byte BLOCKS_PER_COLUMN = 20;

    /*
     * Движение вниз.
     */
    private final TranslateTransition moveDownTransition;

    /**
     * Rotate переход.
     */
    private final RotateTransition rotateTransition;

    /**
     * Последовательный переход.
     */
    private final SequentialTransition moveTransition;

    /**
     * Переход, который позволяет часть двигаться вниз быстро.
     */
    private final TranslateTransition moveDownFastTransition;

    /**
     * Переход перевод левого / правого движения.
     */
    private final TranslateTransition translateTransition;

    /**
     * Множество запущенных переходов. Все запущенные переходы приостановлены, когда игра приостановлена.
     */
    private final Set<Animation> runningAnimations = new HashSet<>();

    /**
     * Двумерный массив, который определяет плату. Если элемент является нулевым в матрице, она пуста, в противном случае он занят.
     */
    private final Rectangle[][] matrix = new Rectangle[BLOCKS_PER_COLUMN + HIDDEN_ROWS][BLOCKS_PER_ROW];

    /**
     * Список тетромино, которые приходят в следующем.
     */
    private final ObservableList<Tetromino> waitingTetrominos = FXCollections.observableArrayList();

    /**
     * Очень быстро выпадающие переход.
     */
    private final TranslateTransition dropDownTransition;

    private boolean moving = false;

    /**
     * Ток х и у позиции с матрицей текущего Tetromino.
     */
    private int x = 0, y = 0;

    /**
     * Правда, в то время как Tetromino падает (с помощью клавиши пробела).
     */
    private boolean isDropping = false;

    /**
     * Tetromino, который падает.
     */
    private Tetromino currentTetromino;

    private List<BoardListener> boardListeners = new CopyOnWriteArrayList<>();

    private DoubleProperty squareSize = new SimpleDoubleProperty();

    /**
     * Создает доску.
     */
    public Board() {
        setFocusTraversable(true);

        setId("board");
        setMinWidth(35 * BLOCKS_PER_ROW);
        setMinHeight(35 * BLOCKS_PER_COLUMN);

        maxWidthProperty().bind(minWidthProperty());
        maxHeightProperty().bind(minHeightProperty());


        clipProperty().bind(new ObjectBinding<Node>() {
            {
                super.bind(widthProperty(), heightProperty());
            }

            protected Node computeValue() {
                return new Rectangle(getWidth(), getHeight());
            }
        });

        setAlignment(Pos.TOP_LEFT);

        //задаем скорость падения.
        moveDownTransition = new TranslateTransition(Duration.seconds(0.3));
        moveDownTransition.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                moving = false;
                y++;
            }
        });

        //После того, как часть переместилась вниз, немного подождите, пока она не начнет двигаться снова.
        PauseTransition pauseTransition = new PauseTransition();
        pauseTransition.durationProperty().bind(moveDownTransition.durationProperty());

        moveTransition = new SequentialTransition();
        moveTransition.getChildren().addAll(moveDownTransition, pauseTransition);
        moveTransition.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                moveDown();
            }
        });

        //Это движение должно быть правдоподобным.
        registerPausableAnimation(moveTransition);

        //  кусок вниз быстро.
        moveDownFastTransition = new TranslateTransition(Duration.seconds(0.08));
        //Для того, чтобы заставить это выглядеть более гладко, то испозьзую линейный интерполятор.
        moveDownFastTransition.setInterpolator(Interpolator.LINEAR);
        moveDownFastTransition.setOnFinished(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent actionEvent) {
                y++;
                moveDownFast();
            }
        });
        registerPausableAnimation(moveDownFastTransition);

        // кусок влево и вправо.
        translateTransition = new TranslateTransition(Duration.seconds(0.1));
        registerPausableAnimation(translateTransition);

        // Вращает часть.
        rotateTransition = new RotateTransition(Duration.seconds(0.1));
        dropDownTransition = new TranslateTransition(Duration.seconds(0.1));
        dropDownTransition.setInterpolator(Interpolator.EASE_IN);

        squareSize.bind(new DoubleBinding() {
            {
                super.bind(widthProperty());
            }

            @Override
            protected double computeValue() {
                return getWidth() / BLOCKS_PER_ROW;
            }
        });
    }

    /**
     * Регистры анимацию, которая добавляется к списку запущенных анимации, если она была запущена, и удаляется снова,
     * если она остановлена. Когда игра останавливается, все запущенные анимации приостановлены.
     */
    private void registerPausableAnimation(final Animation animation) {
        animation.statusProperty().addListener(new ChangeListener<Animation.Status>() {
            @Override
            public void changed(ObservableValue<? extends Animation.Status> observableValue, Animation.Status status, Animation.Status status2) {
                if (status2 == Animation.Status.STOPPED) {
                    runningAnimations.remove(animation);
                } else {
                    runningAnimations.add(animation);
                }
            }
        });
    }

    /**
     * спавм новое случайное Tetromino.
     */

    private void spawnTetromino() {

        //след. фигура
        while (waitingTetrominos.size() <= 1) {
            waitingTetrominos.add(Tetromino.random(squareSize));
            System.out.println(waitingTetrominos.get(waitingTetrominos.size() - 1));
        }

        // Удаляем первый из очереди и создаем его.
        currentTetromino = waitingTetrominos.remove(0);

        // Сбросить все переходы.
        rotateTransition.setNode(currentTetromino);
        rotateTransition.setToAngle(0);

        translateTransition.setNode(currentTetromino);
        moveDownTransition.setNode(currentTetromino);
        moveDownFastTransition.setNode(currentTetromino);

        // Добавление текущей Tetromino к доске.
        getChildren().add(currentTetromino);

        // Переместить его в правильное положение
        // респ Tetromino в середине (I, O) или в левой середине (J, L, S, T, Z).
        x = (matrix[0].length - currentTetromino.getMatrix().length) / 2;
        y = 0;

        // Перевести Tetromino в исходное положение.
        currentTetromino.setTranslateY((y - Board.HIDDEN_ROWS) * getSquareSize());
        currentTetromino.setTranslateX(x * getSquareSize());


        // Начинаем, чтобы переместить его.
        moveDown();
    }

    /**
     * Уведомление о Tetromino, что он не может двигаться дальше вниз.
     */
    private void tetrominoDropped() {
        if (y == 0) {
            // Если кусок не мог двигаться, и мы все еще находятся в начальной позиции у, игра окончена.
            currentTetromino = null;
            waitingTetrominos.clear();
            notifyGameOver();
        } else {
            mergeTetrominoWithBoard();
        }
    }

   /**
    * * Уведомляет, что игра окончена.
    */
   private void notifyGameOver() {
       for (BoardListener boardListener : boardListeners) {
           boardListener.onGameOver();
       }
   }

   /**
    * Уведомляет , что строки были устранены.
    */
   private void notifyOnRowsEliminated(int rows) {
       for (BoardListener boardListener : boardListeners) {
           boardListener.onRowsEliminated(rows);
       }
   }

    /**
     * Объединяет Tetromino с доской.
       Для каждой плитки, создаем прямоугольник в доске.
       В конце концов удаляет Tetromino с доски и запускает новый.
     */
    private void mergeTetrominoWithBoard() {
        int[][] tetrominoMatrix = currentTetromino.getMatrix();

        for (int i = 0; i < tetrominoMatrix.length; i++) {
            for (int j = 0; j < tetrominoMatrix[i].length; j++) {

                final int x = this.x + j;
                final int y = this.y + i;

                if (tetrominoMatrix[i][j] == 1 && y < BLOCKS_PER_COLUMN + HIDDEN_ROWS && x < BLOCKS_PER_ROW) {
                    final Rectangle rectangle = new Rectangle();

                    ChangeListener<Number> changeListener = new ChangeListener<Number>() {
                        @Override
                        public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                            rectangle.setWidth(number2.doubleValue());
                            rectangle.setHeight(number2.doubleValue());
                            rectangle.setTranslateX(number2.doubleValue() * x);
                            rectangle.setTranslateY(number2.doubleValue() * ((Integer) rectangle.getProperties().get("y")));
                        }
                    };
                    squareSize.addListener(new WeakChangeListener<>(changeListener));
                    rectangle.setUserData(changeListener);
                    rectangle.getProperties().put("y", y - HIDDEN_ROWS);
                    rectangle.setWidth(squareSize.doubleValue());
                    rectangle.setHeight(squareSize.doubleValue());
                    rectangle.setTranslateX(squareSize.doubleValue() * x);
                    rectangle.setTranslateY(squareSize.doubleValue() * ((Integer) rectangle.getProperties().get("y")));

                    rectangle.setFill(currentTetromino.getFill());
                    rectangle.setEffect(currentTetromino.getLighting());

                   // rectangle.setArcHeight(0);
                    //rectangle.setArcWidth(0);
                    // Присвоить прямоугольник с матрицей платы.
                    matrix[y][x] = rectangle;
                    getChildren().add(rectangle);
                }
            }
        }

        ParallelTransition fallRowsTransition = new ParallelTransition();
        ParallelTransition deleteRowTransition = new ParallelTransition();
        int fall = 0;

        for (int i = y + currentTetromino.getMatrix().length - 1; i >= 0; i--) {
            if (i < matrix.length) {
                boolean rowComplete = i >= y;

                // Предположим, что строка завершена. Докажем обратное.
                if (rowComplete) {
                    for (int j = 0; j < matrix[i].length; j++) {
                        if (matrix[i][j] == null) {
                            rowComplete = false;
                            break;
                        }
                    }
                }
                if (rowComplete) {
                    deleteRowTransition.getChildren().add(deleteRow(i));
                    fall++;
                } else if (fall > 0) {
                    fallRowsTransition.getChildren().add(fallRow(i, fall));
                }
            }
        }
        final int f = fall;

        // Если хотя бы одна строка была устранена.
        if (f > 0) {
            notifyOnRowsEliminated(f);
        }
        final SequentialTransition sequentialTransition = new SequentialTransition();
        sequentialTransition.getChildren().add(deleteRowTransition);
        sequentialTransition.getChildren().add(fallRowsTransition);
        sequentialTransition.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                //. // последовательные Transition.getChildren;
                spawnTetromino();
            }
        });
        // Кэшированные узлы утечка памяти
        getChildren().remove(currentTetromino);
        currentTetromino = null;
        registerPausableAnimation(sequentialTransition);
        sequentialTransition.playFromStart();
    }

    private Transition fallRow(final int i, final int by) {
        ParallelTransition parallelTransition = new ParallelTransition();

        if (by > 0) {
            for (int j = 0; j < matrix[i].length; j++) {
                final Rectangle rectangle = matrix[i][j];

                if (rectangle != null) {
                    final TranslateTransition translateTransition = new TranslateTransition(Duration.seconds(0.1), rectangle);
                    rectangle.getProperties().put("y", i - HIDDEN_ROWS + by);

                    translateTransition.toYProperty().bind(squareSize.multiply(i - HIDDEN_ROWS + by));
                    translateTransition.setOnFinished(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent actionEvent) {
                            translateTransition.toYProperty().unbind();

                        }
                    });
                    parallelTransition.getChildren().add(translateTransition);
                }
                matrix[i + by][j] = rectangle;
            }
        }
        return parallelTransition;
    }

    /**
     * Удаляет строку на доске.
     */
    private Transition deleteRow(int rowIndex) {

        ParallelTransition parallelTransition = new ParallelTransition();

        for (int i = rowIndex; i >= 0; i--) {
            for (int j = 0; j < BLOCKS_PER_ROW; j++) {
                if (i > 1) {
                    final Rectangle rectangle = matrix[i][j];

                    if (i == rowIndex && rectangle != null) {
                        FadeTransition fadeTransition = new FadeTransition(Duration.seconds(.01), rectangle);
                        fadeTransition.setToValue(0);
                        fadeTransition.setCycleCount(3);
                        fadeTransition.setAutoReverse(true);
                        fadeTransition.setOnFinished(new EventHandler<ActionEvent>() {
                            public void handle(ActionEvent actionEvent) {
                                getChildren().remove(rectangle);
                            }
                        });
                        parallelTransition.getChildren().add(fadeTransition);
                    }

                }
            }
        }
        return parallelTransition;
    }

    /**
     * Очищает доску .
     */
    public void clear() {
        for (int i = 0; i < BLOCKS_PER_COLUMN + HIDDEN_ROWS; i++) {
            for (int j = 0; j < BLOCKS_PER_ROW; j++) {
                matrix[i][j] = null;
            }
        }
        getChildren().clear();
        getChildren().remove(currentTetromino);
        currentTetromino = null;
        waitingTetrominos.clear();
    }

    /**
     * Рассчитывает если Tetromino пересекались бы с доской,
     путем пропускания матрицы, что есть в tetromino.
     Она пересекает либо, если она попадает на другой Tetromino или если оно превышает левую, правую или нижнюю границу.


     * Правда, если пересекается с доской, в противном случае ложно.
     */
    private boolean intersectsWithBoard(final int[][] targetMatrix, int targetX, int targetY) {
        Rectangle[][] boardMatrix = matrix;

        for (int i = 0; i < targetMatrix.length; i++) {
            for (int j = 0; j < targetMatrix[i].length; j++) {

                boolean boardBlocks = false;
                int x = targetX + j;
                int y = targetY + i;

                if (x < 0 || x >= boardMatrix[i].length || y >= boardMatrix.length) {
                    boardBlocks = true;
                } else if (boardMatrix[y][x] != null) {
                    boardBlocks = true;
                }

                if (boardBlocks && targetMatrix[i][j] == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private Replay replay = new Replay();

    public void start() {
        clear();
        startTime = System.nanoTime();
        replay = new Replay();


        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                requestFocus();
            }
        });
        spawnTetromino();
    }

    private boolean isReplay = false;
    public void startReplay() {
        clear();
        isReplay = true;
        //long startTime = System.nanoTime();




        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                requestFocus();
            }
        });
        spawnTetromino();
    }

    public void startBot() {
        setBot(true);
        clear();

        startTime = System.nanoTime();
        replay = new Replay();

        Thread thread = new Thread(new Runnable() {
            private boolean rt = true;
            private Random rnd = new Random();
            @Override
            public void run() {
                HorizontalDirection x;
                if((rnd.nextInt() % 5) == 1) {
                    dropDown();
                    run();
                }
                if (rnd.nextBoolean()) {
                    x = HorizontalDirection.LEFT;
                } else {
                    x = HorizontalDirection.RIGHT;
                }
                if (this.rt) rotate(x);
                else move(x);
                rt = !rt;
                move(x);
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {

                }
                run();
            }
        });
        thread.setDaemon(true);
        thread.start();
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                requestFocus();
            }
        });
        spawnTetromino();
    }

    /**
     * Понижает Tetromino вниз к следующей возможной позиции ..
     */
    public void dropDown() {

        if (!isReplay) {
            long deltatime = System.nanoTime() - startTime;
            System.out.println("dropDown() delta = "+ deltatime);
            replay.addAction(new Action(deltatime, Action.DROP_DOWN));
        }

        if (currentTetromino == null) {
            return;
        }

        moveTransition.stop();
        moveDownFastTransition.stop();
        dropDownTransition.stop();

        do {
            y++;
        }
        while (!intersectsWithBoard(currentTetromino.getMatrix(), x, y));
        y--;
        isDropping = true;
        dropDownTransition.setNode(currentTetromino);
        dropDownTransition.toYProperty().bind(squareSize.multiply(y - Board.HIDDEN_ROWS));
        dropDownTransition.setOnFinished(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent actionEvent) {
                isDropping = false;
                tetrominoDropped();
            }
        });
        registerPausableAnimation(dropDownTransition);
        dropDownTransition.playFromStart();

    }

    /**
     * Правда, если вращение было успешно, в противном случае ложной.
     */
    public boolean rotate(final HorizontalDirection direction) {

        if (!isReplay) {
            long deltatime = System.nanoTime() - startTime;
            System.out.println("rotate() delta = " + deltatime);
            replay.addAction(new Action(deltatime, Action.ROTATE));
        }

        boolean result = false;
        if (currentTetromino == null) {
            result = false;
        } else {
            int[][] matrix = currentTetromino.getMatrix();

            int[][] newMatrix = new int[matrix.length][matrix.length];


            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    if (direction == HorizontalDirection.RIGHT) {
                        newMatrix[j][matrix.length - 1 - i] = matrix[i][j];
                    } else {
                        newMatrix[matrix[i].length - 1 - j][i] = matrix[i][j];
                    }
                }
            }

            if (!intersectsWithBoard(newMatrix, x, y)) {
                currentTetromino.setMatrix(newMatrix);

                int f = direction == HorizontalDirection.RIGHT ? 1 : -1;

                rotateTransition.setFromAngle(rotateTransition.getToAngle());
                rotateTransition.setToAngle(rotateTransition.getToAngle() + f * 90);

                KeyValue kv = new KeyValue(((Light.Distant) currentTetromino.getLighting().getLight()).azimuthProperty(), 360 - 225 + 90 - rotateTransition.getToAngle());
                KeyFrame keyFrame = new KeyFrame(rotateTransition.getDuration(), kv);
                Timeline lightingAnimation = new Timeline(keyFrame);

                final ParallelTransition parallelTransition = new ParallelTransition(rotateTransition, lightingAnimation);
                registerPausableAnimation(parallelTransition);
                parallelTransition.setOnFinished(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent actionEvent) {
                        // очисчает, поскольку в противном случае параллельный переход не будет,
                        // поскольку она направлена копия имеет отношение вращаться на переходном этапе.
                        parallelTransition.getChildren().clear();
                    }
                });
                parallelTransition.playFromStart();
                result = true;
            }
        }
        return result;
    }

    /*
     * Перемещение Tetromino влево или вправо.
     * Правда, если движение было успешным. Ложь, если движение было заблокировано бортом
     */
    public boolean move(final HorizontalDirection direction) {

        if (!isReplay) {
            long deltatime = System.nanoTime() - startTime;
            System.out.println("move() delta = "+ deltatime);
            replay.addAction(new Action(deltatime, direction == HorizontalDirection.RIGHT ? Action.MOVE_RIGHT : Action.MOVE_LEFT));
        }

        boolean result;
        if (currentTetromino == null || isDropping) {
            result = false;
        } else {
            int i = direction == HorizontalDirection.RIGHT ? 1 : -1;
            x += i;
            // Если он не движется, только проверить текущую у позицию.
            // Если он движется, а также проверить положение цели у.
            if (!moving && !intersectsWithBoard(currentTetromino.getMatrix(), x, y) || moving && !intersectsWithBoard(currentTetromino.getMatrix(), x, y) && !intersectsWithBoard(currentTetromino.getMatrix(), x, y + 1)) {
                translateTransition.toXProperty().unbind();
                translateTransition.toXProperty().bind(squareSize.multiply(x));
                translateTransition.playFromStart();
                result = true;
            } else {
                x -= i;
                result = false;
            }
        }
        return result;
    }

    /*
     * Перемещение поля Tetromino один вниз.
     */
    public void moveDown() {
        if (bot == true) {

        }
        if (!isDropping && currentTetromino != null) {
            moveDownFastTransition.stop();
            moving = true;
            // Если он способен перейти к следующему у позиции, то делает это.
            if (!intersectsWithBoard(currentTetromino.getMatrix(), x, y + 1) && !isDropping) {
                //moveDownTransition.setFromY(moveDownTransition.getNode().getTranslateY());
                moveDownTransition.toYProperty().unbind();
                moveDownTransition.toYProperty().bind(squareSize.multiply(y + 1 - Board.HIDDEN_ROWS));
                moveTransition.playFromStart();
            } else {
                tetrominoDropped();
            }
        }
    }

    /**
     * Перемещение текущего Tetromino вниз быстро, если он уже не снижается.
     */
    public void moveDownFast() {
        if (!isDropping) {
            // Остановить нормальное перемещение перехода.
            moveTransition.stop();
            // проверка, если следующая позиция, не пересекалась бы с полем.
            if (!intersectsWithBoard(currentTetromino.getMatrix(), x, y + 1)) {
                // Если он может двигаться, то перемещаем
                moveDownFastTransition.toYProperty().unbind();
                moveDownFastTransition.toYProperty().bind(squareSize.multiply(y + 1 - Board.HIDDEN_ROWS));
                moveDownFastTransition.playFromStart();
            } else {
                // В противном случае он достиг земли.
                tetrominoDropped();
            }
        }
    }

    /*
     * Приостановка поля.
     */
    public void pause() {
        for (Animation animation : runningAnimations) {
            if (animation.getStatus() == Animation.Status.RUNNING) {
                animation.pause();
            }
        }
    }

    /*
     * продолжить игру.
     */
    public void play() {
        for (Animation animation : runningAnimations) {
            if (animation.getStatus() == Animation.Status.PAUSED) {
                animation.play();
            }
        }
        requestFocus();
    }

    /**
     *
     * Получает ожидания тетромино, которые собираются быть реснутым.
     * Первый элемент будет порожден в следующем.
     * Список поставленных в очередь тетромино.
     */
    public ObservableList<Tetromino> getWaitingTetrominos() {
        return waitingTetrominos;
    }

    public double getSquareSize() {
        return squareSize.get();
    }

    /**
     * Добавляет слушателя к борту, который получает уведомление для определенных событий.
     */
    public void addBoardListener(BoardListener boardListener) {
        boardListeners.add(boardListener);
    }

    /**
     * Удаляет слушатель, который ранее был добавлен
     */
    public void removeBoardListener(BoardListener boardListener) {
        boardListeners.remove(boardListener);
    }

    /*
     * Позволяет прослушивать определенные события борту.
     */
    public static interface BoardListener extends EventListener {


        /*
         * Вызывается, когда Tetromino выброшены или полная строка удаляется после того, как некоторые строки были устранены.
         */
        void onDropped();

        /*
         * Вызывается, когда один или несколько строк заполнены и их удаляют..
         */
        void onRowsEliminated(int rows);

        /*
         * Вызывается, когда игра окончена.
         */
        void onGameOver();

        /*
         * Вызывается, когда была сделана недействительной.
         */
        void onInvalidMove();

       //Вызывается, когда фигура была перемещен.

        void onMove(HorizontalDirection horizontalDirection);

        // Вызывается, когда фигура была повернута.

        void onRotate(HorizontalDirection horizontalDirection);
    }
}
