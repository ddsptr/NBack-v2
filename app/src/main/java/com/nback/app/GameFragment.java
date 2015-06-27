package com.nback.app;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.support.v4.app.Fragment;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
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

    private Game mGame;
    private GameDto mGameDto;
    private GridLayout mGameGrid;
    private TextView mResult;
    private Animation mAnimationShow;
    private Animation mAnimationHide;
    private AnimatorSet mPositionAnimatorGreen;
    private AnimatorSet mPositionAnimatorRed;
    private Button mButtonPosition;
    private Button mButtonStartPause;
    private boolean mPositionPushed = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_game, container, false);
        Fragment fragment = this;
        mGame = new Game((Observer) fragment);
        mGameGrid = (GridLayout) rootView.findViewById(R.id.gameGrid);
        mGameGrid.setColumnCount(mGame.getFieldSize());
        mGameGrid.setRowCount(mGame.getFieldSize());
        mGameGrid.setUseDefaultMargins(true);

        View view;
        for (int i = 0; i < mGame.getFieldSize(); i++) {
            for (int j = 0; j < mGame.getFieldSize(); j++) {
                view = inflater.inflate(R.layout.grid_cell, null);
                mGameGrid.addView(view);
            }
        }

        mButtonStartPause = (Button) rootView.findViewById(R.id.btnStartPause);
        mButtonStartPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mGame.getState() == State.STOPPED) {
                    mGame.initializeParams(2, TimeLapse.SLOW);
                    new Thread(mGame).start();
                    getView().setKeepScreenOn(true);
                    mButtonStartPause.setText(R.string.pause);
                } else if (mGame.getState() == State.PAUSED) {
                    getView().setKeepScreenOn(true);
                    mButtonStartPause.setText(R.string.pause);
                    mGame.resume();
                } else {
                    getView().setKeepScreenOn(false);
                    mButtonStartPause.setText(R.string.resume);
                    pause();
                }
            }
        });

        rootView.findViewById(R.id.btnStop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGame.stop();
                if (mGameDto != null) {
                    hidePoint();
                }
            }
        });

        mButtonPosition = (Button) rootView.findViewById(R.id.btnPosition);
        mPositionAnimatorGreen = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.flash_green);
        mPositionAnimatorGreen.setTarget(mButtonPosition);
        mPositionAnimatorRed = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.flash_red);
        mPositionAnimatorRed.setTarget(mButtonPosition);

        mButtonPosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGame.match();
                mPositionPushed = true;
            }
        });

        mResult = (TextView) rootView.findViewById(R.id.tvResult);

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAnimationShow = AnimationUtils.loadAnimation(getActivity(), R.anim.point_show);
        mAnimationHide = AnimationUtils.loadAnimation(getActivity(), R.anim.point_hide);
    }

    public void pause() {
        if (mGame != null) {
            mGame.pause();
            ((GameActivity) getActivity()).show();
        }
    }

    @Override
    public void update(Observable observable, Object data) {
        mGameDto = (GameDto) data;
        if (mGameDto.getState() == State.SHOWING_POINT) {
            Log.d(LOG_TAG, "SHOWING_POINT");
            showPoint();
        } else if (mGameDto.getState() == State.HIDING_POINT) {
            Log.d(LOG_TAG, "HIDING_POINT");
            hidePoint();
        } else if (mGameDto.getState() == State.MATCHING) {
            Log.d(LOG_TAG, "MATCHING");
            updateResult();
        } else if (mGameDto.getState() == State.PAUSED) {
            Toast toast = Toast.makeText(getActivity(), "GAME PAUSED!", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0 ,0);
            toast.show();
            Log.d(LOG_TAG, "PAUSED");
        } else if (mGameDto.getState() == State.STOPPED) {
            Log.d(LOG_TAG, "STOPPED");
            mResult.setText(String.format("GAME OVER! R: %s W: %s", mGameDto.getMatched(), mGameDto.getMismatched()));
            mButtonStartPause.setText("START");
            getView().setKeepScreenOn(false);
        }
    }

    private void updateResult() {
        Matched matched = mGameDto.getLastMatch();
        if (matched == Matched.MATCH) {
            mResult.setTextColor(getResources().getColor(R.color.green));
            if (mPositionPushed) {
                flashButtonGreen();
                mPositionPushed = false;
            }
        } else if (matched == Matched.MISMATCH) {
            mResult.setTextColor(getResources().getColor(R.color.red));
            if (mPositionPushed) {
                flashButtonRed();
                mPositionPushed = false;
            }
        } else {
            mResult.setTextColor(getResources().getColor(R.color.black));
        }
        mResult.setText(String.format("R: %s W: %s", mGameDto.getMatched(), mGameDto.getMismatched()));
    }

    private void showPoint() {
        getViewAtPoint(mGameDto.getCurrentPoint()).startAnimation(mAnimationShow);
        getViewAtPoint(mGameDto.getCurrentPoint()).setVisibility(View.VISIBLE);
    }

    private void hidePoint() {
        getViewAtPoint(mGameDto.getCurrentPoint()).startAnimation(mAnimationHide);
        getViewAtPoint(mGameDto.getCurrentPoint()).setVisibility(View.INVISIBLE);
    }

    private View getViewAtPoint(Point point) {
        return mGameGrid.getChildAt(point.x * mGame.getFieldSize() + point.y).findViewWithTag("btn");
    }

    private void flashButtonGreen() {
        mPositionAnimatorGreen.start();
    }

    private void flashButtonRed() {
        mPositionAnimatorRed.start();
    }
}
