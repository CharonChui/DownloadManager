
package com.charon.download.engine;

/**
 * 每个下载线程的监听器
 * 
 * @author xuchuanren
 */
public interface DownloadThreadListener {

    /**
     * 该线程所下载的内容已经全部下载完成
     */
    void onSuccess();

    /**
     * 该线程下载失败
     */
    void onFailed();

    /**
     * 该线程新下载的文件大小
     */
    void onProgressUpdate(int length);
}
