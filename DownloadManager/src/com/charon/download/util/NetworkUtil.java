
package com.charon.download.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkUtil {
    /**
     * 检测是否有网络
     * 
     * @param context
     * @return
     */
    public static boolean isNetAvailable(Context context) {
        boolean isCheckNet = false;
        try {
            final ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo mobNetInfoActivity = connectivityManager
                    .getActiveNetworkInfo();
            if (mobNetInfoActivity == null || !mobNetInfoActivity.isAvailable()) {
                isCheckNet = false;
                return isCheckNet;
            } else {
                isCheckNet = true;
                return isCheckNet;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return isCheckNet;
    }

    /**
     * 判断当前网络是否是手机网络
     * 
     * @param context
     * @return
     */
    public static boolean isMobile(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null
                && activeNetInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
            return true;
        }
        return false;
    }
}
