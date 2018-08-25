package com.chillingvan.instantvideo.sample.util;

import android.content.Context;

/**
 * Created by Chilling on 2018/8/25.
 */
public class ScreenUtil {

    public static float dpToPx(Context context, float dp) {
        if (context == null) {
            return -1;
        }
        return dp * context.getResources().getDisplayMetrics().density;
    }
}
