
package com.charon.download.dao.inter;

import com.charon.download.entity.Download;
import com.charon.download.entity.Novel;

import android.content.Context;

/**
 * 下载的管理接口
 * 
 * @author xuchuanren
 */
public interface IDownloadManager {
    boolean enqueue(Context context, Novel novel);

    void pause(Download download);

    void delete(Download download);
}
