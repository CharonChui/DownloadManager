
package com.charon.download.entity;

import com.charon.download.engine.DownloadThread;
import com.charon.download.engine.DownloadThreadListener;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 每个下载任务的对象，提供了下载、暂停、停止等方法
 * 
 * @author xuchuanren
 */

public class Download {
    public static final String TAG = "Download";

    /**
     * 多线程断点下载的线程数
     */
    private static final int THREAD_COUNT = 3;

    public static final int UNKNOWN_ERROR = 10;
    public static final int INVALIDATE_URL = 11;
    public static final int SERVER_ERROR = 12;
    public static final int IOEXCEPTION = 13;

    private DownloadThreadInfo[] mDownloadThreadInfos;
    private DownloadThread[] mDownloadThreads;

    private int mDownloadState;

    /*
     * 五种不同的下载状态
     */
    public static final int STATE_DOWNLOADING = 1;
    public static final int STATE_WAIT = 2;
    public static final int STATE_PAUSE = 3;
    public static final int STATE_SUCCESS = 4;

    public static final int DOWNLOAD_SUCCESS = 101;
    public static final int DOWNLOAD_FAILED = 102;

    /**
     * 要下载文件的总长度
     */
    private long mTotalLength;

    /**
     * 已经下载的总长度
     */
    private long mDownloadLength;

    private StartTask mStartTask;

    /**
     * 是否已经开启相应的线程去进行下载？
     */
    private boolean mStartDownload;

    private DownloadListener mDownloadListener;

    private File mFile;

    private Novel mNovel;

    private Handler mHandler;

    private DownloadCompleteListener mCompleteListener;

    private NotificationProgressListener mProgressListener;

    /**
     * 初试下载和重新下载的时候调用
     * 
     * @param context
     * @param dowloadProgram
     * @param sender 此项目中必须是DownloadService对象,用于通知DownloadService是否下载成功
     */
    public Download(Novel novel, Handler handler) {
        mNovel = novel;
        mDownloadState = STATE_WAIT;
        mTotalLength = 0;
        mDownloadLength = 0;
        mHandler = handler;
    }

    /**
     * 下载完成后调用的构造函数
     * 
     * @param novel 下载对象
     * @param totalLength 总长度
     */
    public Download(Novel novel, long totalLength) {
        this.mNovel = novel;
        mTotalLength = totalLength;
        mDownloadLength = totalLength;
        mDownloadState = STATE_SUCCESS;
    }

    /**
     * 断点后继续下载调用此方法
     * 
     * @param novel
     * @param downloadInfos
     * @param downloadState
     * @param totalSize
     * @param sender 此项目中必须是DownloadService对象
     */
    public Download(Novel novel,
            DownloadThreadInfo[] downloadInfos,
            int downloadState, long totalSize, Handler handler) {
        mDownloadThreadInfos = downloadInfos;
        mTotalLength = totalSize;
        mDownloadState = downloadState;
        mNovel = novel;
        mHandler = handler;
        if (downloadInfos != null) {
            for (DownloadThreadInfo info : downloadInfos) {
                mDownloadLength += info.getCompeleteSize();
            }
        }
    }

    /**
     * 开始下载或者是继续下载都调用这个方法
     */
    public void start() {

        if (mDownloadState != STATE_DOWNLOADING) {
            mDownloadState = STATE_DOWNLOADING;
            setStatusChange();

            mStartDownload = false;

            // 开启之前如果前面有没停止的，就先停止
            if (mStartTask != null) {
                mStartTask.cancel();
                if (mDownloadThreads != null) {
                    for (int i = 0; i < THREAD_COUNT; i++) {
                        if (mDownloadThreads[i] != null) {
                            mDownloadThreads[i].pause();
                        }
                    }
                }
                mStartTask = null;
            }

            mStartTask = new StartTask();
            mStartTask.execute();
        }
    }

    /**
     * 暂停下载
     */
    public void pause() {
        if (isDownloadOver()) {
            return;
        }
        mDownloadState = STATE_PAUSE;
        setStatusChange();
        release();
    }

    /**
     * 释放相应的线程
     */
    private void release() {
        if (!checkDownloading() && mStartTask != null) {
            // 线程没开启，就去停止StartTask
            mStartTask.cancel();
        } else {
            // 已经开启相应的下载线程了，去停止所有的下载线程
            if (mDownloadThreads != null) {
                for (int i = 0; i < THREAD_COUNT; i++) {
                    if (mDownloadThreads[i] != null) {
                        mDownloadThreads[i].pause();
                    }
                }
            }
        }

        mStartDownload = false;
    }

    public boolean isDownloading() {
        return mDownloadState == STATE_DOWNLOADING;
    }

    public boolean isDownloadOver() {
        return mDownloadState == STATE_SUCCESS;
    }

    public void wait2Download() {
        mDownloadState = STATE_WAIT;
    }

    public void downloadOver() {
        mDownloadState = STATE_SUCCESS;
    }

    public boolean isWait() {
        return mDownloadState == STATE_WAIT;
    }

