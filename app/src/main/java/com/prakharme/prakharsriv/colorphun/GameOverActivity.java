package com.prakharme.prakharsriv.colorphun;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.Leaderboards;
import com.google.example.games.basegameutils.BaseGameActivity;

public class GameOverActivity extends BaseGameActivity {

    private int points, best;
    private boolean newScore;
    private boolean shown = false;
    private TextView gameOverText, pointsBox, highScoreText;

    final int REQUEST_LEADERBOARD = 4000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);

        gameOverText = (TextView) findViewById(R.id.game_over);
        TextView levelIndicator = (TextView) findViewById(R.id.level_indicator);
        pointsBox = (TextView) findViewById(R.id.points_box);
        TextView bestLabel = (TextView) findViewById(R.id.best_label);
        TextView bestBox = (TextView) findViewById(R.id.best_box);
        highScoreText = (TextView) findViewById(R.id.highscore_txt);
        Button replayBtn = (Button) findViewById(R.id.replay_btn);

        // setting up typeface
        Typeface avenir_black = Typeface.createFromAsset(getAssets(), "fonts/avenir_black.ttf");
        Typeface avenir_book = Typeface.createFromAsset(getAssets(), "fonts/avenir_book.ttf");

        gameOverText.setTypeface(avenir_black);
        levelIndicator.setTypeface(avenir_book);
        pointsBox.setTypeface(avenir_black);
        bestBox.setTypeface(avenir_black);
        bestLabel.setTypeface(avenir_book);
        replayBtn.setTypeface(avenir_book);
        highScoreText.setTypeface(avenir_black);

        // disallow auto sign-in on this screen
        getGameHelper().setMaxAutoSignInAttempts(0);

        // get data
        Bundle bundle = getIntent().getExtras();
        points = bundle.getInt("points");
        int level = bundle.getInt("level");
        best = bundle.getInt("best");
        newScore = bundle.getBoolean("newScore");

        // set data
        pointsBox.setText(String.format("%03d", points));
        bestBox.setText(String.format("%03d", best));
        levelIndicator.setText("Level " + Integer.toString(level));

        // show high score
        if (newScore) {
            highScoreText.setVisibility(View.VISIBLE);
        } else {
            highScoreText.setVisibility(View.INVISIBLE);
        }
    }


    void pushAccomplishments() {
        if (!isSignedIn()) {
            return;
        }
        if (best > 0) {
            // submit score to play services
            Games.Leaderboards.submitScore(getApiClient(),
                    getString(R.string.LEADERBOARD_ID) , best);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus && !shown) {
            shown = true;
            ValueAnimator pointsAnim = getCounterAnimator(pointsBox, points);
            pointsAnim.setDuration(1200);

            // animate high score text
            if (newScore) {
                ObjectAnimator highScoreAnim = ObjectAnimator.ofFloat(highScoreText, "alpha", 0f, 1f);
                highScoreAnim.setDuration(600);
                highScoreAnim.start();
            }

            pointsAnim.start();
        }
    }

    ValueAnimator getCounterAnimator(final TextView view, final int maxValue) {
        ValueAnimator anim = ValueAnimator.ofInt(0, 1);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (int) (maxValue * valueAnimator.getAnimatedFraction());
                view.setText(String.format("%03d", val));
            }
        });
        return anim;
    }

    public void playGame(View view) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public void showLeaderboard(View view) {
        if (isSignedIn()) {
            startActivityForResult(Games.Leaderboards.getLeaderboardIntent(getApiClient(),
                    getString(R.string.LEADERBOARD_ID)), REQUEST_LEADERBOARD);
        } else {
            Toast.makeText(this, R.string.signin_help, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSignInFailed() {
        Log.e("SIGN IN", "ERROR Signin in game over");
    }

    @Override
    public void onSignInSucceeded() {
        // save scores on the cloud
        pushAccomplishments();

        // fetching results from leaderboard and matching scores
        PendingResult result = Games.Leaderboards.loadCurrentPlayerLeaderboardScore(getApiClient(),
                getString(R.string.LEADERBOARD_ID), LeaderboardVariant.TIME_SPAN_ALL_TIME,
                LeaderboardVariant.COLLECTION_PUBLIC);

        result.setResultCallback(new ResultCallback<Leaderboards.LoadPlayerScoreResult>() {
            @Override
            public void onResult(Leaderboards.LoadPlayerScoreResult result) {
                // check if valid score
                if (result != null
                        && GamesStatusCodes.STATUS_OK == result.getStatus().getStatusCode()
                        && result.getScore() != null) {

                    // assign score fetched as best score
                    updateHighScore((int) result.getScore().getRawScore());
                }
            }

        });

    }

    // save high score in shared preferences file
    private void updateHighScore(int score) {
        if (score != best && score > 0) {
            SharedPreferences sharedPreferences = this.getSharedPreferences(
                    getString(R.string.preference_file_key), Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("HIGHSCORE", score);
            editor.apply();
        }
    }
}
