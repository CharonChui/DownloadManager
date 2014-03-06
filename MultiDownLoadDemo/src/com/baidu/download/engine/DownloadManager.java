
package com.baidu.download.engine;

import com.baidu.download.dao.DownloadDao;
import com.baidu.download.entity.Download;
import com.baidu.download.entity.Download.DownloadCompleteListener;
import com.baidu.download.entity.Novel;
import com.baidu.download.service.DownloadService;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

/**
 * 下载的管理类,单例<br/>
 * TODO ...没有实现断点续传的功能
 * 
 * @author xuchuanren
 */
public class DownloadManager {
    private static final String TAG = "DownloadManager";
    private static final DownloadManager mDownloadManager = new DownloadManager();

    /** 下载队列，包括正在下载和等待下载的任务 */
    private List<Download> mDownloadList;

    /** 所有已经下载完成的任务 */
    private List<Download> mDownloadOverList;

    /** 当前正在下载的任务 */
    private Download mDownload;

    private DownloadDao mDownloadDao;

    private DownloadCompleteListener mCompleteListener;

    private Context mContext;

    /**
     * 是否显示状态栏通知
     */
    private boolean mNotificationVisibility;

    /**
     * 当前允许的网络类型
     */
    private Network mAllowedNetworkType = Network.WIFI;

    private DownloadManager() {
        mCompleteListener = new DownloadCompleteListener() {

            @Override
            public void onSuccess(Download download) {
                Log.e(TAG, "onSuccesss");
                mDownloadList.remove(download);
                mDownloadOverList.add(download);
                new DownloadOverTask(download).execute();
            }

            @Override
            public void onFailed(Download download) {
                Log.e(TAG, "onFailed");
                try2DownNextProgram();
            }
        };
    }

    public static DownloadManager getInstance() {
        return mDownloadManager;
    }

    /**
     * 一般在应用启动时调用此方法，来保证没有下载完的任务可以继续下载
     * 
     * @param context
     */
    public void onCreate(Context context) {
        mContext = context.getApplicationContext();
        mDownloadDao = new DownloadDao(context.getApplicationContext());
        mDownloadList = mDownloadDao.getDownloadList();
        mDownloadOverList = mDownloadDao.getDownloadOverList();
        Log.e(TAG, "DownloadList size is :" + mDownloadList.size()
                + " and the DownloadOverList size is ::" + mDownloadOverList.size());
        // 如果需要一开启就自动下载的话可以使用这种方式在Service的onCreate方法中去让其自动下载
        // Intent intent = new Intent(context, DownloadService.class);
        // context.startService(intent);
    }

    /**
     * 在应用退出时进行数据的保存
     * 
     * @param context
     */
    public void onDestroy(Context context) {
        quit();
        Intent intent = new Intent(context, DownloadService.class);
        context.stopService(intent);
    }

    /**
     * 将一个新的下载任务添加到下载队列,在前面任务下载完后会自动开始下载
     * 
     * @param context
     * @param novel
     * @return true为新建下载任务成功，false为已经存在此任务,不管true还是false，这个任务都会被下载
     */
    public boolean enqueue(Context context, Novel novel) {
        if (mDownloadDao.isExistDownload(novel)) {
            Log.d(TAG, "已存在,在未下载的队列中已经有下载的任务了");
            // 判断当前是否有下载的任务，如果没有的话，就去下载去
            return false;
        } else {
            mDownloadDao.insertNewProgram(novel);
            Download atom = new Download(novel, null);
            atom.setCompleteListener(mCompleteListener);
            atom.setDownloadState(Download.STATE_WAIT);
            mDownloadList.add(atom);

            if (mDownload == null || !mDownload.isDownloading()) {
                Log.e(TAG, "当前没有任务在下载，让新建的下载任务区下载");
                mDownload = atom;
                startDownloadService();
            } else if (mDownload.isDownloading() && mDownload.checkDownloading()) {
                Log.e(TAG, "当前已经有别的下载任务在下载了，，，，这个新加的任务要先暂停。。。。  ");
            } else {
                startDownloadService();
            }

            return true;
        }
    }

    /**
     * 暂停，暂停当前的任务，会自动去下载下一个等待中的任务
     * 
     * @return
     */
    public void pause(Download download) {
        if (isDownLoadOver(download))
            return;

        download.pause();
        saveDownloadData(download);
        if (mDownload != null && mDownload.equals(download)) {
            mDownload = null;
            try2DownNextProgram();
        }
    }

    /**
     * 直接开始下载该任务，会暂停正在下载的任务
     * 
     * @param download
     */
    public void start(Download download) {
        if (download == null) {
            return;
        }

        if (mDownload != null && !mDownload.equals(download)) {
            mDownload.pause();
        }

        mDownload = download;
        startDownloadService();
    }

