
package com.charon.download.engine;

import com.charon.download.entity.DownloadThreadInfo;

import android.os.Process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 下载文件的线程
 * 
 * @author xuchuanren
 */
public class DownloadThread extends Thread {

    /**
     * 下载失败重试的时间
     */
    private static final long ERROR_SLEEP_TIME = 2000;

    /**
     * 下载失败后可以重试的最大次数
     */
    private static final int MAX_RETRY_TIMES = 3;

    /**
     * 下载流缓冲数组的大小，现在用8k，大小需要以后再设定，太小的话在一些低端机上会很慢
     */
    public static final int BUFFER_SIZE = 8192;

    /**
     * 要下载文件的下载地址
     */
    private String mUrl;

    /**
     * 该线程详细信息的监听器
     */
    private DownloadThreadListener mDownloadThreadListener;

    /**
     * 记录当前是否需要进行重试下载
     */
    private boolean mNeedRetry;

    /**
     * 记录该线程是否暂停
     */
    private boolean mPaused;

    /**
     * 该下载线程的详细信息
     */
    private DownloadThreadInfo mDownloadThreadInfo;

    /**
     * 当前下载失败重试的总次数
     */
    private int mRetryTimes = 0;

    /**
     * 要下载到本地的文件
     */
    private File mFile;

    /**
     * @param info 进行该线程相应下载信息的保存
     * @param url 下载地址
     * @param file 下载后的本地文件
     */
    public DownloadThread(DownloadThreadInfo info, String url, File file) {
        mNeedRetry = true;
        mPaused = false;
        mDownloadThreadInfo = info;
        mUrl = url;
        mFile = file;
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        InputStream is = null;
        HttpURLConnection conn = null;
        while (mNeedRetry) {
            try {
                URL url = new URL(mUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.setRequestProperty(
                        "Range",
                        "bytes="
                                + (mDownloadThreadInfo.getStartPos() + mDownloadThreadInfo
                                        .getCompeleteSize())
                                + "-" + mDownloadThreadInfo.getEndPos());
                conn.connect();

                int resCode = conn.getResponseCode();
                if (resCode == HttpURLConnection.HTTP_PARTIAL
                        || resCode == HttpURLConnection.HTTP_OK) {
                    is = conn.getInputStream();
                    transferData(is, mFile);
                    mNeedRetry = false;
                } else if (resCode == 416) {
                    if (mRetryTimes == MAX_RETRY_TIMES) {
                        if (mDownloadThreadListener != null) {
                            mDownloadThreadListener.onFailed();
                        }
                        // 下载失败，退出循环
                        mNeedRetry = false;
                    } else {
                        // 重试下载
                        mRetryTimes++;
                        try {
                            Thread.sleep(ERROR_SLEEP_TIME);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                } else {
                    // 下载失败
                    if (mDownloadThreadListener != null) {
                        mDownloadThreadListener.onFailed();
                    }
                    mNeedRetry = false;
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
                mNeedRetry = false;
                if (mDownloadThreadListener != null) {
                    mDownloadThreadListener.onFailed();
                }
            } catch (FileNotFoundException fileException) {
                fileException.printStackTrace();
                mNeedRetry = false;
                if (mDownloadThreadListener != null) {
                    mDownloadThreadListener.onFailed();
                }
            } catch (IOException e1) {
                if (mRetryTimes == MAX_RETRY_TIMES) {
                    mNeedRetry = false;
                    if (mDownloadThreadListener != null) {
                        mDownloadThreadListener.onFailed();
                    }
                } else {
                    try {
                        Thread.sleep(ERROR_SLEEP_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mRetryTimes++;
                    continue;
                }
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
    }

    /**
     * 线程暂停下载
     */
    public void pause() {
        mNeedRetry = false;
        mPaused = true;
    }

    /**
     * 下载文件，将流中的内容保存到File中
     * 
     * @param is InputStream
     * @param file File
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void transferData(InputStream is, File file)
            throws IOException, FileNotFoundException {
        
        RandomAccessFile randomAccessFile = null;
        try {
            if(file == null) {
                throw new FileNotFoundException("File参数为空");
            }
            randomAccessFile = new RandomAccessFile(file, "rws");
        } catch (FileNotFoundException e) {
            // 找不到SDcard，由于开启了USB存储设备
            e.printStackTrace();
            throw new FileNotFoundException("文件找不到");
        }
        try {
            randomAccessFile.seek(mDownloadThreadInfo.getStartPos()
                    + mDownloadThreadInfo.getCompeleteSize());
            byte[] buffer = new byte[BUFFER_SIZE];
            int len = 0;
            while (!mPaused && (len = is.read(buffer)) != -1) {
                randomAccessFile.write(buffer, 0, len);
                mDownloadThreadInfo.addCompeleteSize(len);
                if (mDownloadThreadListener != null) {
                    synchronized (mDownloadThreadListener) {
                        mDownloadThreadListener
                                .onProgressUpdate(len);
                    }
                }
            }

            if (!mPaused && mDownloadThreadListener != null) {
                mDownloadThreadListener.onSuccess();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            is.close();
            randomAccessFile.close();
        }
    }

    /**
     * 设置线程现在的监听器
     * 
     * @param threadListener DownloadThreadListener
     */
    public void setDownloadThreadListener(
            DownloadThreadListener threadListener) {
        this.mDownloadThreadListener = threadListener;
    }
}