    /**
     * 设置当前的下载状态
     * 
     * @param downloadState
     */
    public void setDownloadState(int downloadState) {
        this.mDownloadState = downloadState;
        setStatusChange();
    }

    /**
     * 获取当前的下载状态
     * 
     * @return
     */
    public int getDownloadState() {
        return mDownloadState;
    }

    /**
     * 获取当前已经下在的大小
     * 
     * @return
     */
    public long getDownloadLength() {
        return mDownloadLength;
    }

    private DownloadThreadListener curDownloadProgressListener = new DownloadThreadListener() {

        @Override
        public void onProgressUpdate(int length) {
            mDownloadLength += length;
            if (mDownloadLength > mTotalLength) {
                Log.e(TAG, "current download length > total length");
            }

            if (mDownloadListener != null) {
                mDownloadListener.onProgress(mDownloadLength, mTotalLength);
            }

            if (mProgressListener != null) {
                mProgressListener.onProgress(mDownloadLength, mTotalLength);
            }

            // TODO ..没有实现进度的数据库保存功能,下载多大往数据库存一次比较合适？？？？
            // DownloadManager.getInstance().saveDownloadData(Download.this);

            if (mDownloadLength == mTotalLength) {
                mDownloadState = STATE_SUCCESS;
                // 下载完成
                if (mDownloadListener != null) {
                    mDownloadListener.onSuccess(mFile);
                }

                sendMessage(DOWNLOAD_SUCCESS);
            }
        }

        @Override
        public void onSuccess() {

        }

        @Override
        public void onFailed() {
            // 某个下载线程下载失败了
            if (mDownloadListener != null) {
                mDownloadListener.onFailed(UNKNOWN_ERROR);
            }
            sendMessage(DOWNLOAD_FAILED);
        }
    };

    /**
     * 获取当前文件的长度，在本地建立文件，然后就开启三个线程，去下载相应的部分
     * 
     * @author xuchuanren
     */
    private class StartTask extends AsyncTask<Object, Object, Boolean> {

        /**
         * 通过该变量来控制住StartTask的停止
         */
        private boolean cancel;