    /**
     * 删除一个任务
     * 
     * @param download
     * @return
     */
    public void delete(Download download) {

        if (mDownloadList.contains(download)) {
            mDownloadList.remove(download);
        } else if (mDownloadOverList.contains(download)) {
            mDownloadOverList.remove(download);
        }
        new CancelTask(download).execute();
    }

    /**
     * 停止某个任务的下载,停止了后不会去通知下一个任务继续进行下载
     * 
     * @param download
     */
    public void stop(Download download) {
        if (isDownLoadOver(download))
            return;

        download.pause();
        saveDownloadData(download);
    }

    /**
     * 应用程序退出的时候需要调用该方法
     */
    private void quit() {
        new QuitTask().execute();
    }

    /**
     * 把当前任务信息保存到数据库中
     * 
     * @param download
     */
    public void saveDownloadData(Download download) {
        mDownloadDao.updatePauseDownloadingProgram(download);
    }

    /**
     * 开始去下载
     * 
     * @param context
     */
    private void startDownloadService() {
        Intent intent = new Intent(mContext, DownloadService.class);
        mContext.getApplicationContext().startService(intent);
    }

    private boolean isDownLoadOver(Download download) {
        return download != null && download.getDownloadState() == Download.STATE_SUCCESS;
    }

    /**
     * 下载完成后自动进行下一个下载任务
     */
    public void try2DownNextProgram() {
        Log.e(TAG, "try to download next program..");
        if (mDownload != null) {
            if (mDownload.isWait() || mDownload.isDownloading()) {
                mDownload.pause();
            }
        }

        for (Download download : mDownloadList) {
            if (download.isWait()) {
                mDownload = download;
                break;
            }
        }

        startDownloadService();
    }

    public Download getCurrentDownload() {
        return mDownload;
    }

    /**
     * 获取当前正在下载的队列
     * 
     * @return
     */
    public List<Download> getDownloads() {
        return mDownloadList;
    }

    /**
     * 获取已经下载完成的下载任务
     * 
     * @return
     */
    public List<Download> getDownloadOverList() {
        return mDownloadOverList;
    }

    /**
     * 设置当前允许下载的网络类型
     * 
     * @param network
     */
    public void setAllowedNetworkTypes(Network network) {
        mAllowedNetworkType = network;
    }

    /**
     * 设置当前下载是否显示状态栏通知
     * 
     * @param flag
     */
    public void setNotificationVisibility(boolean flag) {
        mNotificationVisibility = flag;
    }

    /**
     * 是否显示状态栏通知
     * 
     * @return
     */
    public boolean getNotificationVisibility() {
        return mNotificationVisibility;
    }

    /**
     * 取消下载数据 并且删除数据库与文件系统数据，通知下一个进入下载
     */
    public class CancelTask extends AsyncTask<Void, Void, Void> {

        private Download download;

        public CancelTask(Download atom) {
            this.download = atom;
        }

        @Override
        protected void onPreExecute() {
            download.pause();
        }

        @Override
        protected Void doInBackground(Void... params) {
            // TODO ..试一下，是不是和电脑上删除文件，有其他引用存在导致的？
            // while (!download.isPaused()) {
            // Thread.yield();
            // }
            Log.d(TAG, "下载已经完全停止,开始进行数据库的删除操作");
            deleteDownloadProgram(download.getNovel().getID() + "");
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mDownload != null && mDownload.equals(download)) {
                mDownload = null;
                try2DownNextProgram();
            }
        }
    }

    /**
     * 下载完成后的处理
     * 
     * @author xuchuanren
     */
    private class DownloadOverTask extends AsyncTask<Void, Void, Void> {

        private Download download;

        public DownloadOverTask(Download atom) {
            this.download = atom;
            atom.downloadOver();
        }

        @Override
        protected Void doInBackground(Void... params) {
            updateDownloadOverProgram(download);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mDownload = null;
            try2DownNextProgram();
        }
    }

    /**
     * 删除一个正在下载的任务
     * 
     * @param programId
     */
    private void deleteDownloadProgram(String programId) {
        mDownloadDao.deleteDownloadProgram(programId);
        // TODO ...FileUtil.deleteSdOrInnerVideoFile(this, programId);
    }

    /**
     * 更新下载完成的任务在数据库中的记录
     * 
     * @param atom
     */
    private void updateDownloadOverProgram(Download download) {
        mDownloadDao.updateDownloadOverProgram(download);
    }

    /**
     * 获取当前的Context对象
     * 
     * @return
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * 把当前所有的下载信息保存到数据库
     * 
     * @author xuchuanren
     */
    private class QuitTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            for (Download download : mDownloadList) {
                if (download.isDownloadOver()) {
                    continue;
                }

                download.pause();
                saveDownloadData(download);
            }
            return null;
        }
    }

    /**
     * 网络类型
     * 
     * @author xuchuanren
     */
    public enum Network {
        WIFI,
        MOBILE
    }
}
