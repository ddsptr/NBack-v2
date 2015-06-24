package com.nback.app;

//import android.support.v4.app.Fragment;
import android.app.Fragment;
import android.graphics.Point;
import android.util.Log;

import java.util.*;

class Game extends Observable implements Runnable{
    private final static String LOG_TAG = Game.class.getSimpleName();
    public static final int FIELD_SIZE = 3;
    public static final int GAME_CYCLES = 20;
    public static final int TIME_LAPSE_FAST = (int)(1 * 1000);
    public static final int TIME_LAPSE_MIDDLE = (int)(2 * 1000);
    public static final int TIME_LAPSE_SLOW = (int)(3 * 1000);

    private Point currentPoint;
    private int level;
    private Observer observer;
    private Queue<Point> moves = new LinkedList<Point>();
    private Matched lastMatch = Matched.EMPTY;
    private Point nBackPoint;
    private int matched;
    private int mismatched;
    private int timeLapse;
    private boolean initializedParams = false;
    private State state = State.STOPPED;
    private long startDelay;
    private State stateAfterDelay;
    private int currentCycle;
    private boolean cycleMatched;
    private State stateBeforePause;
    private long timePaused;
    private State stateAfterMatch;
    private boolean matchPushed;

    public Game(Observer observer) {
        this.observer = observer;
        addObserver(observer);
    }

    public Point getRandomPoint() {
        Random random = new Random();
        return new Point(random.nextInt(FIELD_SIZE), random.nextInt(FIELD_SIZE));
    }

    public void start() {
        initialize();
        startGameLoop();
    }

    private void startGameLoop() {
        state = State.SHOWING_POINT;
        currentCycle = 0;
        while (true) {
            update();
            if (state == State.STOPPED) {
                notifyStateChanged();
                break;
            }
        }
    }

    private void update() {
        switch (state) {

            case SHOWING_POINT:
                stateAfterMatch = State.HIDING_POINT;
                currentCycle++;
                cycleMatched = false;
                matchPushed = false;
                if (currentCycle > GAME_CYCLES) {
                    gameOver();
                    break;
                }
                showPoint();
                delay(State.HIDING_POINT);
                break;

            case HIDING_POINT:
                stateAfterMatch = State.SHOWING_POINT;
                hidePoint();
                if (!cycleMatched) {
                    delay(State.MATCHING);
                } else {
                    delay(State.SHOWING_POINT);
                }
                break;

            case DELAYED:
                handleDelay();
                break;

            case PAUSED:
//                try {
//                    wait();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                break;

            case MATCHING:
                if (!cycleMatched) {
                    cycleMatched = true;
                    checkMatch();
                    matchPushed = false;
                }
                stateAfterDelay = stateAfterMatch;
                state = State.DELAYED;
                break;

            case STOPPED:
                break;
        }
    }

    private void handleDelay() {
        if (System.currentTimeMillis() - startDelay >= 0.5 * timeLapse) {
            state = stateAfterDelay;
        }
    }

    private void gameOver() {
        state = State.STOPPED;
        Log.d(LOG_TAG, "STOPPED");
        notifyStateChanged();
    }

    private void delay(State state) {
        this.state = State.DELAYED;
        startDelay = System.currentTimeMillis();
        stateAfterDelay = state;
    }

    private void hidePoint() {
        state = State.HIDING_POINT;
        Log.d(LOG_TAG, "HIDING_POINT");
        notifyStateChanged();
    }

    private void showPoint() {
        currentPoint = getRandomPoint();
        Log.d(LOG_TAG, "currentPoint x = " + currentPoint.x + " y = " + currentPoint.y);
        moves.add(currentPoint);
        if (moves.size() > level)
        {
            nBackPoint = moves.remove();
        }
        state = State.SHOWING_POINT;
        Log.d(LOG_TAG, "SHOWING_POINT");
        notifyStateChanged();
    }

    public void initializeParams(int level, TimeLapse timeLapse){
        this.level = level;
        switch (timeLapse){
            case FAST:
                this.timeLapse = TIME_LAPSE_FAST;
                break;
            case MEDIUM:
                this.timeLapse = TIME_LAPSE_MIDDLE;
                break;
            case SLOW:
                this.timeLapse = TIME_LAPSE_SLOW;
                break;
            default:
                this.timeLapse = TIME_LAPSE_SLOW;
        }
        initializedParams = true;
    }

    private void initialize() {
        if (!initializedParams) {
            throw new IllegalStateException();
        }
        nBackPoint = null;
        matched = 0;
        mismatched = 0;
        moves.clear();
    }

    private void notifyStateChanged() {
        final GameDto gameDto = toDto();
        ((Fragment) observer).getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setChanged();
                notifyObservers(gameDto);
            }
        });
    }

    public void stop() {
        state = State.STOPPED;
    }

    public void match() {
        if (!cycleMatched) {
            state = State.MATCHING;
            matchPushed = true;
        }
    }

    public void pause() {
        if (state != State.PAUSED) {
            stateBeforePause = state;
            timePaused = System.currentTimeMillis();
            state = State.PAUSED;
        } else {
            state = stateBeforePause;
            if (state == State.DELAYED) {
                startDelay += System.currentTimeMillis() - timePaused;
            }
        }
        notifyStateChanged();
    }

    @Override
    public void run() {
        start();
    }

    private void checkMatch() {
        if (nBackPoint != null) {
            Log.d(LOG_TAG, "nBackPoint x = " + nBackPoint.x + " y = " + nBackPoint.y);
            Log.d(LOG_TAG, "MATCHING");
            if (nBackPoint.equals(currentPoint) == matchPushed) {
                Log.d(LOG_TAG, "Match!");
                lastMatch = Matched.MATCH;
                matched++;
            } else {
                Log.d(LOG_TAG, "Mismatch!");
                lastMatch = Matched.MISMATCH;
                mismatched++;
            }
            notifyStateChanged();
        }
    }

    private GameDto toDto(){
        return new GameDto(currentPoint, matched, mismatched, state, lastMatch);
    }

    public State getState() {
        return state;
    }
}

enum Matched {
    EMPTY,
    MATCH,
    MISMATCH
}

enum TimeLapse {
    FAST,
    MEDIUM,
    SLOW
}

enum State {
    SHOWING_POINT,
    HIDING_POINT,
    DELAYED,
    MATCHING,
    PAUSED,
    STOPPED
}