package com.nback.app;

//import android.app.Fragment;
import android.support.v4.app.Fragment;
import android.graphics.Point;
import android.util.Log;

import java.util.*;

class Game extends Observable implements Runnable{
    private final static String LOG_TAG = Game.class.getSimpleName();
    private static final int FIELD_SIZE = 3;
    public static final int GAME_CYCLES = 20;
    public static final int TIME_LAPSE_FAST = (int)(1 * 1000);
    public static final int TIME_LAPSE_MIDDLE = (int)(2 * 1000);
    public static final int TIME_LAPSE_SLOW = (int)(3 * 1000);

    private Point mCurrentPoint;
    private int mLevel;
    private Observer mObserver;
    private Queue<Point> mMoves = new LinkedList<Point>();
    private Matched mLastMatch = Matched.EMPTY;
    private Point mNBackPoint;
    private int mMatched;
    private int mMismatched;
    private int mTimeLapse;
    private boolean mInitializedParams = false;
    private State mState = State.STOPPED;
    private long mStartDelay;
    private State mStateAfterDelay;
    private int mCurrentCycle;
    private boolean mCycleMatched;
    private State mStateBeforePause;
    private long mTimePaused;
    private State mStateAfterMatch;
    private boolean mMatchPushed;
    private int mFieldSize;

    public Game(Observer observer) {
        mObserver = observer;
        addObserver(observer);
        mFieldSize = FIELD_SIZE;
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
        mState = State.SHOWING_POINT;
        mCurrentCycle = 0;
        while (true) {
            update();
            if (mState == State.STOPPED) {
                notifyStateChanged();
                break;
            }
        }
    }

    private void update() {
        switch (mState) {

            case SHOWING_POINT:
                handleShowingPoint();
                break;

            case HIDING_POINT:
                handleHidingPoint();
                break;

            case DELAYED:
                handleDelay();
                break;

            case PAUSED:
                handlePaused();
                break;

            case MATCHING:
                handleMatching();
                break;

            case STOPPED:
                break;
        }
    }

    private void handleMatching() {
        if (!mCycleMatched) {
            mCycleMatched = true;
            checkMatch();
            mMatchPushed = false;
        }
        mStateAfterDelay = mStateAfterMatch;
        mState = State.DELAYED;
    }

    private void handlePaused() {
        synchronized (this) {
            while (mState == State.PAUSED) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleHidingPoint() {
        mStateAfterMatch = State.SHOWING_POINT;
        hidePoint();
        if (!mCycleMatched) {
            delay(State.MATCHING);
        } else {
            delay(State.SHOWING_POINT);
        }
        return;
    }

    private void handleShowingPoint() {
        mStateAfterMatch = State.HIDING_POINT;
        mCurrentCycle++;
        mCycleMatched = false;
        mMatchPushed = false;
        if (mCurrentCycle > GAME_CYCLES) {
            gameOver();
            return;
        }
        showPoint();
        delay(State.HIDING_POINT);
        return;
    }

    private void handleDelay() {
        if (System.currentTimeMillis() - mStartDelay >= 0.5 * mTimeLapse) {
            mState = mStateAfterDelay;
        }
    }

    private void gameOver() {
        mState = State.STOPPED;
        Log.d(LOG_TAG, "STOPPED");
        notifyStateChanged();
    }

    private void delay(State state) {
        mState = State.DELAYED;
        mStartDelay = System.currentTimeMillis();
        mStateAfterDelay = state;
    }

    private void hidePoint() {
        mState = State.HIDING_POINT;
        Log.d(LOG_TAG, "HIDING_POINT");
        notifyStateChanged();
    }

    private void showPoint() {
        mCurrentPoint = getRandomPoint();
        Log.d(LOG_TAG, "mCurrentPoint x = " + mCurrentPoint.x + " y = " + mCurrentPoint.y);
        mMoves.add(mCurrentPoint);
        if (mMoves.size() > mLevel)
        {
            mNBackPoint = mMoves.remove();
        }
        mState = State.SHOWING_POINT;
        Log.d(LOG_TAG, "SHOWING_POINT");
        notifyStateChanged();
    }

    public void initializeParams(int level, TimeLapse timeLapse){
        mLevel = level;
        switch (timeLapse){
            case FAST:
                mTimeLapse = TIME_LAPSE_FAST;
                break;
            case MEDIUM:
                mTimeLapse = TIME_LAPSE_MIDDLE;
                break;
            case SLOW:
                mTimeLapse = TIME_LAPSE_SLOW;
                break;
            default:
                mTimeLapse = TIME_LAPSE_SLOW;
        }
        mInitializedParams = true;
    }

    private void initialize() {
        if (!mInitializedParams) {
            throw new IllegalStateException();
        }
        mNBackPoint = null;
        mMatched = 0;
        mMismatched = 0;
        mMoves.clear();
    }

    private void notifyStateChanged() {
        final GameDto gameDto = toDto();
        ((Fragment) mObserver).getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setChanged();
                notifyObservers(gameDto);
            }
        });
    }

    public void stop() {
        mState = State.STOPPED;
    }

    public void match() {
        if (!mCycleMatched) {
            mState = State.MATCHING;
            mMatchPushed = true;
        }
    }

    public void pause() {
        if (mState != State.PAUSED) {
            mStateBeforePause = mState;
            mTimePaused = System.currentTimeMillis();
            synchronized (this) {
                mState = State.PAUSED;
            }
        }
        notifyStateChanged();
    }

    public void resume() {
        if (mState == State.PAUSED) {
            synchronized (this) {
                mState = mStateBeforePause;
                if (mState == State.DELAYED) {
                    mStartDelay += System.currentTimeMillis() - mTimePaused;
                }
                this.notify();
            }
        }
        notifyStateChanged();
    }

    @Override
    public void run() {
        start();
    }

    private void checkMatch() {
        if (mNBackPoint != null) {
            Log.d(LOG_TAG, "mNBackPoint x = " + mNBackPoint.x + " y = " + mNBackPoint.y);
            Log.d(LOG_TAG, "MATCHING");
            if (mNBackPoint.equals(mCurrentPoint) == mMatchPushed) {
                Log.d(LOG_TAG, "Match!");
                mLastMatch = Matched.MATCH;
                mMatched++;
            } else {
                Log.d(LOG_TAG, "Mismatch!");
                mLastMatch = Matched.MISMATCH;
                mMismatched++;
            }
            notifyStateChanged();
        }
    }

    private GameDto toDto(){
        return new GameDto(mCurrentPoint, mMatched, mMismatched, mState, mLastMatch);
    }

    public State getState() {
        return mState;
    }

    public int getFieldSize() {
        return mFieldSize;
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