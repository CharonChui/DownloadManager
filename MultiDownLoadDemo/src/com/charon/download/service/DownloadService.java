
package com.charon.download.service;

import com.charon.download.R;
import com.charon.download.engine.DownloadManager;
import com.charon.download.entity.Download;
import com.charon.download.entity.Download.NotificationProgressListener;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * 只包含下载的部分，其他的全部都在DownloadManager中去管理
 * 
 * @author xuchuanren
 */
public class DownloadService extends Service {

    public static final String TAG = "DownloadService";

    private Notification mNotification;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Download currentDownload = DownloadManager.getInstance().getCurrentDownload();
        if (currentDownload != null && !currentDownload.checkDownloading()) {
            Log.e(TAG, "开始去下载当前的任务");
            currentDownload.start();
        } else {
            Log.e(TAG, "已经存在正在下载的任务，不去开启了");
        }

        if (DownloadManager.getInstance().getNotificationVisibility()) {
            showNotification("开始下载", 0, 100);
        }
        
        currentDownload.setNotificationProgressListener(new NotificationProgressListener() {
            
            @Override
            public void onProgress(long mDownloadLength, long mTotalLength) {
                showNotification("开始下载", 0, 100);
            }
        });
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 显示状态栏通知
     * 
     * @param message
     * @param progress
     * @param max
     * @return
     */
    public Notification showNotification(String message, int progress, int max) {
        if (mNotification == null) {
            mNotification = new Notification(
                    android.R.drawable.stat_sys_download, message,
                    System.currentTimeMillis());
            mNotification.flags = mNotification.flags
                    | Notification.FLAG_ONGOING_EVENT;// 不可清除的Notifiction

            PendingIntent contentIntent = PendingIntent.getActivity(DownloadManager.getInstance()
                    .getContext(), 0,
                    new Intent(), 0);
            mNotification.contentIntent = contentIntent;

            mNotification.contentView = new RemoteViews(DownloadManager.getInstance().getContext()
                    .getPackageName(),
                    R.layout.download_notification);
            mNotification.contentView.setImageViewResource(R.id.image,
                    R.drawable.ic_launcher);
            mNotification.contentView.setTextViewText(R.id.title, "正在下载新版凤凰视频...");

        }
        mNotification.contentView.setTextViewText(R.id.percent, progress * 100
                / max + "%");
        mNotification.contentView.setProgressBar(R.id.progress, max, progress,
                false);
        startForeground(DownloadManager.getInstance().getCurrentDownload().getNovel().getID(),
                mNotification);

        return mNotification;
    }

}
