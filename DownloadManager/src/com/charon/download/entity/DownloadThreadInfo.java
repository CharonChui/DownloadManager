
package com.charon.download.entity;

import java.io.Serializable;

/**
 * 每个下载线程的相关信息，包括该线程所需下载的起始、结束位置已经已经下载完成部分的大小,进行序列化方便保存
 * 
 * @author xuchuanren
 */
public class DownloadThreadInfo implements Serializable {

    private static final long serialVersionUID = -3034050485349260260L;

    /**
     * 该线程所需下载内容的起始位置
     */
    private long startPos;

    /**
     * 该线程所需下载内容的结束位置
     */
    private long endPos;

    /**
     * 该线程已经下载完成的大小
     */
    private long compeleteSize;

    public DownloadThreadInfo(long startPos, long endPos) {
        this.startPos = startPos;
        this.endPos = endPos;
    }

    public long getStartPos() {
        return startPos;
    }

    public long getEndPos() {
        return endPos;
    }

    /**
     * 获取当前线程已经下载完成部分的大小
     * 
     * @return 已经下载完成的大小
     */
    public long getCompeleteSize() {
        return compeleteSize;
    }

    /**
     * 增加该线程已经下载完成的文件大小</br> <font color=red>注意：</font>是该线程已经下载过的文件大小而不是下载到的位置
     * 
     * @param addSize 新下载完的大小
     */
    public void addCompeleteSize(int addSize) {
        compeleteSize += addSize;
    }

}
