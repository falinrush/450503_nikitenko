package tetris;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Дима on 06.06.2016.
 */

public class Replay implements Serializable {
    ArrayList<Action> actions = new ArrayList<>();
    ArrayList<Integer> tetrominos = new ArrayList<>();

    transient private int nextActionIndx = 0;
    transient private int nextTetrminoIndx = 0;

    public void addAction(Action action) { actions.add(action); }
    public void addTetromino(int number) {tetrominos.add(number);}

    public boolean hasNextAction() {return nextActionIndx < actions.size();}
    public Action getNextAction(){return actions.get(nextActionIndx++);}

    public boolean hasNextTetromino() {return nextTetrminoIndx < tetrominos.size();}
    public int getNextTetrmino() {return tetrominos.get(nextTetrminoIndx++);}
   }

class Action implements Serializable{
    public static byte ROTATE = 0x1;
    public static byte MOVE_LEFT = 0x2;
    public static byte MOVE_RIGHT = 0x3;
    public static byte DROP_DOWN = 0x4;

    private long time;
    private byte action;

    Action(long time, byte action) {
        this.action = action;
        this.time = time;
    }

    public long getTime() {return time;}

    public byte getAction() {return action;}
}
