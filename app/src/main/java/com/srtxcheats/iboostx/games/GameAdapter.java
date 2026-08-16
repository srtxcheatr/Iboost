package com.srtxcheats.iboostx.games;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.srtxcheats.iboostx.R;

import java.util.ArrayList;
import java.util.List;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.ViewHolder> {

    public interface Listener {
        void onLaunch(Game game);
        void onOpenSuzukiSetup(Game game);
    }

    private final List<Game> games = new ArrayList<>();
    private final Listener listener;

    public GameAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<Game> newGames) {
        games.clear();
        games.addAll(newGames);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_game, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Game game = games.get(position);
        holder.name.setText(game.label);
        holder.pkg.setText(game.packageName);
        holder.icon.setImageDrawable(game.icon);
        holder.launch.setOnClickListener(v -> listener.onLaunch(game));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onOpenSuzukiSetup(game);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return games.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name, pkg, launch;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.gameIcon);
            name = itemView.findViewById(R.id.gameName);
            pkg = itemView.findViewById(R.id.gamePackage);
            launch = itemView.findViewById(R.id.gameLaunch);
        }
    }
}
