package com.nback.app;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.support.v4.app.Fragment;
//import android.app.Fragment;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
//import android.widget.GridLayout;
import android.support.v7.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Observable;
import java.util.Observer;

/**
 * Created by dds on 17.06.15.
 */
public class GameFragment extends Fragment implements Observer {
    private final static String LOG_TAG = GameFragment.class.getSimpleName();
    private Game game;
    private GameDto gameDto;
    private GridLayout gameGrid;
    private TextView result;
    private Animation animationShow;
    private Animation animationHide;
    private AnimatorSet positionAnimatorGreen;
    private AnimatorSet positionAnimatorRed;
    private Button btnPosition;
    private Button btnStartPause;
    private boolean positionPushed = false;

    @Override
    public void onStart() {
        super.onStart();
//        gameGrid = (GridLayout) getActivity().findViewById(R.id.gameGrid);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_game, container, false);
        Fragment fragment = this;
        game = new Game((Observer) fragment);
        gameGrid = (GridLayout) rootView.findViewById(R.id.gameGrid);
        gameGrid.setColumnCount(game.FIELD_SIZE);
        gameGrid.setRowCount(game.FIELD_SIZE);
        gameGrid.setUseDefaultMargins(true);

        View view;
        for (int i = 0; i < game.FIELD_SIZE; i++) {
            for (int j = 0; j < game.FIELD_SIZE; j++) {
                view = inflater.inflate(R.layout.grid_cell, null);
                gameGrid.addView(view); //, i * game.FIELD_SIZE + j);
            }
        }

        btnStartPause = (Button) rootView.findViewById(R.id.btnStartPause);
        btnStartPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (game.getState() == State.STOPPED) {
                    game.initializeParams(2, TimeLapse.SLOW);
                    new Thread(game).start();
                    v.setKeepScreenOn(true);
                    btnStartPause.setText("PAUSE");
                } else if (game.getState() == State.PAUSED) {
                    v.setKeepScreenOn(true);
                    btnStartPause.setText("PAUSE");
                    game.pause();
                } else {
                    v.setKeepScreenOn(false);
                    btnStartPause.setText("RESUME");
                    game.pause();
                }
            }
        });

        rootView.findViewById(R.id.btnStop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                game.stop();
                hidePoint();
            }
        });

        btnPosition = (Button) rootView.findViewById(R.id.btnPosition);
        positionAnimatorGreen = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.flash_green);
        positionAnimatorGreen.setTarget(btnPosition);
        positionAnimatorRed = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.flash_red);
        positionAnimatorRed.setTarget(btnPosition);

        btnPosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                game.match();
                positionPushed = true;
            }
        });

        result = (TextView) rootView.findViewById(R.id.tvResult);

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        animationShow = AnimationUtils.loadAnimation(getActivity(), R.anim.point_show);
        animationHide = AnimationUtils.loadAnimation(getActivity(), R.anim.point_hide);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    @Override
    public void update(Observable observable, Object data) {
        gameDto = (GameDto) data;
        if (gameDto.getState() == State.SHOWING_POINT) {
            Log.d(LOG_TAG, "SHOWING_POINT");
            showPoint();
        } else if (gameDto.getState() == State.HIDING_POINT) {
            Log.d(LOG_TAG, "HIDING_POINT");
            hidePoint();
        } else if (gameDto.getState() == State.MATCHING) {
            Log.d(LOG_TAG, "MATCHING");
            updateResult();
        } else if (gameDto.getState() == State.PAUSED) {
            Toast.makeText(getActivity(), "GAME PAUSED!", Toast.LENGTH_LONG).show();
            Log.d(LOG_TAG, "PAUSED");
        } else if (gameDto.getState() == State.STOPPED) {
            Log.d(LOG_TAG, "STOPPED");
            result.setText(String.format("GAME OVER! R: %s W: %s", gameDto.getMatched(), gameDto.getMismatched()));
            btnStartPause.setText("START");
            getView().setKeepScreenOn(false);
        }
    }

    private void updateResult() {
        Matched matched = gameDto.getLastMatch();
        if (matched == Matched.MATCH) {
            result.setTextColor(getResources().getColor(R.color.green));
            if (positionPushed) {
                flashButtonGreen();
                positionPushed = false;
            }
        } else if (matched == Matched.MISMATCH) {
            result.setTextColor(getResources().getColor(R.color.red));
            if (positionPushed) {
                flashButtonRed();
                positionPushed = false;
            }
        } else {
            result.setTextColor(getResources().getColor(R.color.black));
        }
        result.setText(String.format("R: %s W: %s", gameDto.getMatched(), gameDto.getMismatched()));
    }

    private void showPoint() {
        getViewAtPoint(gameDto.getCurrentPoint()).startAnimation(animationShow);
        getViewAtPoint(gameDto.getCurrentPoint()).setVisibility(View.VISIBLE);
    }

    private void hidePoint() {
        getViewAtPoint(gameDto.getCurrentPoint()).startAnimation(animationHide);
        getViewAtPoint(gameDto.getCurrentPoint()).setVisibility(View.INVISIBLE);
    }

    private View getViewAtPoint(Point point) {
        return gameGrid.getChildAt(point.x * game.FIELD_SIZE + point.y).findViewWithTag("btn");
    }

    private void flashButtonGreen() {
        positionAnimatorGreen.start();
    }

    private void flashButtonRed() {
        positionAnimatorRed.start();
    }
}
