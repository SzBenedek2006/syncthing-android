package dev.benedek.syncthingandroid.fragments;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import dev.benedek.syncthingandroid.R;

/**
 * Simply displays a numberpicker and allows easy access to configure it with the public functions.
 */

public class NumberPickerFragment extends Fragment {

    private NumberPicker numberPicker;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        numberPicker = (NumberPicker) inflater.inflate(R.layout.numberpicker_fragment, container, false);
        numberPicker.setWrapSelectorWheel(false);

        return numberPicker;
    }

    public void setOnValueChangedLisenter(NumberPicker.OnValueChangeListener onValueChangeListener){
        numberPicker.setOnValueChangedListener(onValueChangeListener);
    }

    public void updateNumberPicker(int maxValue, int minValue, int currentValue){
        numberPicker.setMaxValue(maxValue);
        numberPicker.setMinValue(minValue);
        numberPicker.setValue(currentValue);
    }
}
