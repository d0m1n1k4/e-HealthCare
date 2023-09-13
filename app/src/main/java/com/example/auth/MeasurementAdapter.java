package com.example.auth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.widget.TextView;

public class MeasurementAdapter extends RecyclerView.Adapter<MeasurementAdapter.MeasurementViewHolder> {

    private List<Measurement> measurements;

    public MeasurementAdapter(List<Measurement> measurements) {
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
        Measurement measurement = measurements.get(position);
        holder.measurementTextView.setText(measurement.getMeasurementName());
    }

    @Override
    public int getItemCount() {
        return measurements.size();
    }

    public class MeasurementViewHolder extends RecyclerView.ViewHolder {
        public TextView measurementTextView;
        public EditText manualTetnoEditText;
        public EditText manualGlukozaEditText;

        public MeasurementViewHolder(View view) {
            super(view);
            measurementTextView = view.findViewById(R.id.measurementTextView);
            manualTetnoEditText = view.findViewById(R.id.manualTetnoEditText);
            manualGlukozaEditText = view.findViewById(R.id.manualGlukozaEditText);
        }
    }
}
