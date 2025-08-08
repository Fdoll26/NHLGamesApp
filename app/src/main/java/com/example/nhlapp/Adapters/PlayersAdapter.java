package com.example.nhlapp.Adapters;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhlapp.Objects.NHLPlayer;
import com.example.nhlapp.Objects.Player;
import com.example.nhlapp.R;

import java.io.File;
import java.util.List;

public class PlayersAdapter extends RecyclerView.Adapter<PlayersAdapter.PlayerViewHolder> {
    private List<NHLPlayer> players;
    private Context context;

    public PlayersAdapter(List<NHLPlayer> players, Context context) {
        this.players = players;
        this.context = context;
    }

    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_player, parent, false);
        return new PlayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        NHLPlayer player = players.get(position);
        holder.playerName.setText(player.getName());

        // Load player headshot if available
        if (player.getHeadshotPath() != null && !player.getHeadshotPath().isEmpty()) {
            File headshotFile = new File(player.getHeadshotPath());
            if (headshotFile.exists()) {
                holder.playerHeadshot.setImageBitmap(BitmapFactory.decodeFile(player.getHeadshotPath()));
            } else {
                holder.playerHeadshot.setImageResource(R.drawable.ic_player_placeholder);
            }
        } else {
            holder.playerHeadshot.setImageResource(R.drawable.ic_player_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    static class PlayerViewHolder extends RecyclerView.ViewHolder {
        ImageView playerHeadshot;
        TextView playerName;

        public PlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            playerHeadshot = itemView.findViewById(R.id.playerHeadshot);
            playerName = itemView.findViewById(R.id.playerName);
        }
    }
}