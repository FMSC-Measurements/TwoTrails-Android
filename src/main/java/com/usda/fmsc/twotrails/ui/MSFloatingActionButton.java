package com.usda.fmsc.twotrails.ui;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.View;

import com.usda.fmsc.android.widget.SheetFab;

public class MSFloatingActionButton extends FloatingActionButton implements SheetFab.AnimatedFab {

    public MSFloatingActionButton(Context context) {
        super(context);
    }

    public MSFloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MSFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Shows the FAB.
     */
    @Override
    public void show() {
        show(0, 0);
    }

    @Override
    public void show(float translationX, float translationY) {
        setVisibility(View.VISIBLE);
    }


    /**
     * Hides the FAB.
     */
    @Override
    public void hide() {
        setVisibility(View.INVISIBLE);
    }
}
