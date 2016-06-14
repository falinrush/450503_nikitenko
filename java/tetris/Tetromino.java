
package tetris;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.Group;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

import java.util.Random;

final class Tetromino extends Group implements Cloneable {

    private static final Random RANDOM = new Random();

    private static final TetrominoDefinition I = new TetrominoDefinition(new int[][]{
            {0, 0, 0, 0},
            {1, 1, 1, 1},
            {0, 0, 0, 0},
            {0, 0, 0, 0}
    }, Color.RED);

    private static final TetrominoDefinition J = new TetrominoDefinition(new int[][]{
            {1, 0, 0},
            {1, 1, 1},
            {0, 0, 0}
    }, Color.BLUE);

    private static final TetrominoDefinition L = new TetrominoDefinition(new int[][]{
            {0, 0, 1},
            {1, 1, 1},
            {0, 0, 0}
    }, Color.ORANGE);

    private static final TetrominoDefinition O = new TetrominoDefinition(new int[][]{
            {1, 1},
            {1, 1}
    }, Color.GREEN);

    private static final TetrominoDefinition S = new TetrominoDefinition(new int[][]{
            {0, 1, 1},
            {1, 1, 0},
            {0, 0, 0}
    }, Color.DARKGRAY);

    private static final TetrominoDefinition T = new TetrominoDefinition(new int[][]{
            {0, 1, 0},
            {1, 1, 1},
            {0, 0, 0}
    }, Color.BROWN);

    private static final TetrominoDefinition Z = new TetrominoDefinition(new int[][]{
            {1, 1, 0},
            {0, 1, 1},
            {0, 0, 0}
    }, Color.ORANGERED);

    public static final TetrominoDefinition[] TETROMINO_DEFINITIONS = new TetrominoDefinition[]{I, J, L, O, S, T, Z};

    /**
     * Random index in TETROMINO_DEFINITION
     * */

    private int randomTetrominoDefenition;

    private Lighting lighting = new Lighting();

    private int[][] matrix;

    private Paint paint;

    private TetrominoDefinition tetrominoDefinition;

    private ReadOnlyDoubleProperty squareSize;

    private Tetromino(TetrominoDefinition tetrominoDefinition, ReadOnlyDoubleProperty squareSize) {
        this.matrix = tetrominoDefinition.matrix;
        this.tetrominoDefinition = tetrominoDefinition;
        this.squareSize = squareSize;
        paint = tetrominoDefinition.color;

        lighting = new Lighting();


        lighting.setSurfaceScale(1);
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {

                final Rectangle rectangle = new Rectangle();
                final int finalI = i;
                final int finalJ = j;
                ChangeListener<Number> changeListener = new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                        rectangle.setWidth(number2.doubleValue());
                        rectangle.setHeight(number2.doubleValue());
                        rectangle.setTranslateY(number2.doubleValue() * finalI);
                        rectangle.setTranslateX(number2.doubleValue() * finalJ);
                    }
                };
                rectangle.setUserData(changeListener);
                squareSize.addListener(new WeakChangeListener<>(changeListener));
                rectangle.setWidth(squareSize.doubleValue());
                rectangle.setHeight(squareSize.get());
                rectangle.setTranslateY(squareSize.get() * finalI);
                rectangle.setTranslateX(squareSize.get() * finalJ);
                //анимация в воздухе
                if (matrix[i][j] == 1) {
                    rectangle.setEffect(lighting);
                    rectangle.setFill(tetrominoDefinition.color);
                    rectangle.setFill(paint);
                   // rectangle.setArcHeight(7);
                    //rectangle.setArcWidth(7);
                } else {
                    rectangle.setOpacity(0);
                }
                getChildren().add(rectangle);

            }
        }

        //setCacheHint(CacheHint.SPEED);
        //setCache(true);
    }

    public static Tetromino random(ReadOnlyDoubleProperty squareSize) {
        int randomInt = RANDOM.nextInt(7);
        TetrominoDefinition tetrominoDefinition = TETROMINO_DEFINITIONS[randomInt];

        Tetromino x = new Tetromino(tetrominoDefinition, squareSize);
        x.setRandomTetrominoDefenition(randomInt);
        return x;
    }

    public void setRandomTetrominoDefenition(int i) {
        randomTetrominoDefenition = i;
    }

    @Override
    public Tetromino clone() {
        return new Tetromino(tetrominoDefinition, squareSize);
    }

    public Paint getFill() {
        return paint;
    }

    public Lighting getLighting() {
        return lighting;
    }

    public int[][] getMatrix() {
        return matrix;
    }

    public void setMatrix(int[][] matrix) {
        this.matrix = matrix;
    }

    private static class TetrominoDefinition {
        private final Color color;

        private final int[][] matrix;

        private TetrominoDefinition(int[][] matrix, Color color) {
            this.color = color;
            this.matrix = matrix;
        }
    }

    @Override
    public String toString() {
        return new Integer(randomTetrominoDefenition).toString() ;
    }
}
