package com.nback.app;

import android.graphics.Point;

/**
 * Created by dds on 19.06.15.
 */
public class GameDto {
    private Point currentPoint;
    private int matched;
    private int mismatched;
    private State state;
    private Matched lastMatch;

    public GameDto(Point currentPoint, int matched, int mismatched, State state, Matched lastMatch) {
        this.currentPoint = currentPoint;
        this.matched = matched;
        this.mismatched = mismatched;
        this.state = state;
        this.lastMatch = lastMatch;
    }

    public Point getCurrentPoint() {
        return currentPoint;
    }

    public int getMatched() {
        return matched;
    }

    public int getMismatched() {
        return mismatched;
    }

    public State getState() {
        return state;
    }

    public Matched getLastMatch() {
        return lastMatch;
    }
}
