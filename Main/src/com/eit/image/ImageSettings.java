package com.eit.image;

import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import com.eit.R;
import android.widget.*;
import org.opencv.core.Scalar;

/**
 * Created by thaffe on 3/4/14.
 */
public class ImageSettings implements AdapterView.OnItemSelectedListener, View.OnTouchListener, NumberPicker.OnValueChangeListener {
    Button locateBalls, trackSquares;
    Spinner spinner;
    ImageProcessing imgP;
    Activity activity;
    ImageProcessing.ColorRange color;


    int[] colorPickerIds = new int[]{R.id.colormin_0,R.id.colormin_1,R.id.colormin_2,R.id.colormax_0,R.id.colormax_1,R.id.colormax_2};

    public ImageSettings(Activity activity, ImageProcessing imgP) {
        activity.findViewById(R.id.settingsview).bringToFront();
        this.imgP = imgP;
        this.activity = activity;
        locateBalls = (Button) activity.findViewById(R.id.locateBalls);
        locateBalls.setOnTouchListener(this);
        spinner = (Spinner) activity.findViewById(R.id.view_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(activity,
                R.array.views_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);


        for (int colorPickerId : colorPickerIds) {
            NumberPicker np = (NumberPicker)activity.findViewById(colorPickerId);
            np.setMinValue(0);
            np.setMaxValue(255);
            np.setWrapSelectorWheel(false);
            np.setOnValueChangedListener(this);
        }

    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        Log.i(ImageProcessing.TAG, "INDEX:" + i);
        activity.findViewById(R.id.seekbars).setVisibility(i > 0 ? View.VISIBLE : View.GONE);
        imgP.currentView = i;

        if(i > 0){
            color = ImageProcessing.colorRanges[i < 3 ? 0 : 1];
            for (int j = 0; j < colorPickerIds.length; j++) {
                NumberPicker np = (NumberPicker)activity.findViewById(colorPickerIds[j]);
                double val = j < 3 ? color.minHsv.val[j] : color.maxHsv.val[j-3];
                np.setValue((int)val);
            }

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        Log.i(ImageProcessing.TAG, "START TO LOCATE BALLS");
        imgP.startBallDetection();

        return false;
    }

    @Override
    public void onValueChange(NumberPicker numberPicker, int i, int i2) {
        int index;
        for (index = 0; index < colorPickerIds.length; index++) {
            if (colorPickerIds[index] == numberPicker.getId()) break;
        }

        if (index < 6) {
            int value = numberPicker.getValue();
            if(index < 3){
                color.minHsv.val[index] = value;
                Log.i(ImageProcessing.TAG, "MIN COLOR CHANGE"+value);
            }else{
                color.maxHsv.val[index - 3] = value;
                Log.i(ImageProcessing.TAG, "MAX COLOR CHANGE"+value);
            }
        }
    }
}
