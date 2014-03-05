package com.eit.image;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

/**
 * Created by thaffe on 3/4/14.
 */
public class ImageSettings implements AdapterView.OnItemSelectedListener,View.OnTouchListener{
    Button locateBalls,trackSquares;
    Spinner spinner;
    ImageProcessing imgP;
    public ImageSettings(Activity activity, ImageProcessing imgP){
        this.imgP = imgP;
        locateBalls = (Button)activity.findViewById(R.id.locateBalls);
        spinner = (Spinner)activity.findViewById(R.id.view_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(activity,
                R.array.views_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);


    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }
}
