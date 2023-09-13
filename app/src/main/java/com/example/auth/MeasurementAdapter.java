package com.example.auth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MeasurementAdapter extends RecyclerView.Adapter<MeasurementAdapter.MeasurementViewHolder> {

    private List<String> measurements;

    public MeasurementAdapter(List<String> measurements) {
        this.measurements = measurements;
    }

    @Override
    public MeasurementViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.measurement_item, parent, false);
        return new MeasurementViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MeasurementViewHolder holder, int position) {
        String measurement = measurements.get(position);
        holder.measurementTextView.setText(measurement);
    }

    @Override
    public int getItemCount() {
        return measurements.size();
    }

    public class MeasurementViewHolder extends RecyclerView.ViewHolder {
        public TextView measurementTextView;

        public MeasurementViewHolder(View view) {
            super(view);
            measurementTextView = view.findViewById(R.id.measurementTextView);
        }
    }
}