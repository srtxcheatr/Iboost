package com.srtxcheats.iboostx.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.srtxcheats.iboostx.R;
import com.srtxcheats.iboostx.core.SensitivityProfile;
import com.srtxcheats.iboostx.games.Game;
import com.srtxcheats.iboostx.games.GameManager;

import java.util.List;
import java.util.Locale;

public class SuzukiSetupActivity extends Activity {

    public static final String EXTRA_GAME_ID = "extra_game_id";
    public static final String EXTRA_GAME_NAME = "extra_game_name";

    private ViewFlipper flipper;
    private TextView stepIndicator, step2GameName, sensXValue, sensYValue, reviewText, backButton, nextButton;
    private ListView gameListView;
    private RadioGroup sensitivityModeGroup, controlLayoutGroup;
    private SeekBar sensXSeekBar, sensYSeekBar;

    private GameManager gameManager;
    private List<Game> games;
    private String selectedGameId;
    private String selectedGameName = "—";
    private int currentStep = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suzuki_setup);

        gameManager = new GameManager(this);

        flipper = findViewById(R.id.stepFlipper);
        stepIndicator = findViewById(R.id.stepIndicator);
        gameListView = findViewById(R.id.gameListView);
        step2GameName = findViewById(R.id.step2GameName);
        sensitivityModeGroup = findViewById(R.id.sensitivityModeGroup);
        sensXValue = findViewById(R.id.sensXValue);
        sensYValue = findViewById(R.id.sensYValue);
        sensXSeekBar = findViewById(R.id.sensXSeekBar);
        sensYSeekBar = findViewById(R.id.sensYSeekBar);
        controlLayoutGroup = findViewById(R.id.controlLayoutGroup);
        reviewText = findViewById(R.id.reviewText);
        backButton = findViewById(R.id.backButton);
        nextButton = findViewById(R.id.nextButton);

        selectedGameId = getIntent().getStringExtra(EXTRA_GAME_ID);
        selectedGameName = getIntent().getStringExtra(EXTRA_GAME_NAME);

        setupStep1();
        setupStep2Listeners();
        setupStep3Listeners();

        backButton.setOnClickListener(v -> goToStep(currentStep - 1));
        nextButton.setOnClickListener(v -> onNextPressed());

        // If a game was already chosen (opened via long-press in Games tab),
        // skip straight to step 2.
        if (selectedGameId != null) {
            goToStep(1);
        } else {
            goToStep(0);
        }
    }

    private void setupStep1() {
        games = gameManager.scanInstalledGames();
        String[] labels = new String[games.size()];
        for (int i = 0; i < games.size(); i++) labels[i] = games.get(i).label;
        gameListView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));
        gameListView.setOnItemClickListener((parent, view, position, id) -> {
            Game g = games.get(position);
            selectedGameId = g.packageName;
            selectedGameName = g.label;
            goToStep(1);
        });
    }

    private void setupStep2Listeners() {
        sensitivityModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int x, y;
            if (checkedId == R.id.modePrecision) {
                x = 100; y = 100;
            } else if (checkedId == R.id.modeBalanced) {
                x = 150; y = 150;
            } else if (checkedId == R.id.modeRapid) {
                x = 250; y = 250;
            } else {
                return; // Custom: leave sliders as the user left them
            }
            sensXSeekBar.setProgress(x);
            sensYSeekBar.setProgress(y);
        });
    }

    private void setupStep3Listeners() {
        sensXSeekBar.setOnSeekBarChangeListener(new SimpleSeekListener(progress -> sensXValue.setText(progress + "%")));
        sensYSeekBar.setOnSeekBarChangeListener(new SimpleSeekListener(progress -> sensYValue.setText(progress + "%")));
    }

    private void onNextPressed() {
        if (currentStep == 0) {
            Toast.makeText(this, "Pick a game from the list", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentStep < 3) {
            goToStep(currentStep + 1);
        } else {
            saveProfile();
        }
    }

    private void goToStep(int step) {
        currentStep = step;
        flipper.setDisplayedChild(step);

        String[] titles = {
                "Step 1 of 4 — Choose game",
                "Step 2 of 4 — Sensitivity mode",
                "Step 3 of 4 — Configure controls",
                "Step 4 of 4 — Save profile"
        };
        stepIndicator.setText(titles[step]);
        backButton.setVisibility(step == 0 ? android.view.View.INVISIBLE : android.view.View.VISIBLE);
        nextButton.setText(step == 3 ? "Save profile" : "Next");

        if (step == 1) {
            step2GameName.setText(selectedGameName != null ? selectedGameName : "—");
        }
        if (step == 3) {
            populateReview();
        }
    }

    private void populateReview() {
        int x = sensXSeekBar.getProgress();
        int y = sensYSeekBar.getProgress();
        String layout = controlLayoutGroup.getCheckedRadioButtonId() == R.id.layoutClaw ? "Claw grip"
                : controlLayoutGroup.getCheckedRadioButtonId() == R.id.layoutTwoThumb ? "Two-thumb" : "Default grip";

        reviewText.setText(String.format(Locale.US,
                "Game: %s\nSensitivity X: %d%%\nSensitivity Y: %d%%\nControl layout: %s",
                selectedGameName, x, y, layout));
    }

    private void saveProfile() {
        SensitivityProfile profile = new SensitivityProfile();
        profile.gameId = selectedGameId;
        profile.gameName = selectedGameName;
        profile.xSensitivity = sensXSeekBar.getProgress();
        profile.ySensitivity = sensYSeekBar.getProgress();
        profile.controlLayout = controlLayoutGroup.getCheckedRadioButtonId() == R.id.layoutClaw ? "claw"
                : controlLayoutGroup.getCheckedRadioButtonId() == R.id.layoutTwoThumb ? "two_thumb" : "default";

        try {
            profile.save(this);
            Toast.makeText(this, "Profile saved for " + selectedGameName, Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** Small helper so each SeekBar only needs to supply the "progress changed" behavior. */
    private static class SimpleSeekListener implements SeekBar.OnSeekBarChangeListener {
        interface OnChange { void onChange(int progress); }
        private final OnChange onChange;

        SimpleSeekListener(OnChange onChange) { this.onChange = onChange; }

        @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { onChange.onChange(progress); }
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    }
}
