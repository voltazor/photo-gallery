package com.voltazor.myapplication;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.view.View;

public class AnimatorCompatHelper {
    private static TimeInterpolator mDefaultInterpolator;

    public static void clearInterpolator(View view) {
        if (view != null) {
            if (mDefaultInterpolator == null) {
                mDefaultInterpolator = new ValueAnimator().getInterpolator();
            }
            view.animate().setInterpolator(mDefaultInterpolator);
        }
    }
}