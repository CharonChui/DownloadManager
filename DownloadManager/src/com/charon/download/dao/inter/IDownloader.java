
package com.charon.download.dao.inter;

/**
 * 可下载对象的接口
 * 
 * @author xuchuanren
 */
public interface IDownloader {

    /**
     * 获取下载地址
     * 
     * @return
     */
    String getUrl();

    /**
     * 获取相应的ID
     * 
     * @return
     */
    int getID();
}
