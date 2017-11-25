package com.app.ballyhoo.crawler.main;

import android.graphics.drawable.BitmapDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.ballyhoo.crawler.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Viet on 20.11.2017.
 */

public class ShoutsAdapter extends RecyclerView.Adapter<ShoutsAdapter.ShoutsViewHolder> {
    private List<Shout> shouts;

    public ShoutsAdapter() {
        shouts = new ArrayList<>();
    }

    public void setShouts(Collection<Shout> shouts) {
        this.shouts = new ArrayList<>(shouts);
        notifyDataSetChanged();
    }

    @Override
    public ShoutsAdapter.ShoutsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.shout_preview, parent, false);
        return new ShoutsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ShoutsAdapter.ShoutsViewHolder holder, int position) {
        holder.setModel(shouts.get(position));
    }

    @Override
    public int getItemCount() {
        return shouts.size();
    }

    class ShoutsViewHolder extends RecyclerView.ViewHolder {
        TextView id, title, description, time, location;
        ImageView image;

        ShoutsViewHolder(View itemView) {
            super(itemView);

            id = itemView.findViewById(R.id.id);
            title = itemView.findViewById(R.id.title);
            description = itemView.findViewById(R.id.message);
            time = itemView.findViewById(R.id.time);
            image = itemView.findViewById(R.id.image);
            location = itemView.findViewById(R.id.location);
        }

        void setModel(Shout shout) {
            id.setText(""+shout.getId());
            time.setText(shout.getStartDate() + " " + shout.getEndDate());
            title.setText(shout.getTitle());
            description.setText(shout.getMessage());
            image.setImageDrawable(new BitmapDrawable(itemView.getContext().getResources(), shout.getImages().get(0)));
            location.setText(shout.getAddress().getLatitude() + " " + shout.getAddress().getLongitude());
        }
    }
}
