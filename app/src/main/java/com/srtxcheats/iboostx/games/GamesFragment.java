package com.srtxcheats.iboostx.games;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.srtxcheats.iboostx.R;
import com.srtxcheats.iboostx.ui.SuzukiSetupActivity;

import java.util.List;

public class GamesFragment extends Fragment implements GameAdapter.Listener {

    private GameManager gameManager;
    private GameAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_games, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        gameManager = new GameManager(requireContext());
        adapter = new GameAdapter(this);

        RecyclerView recyclerView = view.findViewById(R.id.gamesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.addGameButton).setOnClickListener(v -> showAddGameDialog());

        refresh();
    }

    private void refresh() {
        adapter.submitList(gameManager.scanInstalledGames());
    }

    private void showAddGameDialog() {
        List<Game> allApps = gameManager.getAllLaunchableApps();
        String[] labels = new String[allApps.size()];
        for (int i = 0; i < allApps.size(); i++) labels[i] = allApps.get(i).label;

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, labels);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add a game manually")
                .setAdapter(arrayAdapter, (dialog, which) -> {
                    Game picked = allApps.get(which);
                    gameManager.addManualGame(picked.packageName);
                    Toast.makeText(requireContext(), picked.label + " added", Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onLaunch(Game game) {
        if (!gameManager.launch(game.packageName)) {
            Toast.makeText(requireContext(), "Couldn't launch " + game.label, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onOpenSuzukiSetup(Game game) {
        Intent intent = new Intent(requireContext(), SuzukiSetupActivity.class);
        intent.putExtra(SuzukiSetupActivity.EXTRA_GAME_ID, game.packageName);
        intent.putExtra(SuzukiSetupActivity.EXTRA_GAME_NAME, game.label);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }
}
