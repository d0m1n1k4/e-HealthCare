package com.example.auth;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MeasurementAdapter extends RecyclerView.Adapter<MeasurementAdapter.MeasurementViewHolder> {

    private List<Measurement> measurements;

    public MeasurementAdapter(List<Measurement> measurements) {
        this.measurements = measurements;
    }

    @Override
    public MeasurementViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.measure_edit, parent, false);
        return new MeasurementViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MeasurementViewHolder holder, int position) {
        Measurement measurement = measurements.get(position);
        holder.measurementTextView.setText(measurement.getMeasurementName());

        // Obsługa pola Tetno EditText
        holder.manualTetnoEditText.setText(measurement.getTetnoValue());
        holder.manualTetnoEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Aktualizacja wartości tetnoValue w obiekcie Measurement po zmianie
                measurement.setTetnoValue(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // Obsługa pola Glukoza EditText
        holder.manualGlukozaEditText.setText(measurement.getGlukozaValue());
        holder.manualGlukozaEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Aktualizacja wartości glukozaValue w obiekcie Measurement po zmianie
                measurement.setGlukozaValue(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
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

    public void clearUserInputData() {
        for (Measurement measurement : measurements) {
            measurement.setTetnoValue("");
            measurement.setGlukozaValue("");
        }
        notifyDataSetChanged();
    }

    // Metoda do pobierania listy pomiarów
    public List<Measurement> getMeasurements() {
        return measurements;
    }
}