        @Override
        protected Boolean doInBackground(Object... params) {
            String tarUrl = mNovel.getUrl();

            if (TextUtils.isEmpty(tarUrl)) {
                if (mDownloadListener != null) {
                    mDownloadListener.onFailed(INVALIDATE_URL);
                }

                sendMessage(DOWNLOAD_FAILED);
                return false;
            }

            if (mTotalLength <= 0) {
                // 没有下载过
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(tarUrl);
                    conn = (HttpURLConnection) (url.openConnection());
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);
                    conn.connect();
                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        mTotalLength = conn.getContentLength();

                        if (mDownloadListener != null) {
                            mDownloadListener.onTotalLength(mTotalLength);
                        }

                        Log.e(TAG, "total length is :" + mTotalLength);
                        return true;
                    } else {
                        if (mDownloadListener != null) {
                            mDownloadListener.onFailed(SERVER_ERROR);
                        }
                        sendMessage(DOWNLOAD_FAILED);
                        return false;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "获取下载内容长度出错IOException" + e.toString());
                    if (mDownloadListener != null) {
                        mDownloadListener.onFailed(IOEXCEPTION);
                    }
                    sendMessage(DOWNLOAD_FAILED);
                    return false;
                } catch (Exception e) {
                    e.printStackTrace();
                    if (mDownloadListener != null) {
                        mDownloadListener.onFailed(UNKNOWN_ERROR);
                    }
                    sendMessage(DOWNLOAD_FAILED);
                    return false;
                } finally {
                    conn.disconnect();
                }
            } else {
                // 已经下载过了
                if (mDownloadListener != null) {
                    mDownloadListener.onTotalLength(mTotalLength);
                }
                return true;
            }
        }

        protected void onPostExecute(Boolean result) {
            // TODO
            // ..考虑到虽然已经下载过了，但是用户手动清理了下载的文件，所以这里也要判断下是否文件已经存在，如果文件已经下载过，但是不存在的话，我们也要去创建文件，然后重新设置DownloadThreadInfo
            if ((mDownloadThreadInfos == null /*
                                               * ||
                                               * !FileUtil.existSdOrInnerVideoFile
                                               * (context,
                                               * dowloadProgram.getId())
                                               */)
                    && result && !cancel) {

                // TODO 默认的下载地址。。。。。。。。。
                mFile = new File("/sdcard/" + getNovel().getID() + ".apk");

                // TODO 。。检查SD卡大小是否够用
                // boolean enough =
                // IUtil.downloadCapacityCheckWithToast(context,
                // totalSize / 1204);
                // if (!enough) {
                // messageSender.sendMessage(IMessageSender.DOWNLOAD_FAIL,
                // DownloadProgramAtom.this);
                // return;
                // }

                try {
                    RandomAccessFile raf = new RandomAccessFile(mFile,
                            "rws");
                    // 指定创建的这个文件的长度
                    raf.setLength(mTotalLength);
                    raf.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mDownloadThreadInfos = new DownloadThreadInfo[THREAD_COUNT];
                mDownloadLength = 0;
                long per = mTotalLength / THREAD_COUNT;
                for (int i = 0; i < THREAD_COUNT; i++) {
                    if (i == 0) {
                        mDownloadThreadInfos[i] = new DownloadThreadInfo(i * per, (i + 1)
                                * per - 1);
                    } else if (i != THREAD_COUNT - 1) {
                        mDownloadThreadInfos[i] = new DownloadThreadInfo(i * per, (i + 1)
                                * per - 1);
                    } else {
                        mDownloadThreadInfos[i] = new DownloadThreadInfo(i * per, (i + 1)
                                * per + (mTotalLength % THREAD_COUNT) - 1);
                    }
                }
            }

            if (result && !cancel) {

                if (mFile == null) {
                    mFile = new File("/sdcard/" + getNovel().getID() + ".apk");

                    Log.e(TAG, "File is Exist ::: " + mFile.exists());

                }

                if (mDownloadThreads == null) {
                    mDownloadThreads = new DownloadThread[THREAD_COUNT];
                }

                for (int i = 0; i < THREAD_COUNT; i++) {
                    if (mDownloadThreadInfos[i].getStartPos()
                            + mDownloadThreadInfos[i].getCompeleteSize()
                    == mDownloadThreadInfos[i].getEndPos() + 1) {
                        continue;
                    }

                    mDownloadThreads[i] = new DownloadThread(mDownloadThreadInfos[i],
                            mNovel.getUrl(), mFile);
                    mDownloadThreads[i]
                            .setDownloadThreadListener(curDownloadProgressListener);
                    mDownloadThreads[i].start();
                }
            }
            mStartDownload = true;
        }

        /**
         * 取消该StartTask
         */
        private void cancel() {
            cancel = true;
        }

    }

    /**
     * 检查当前是否在下载中
     * 
     * @return true为正在下载中
     */
    public boolean checkDownloading() {
        return mStartDownload;
    }

    /**
     * 设置下载的监听器
     * 
     * @param downloadListener {@link DownloadListener}
     */
    public void setDownloadListener(DownloadListener downloadListener) {
        this.mDownloadListener = downloadListener;
    }

    public void setCompleteListener(DownloadCompleteListener completeListener) {
        this.mCompleteListener = completeListener;
    }

    public void setNotificationProgressListener(NotificationProgressListener listener) {
        this.mProgressListener = listener;
    }

    /**
     * 获取当前的下载监听器
     * 
     * @return
     */
    public DownloadListener getDownloadListener() {
        return mDownloadListener;
    }

    public Novel getNovel() {
        return mNovel;
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    /**
     * 获取中的长度
     * 
     * @return
     */
    public long getTotalLenght() {
        return mTotalLength;
    }

    public DownloadThreadInfo[] getDownloadThreadInfos() {
        return mDownloadThreadInfos;
    }

    @Override
    public boolean equals(Object o) {
        // TODO 需要重写equals方法，来判断当前的下载是否一致.....通过url来判断应该可以
        return super.equals(o);
    }

    /**
     * 发送指定的消息
     * 
     * @param what
     */
    private void sendMessage(int what) {
        if (mHandler != null) {
            Message msg = Message.obtain();
            msg.obj = this;
            msg.what = what;
            mHandler.sendMessage(msg);
        }

        if (mCompleteListener != null) {
            if (what == DOWNLOAD_SUCCESS) {
                mCompleteListener.onSuccess(this);
            } else {
                mCompleteListener.onFailed(this);
            }

        }
    }

    private void setStatusChange() {
        if (mDownloadListener != null) {
            mDownloadListener.onStatusChange();
        }
    }

    /**
     * 文件下载情况的监听器
     * 
     * @author xuchuanren
     */
    public interface DownloadListener {
        /**
         * 下载失败
         * 
         * @param errorCode 失败原因
         */
        void onFailed(int errorCode);

        /**
         * 下载成功
         * 
         * @param file 下载完成后的文件对象
         */
        void onSuccess(File file);

        /**
         * 下载进度
         * 
         * @param progress 当前已经下载的总进度
         */
        void onProgress(long progress, long total);

        /**
         * 得到该下载文件的总大小时的回调
         * 
         * @param total 该文件的总大小
         */
        void onTotalLength(long total);

        /**
         * 当前的下载状态发生改变
         */
        void onStatusChange();
    }

    /**
     * 下载完成的监听器,分为两种情况，一个是当前的任务已经下载成功，另一个就是当前的任务下载失败, 没有用，是不是用handler稍微好点？
     * 
     * @author xuchuanren
     */
    public interface DownloadCompleteListener {
        /**
         * 下载成功时的回调
         */
        void onSuccess(Download download);

        /**
         * 下载失败的回调，这时候不需要再进行重试了，直接下载下一个任务就可以了
         */
        void onFailed(Download download);
    }

    /**
     * 状态栏通知进度的通知
     * 
     * @author xuchuanren
     */
    public interface NotificationProgressListener {

        void onProgress(long mDownloadLength, long mTotalLength);
    }

}
