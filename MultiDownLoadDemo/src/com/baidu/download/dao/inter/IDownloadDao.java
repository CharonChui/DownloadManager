
package com.baidu.download.dao.inter;

import com.baidu.download.entity.Download;
import com.baidu.download.entity.Novel;

import java.util.List;

public interface IDownloadDao {

    /**
     * 获取下载完成的列表
     * 
     * @return
     */
    public List<Download> getDownloadOverList();

    /**
     * 获取非下载完成的列表
     */
    public List<Download> getDownloadList();

    /**
     * 检测该是否下载完成
     */
    public boolean isDownloadOver(Novel novel);

    /**
     * 保存入数据库，默认插入进来的状态是等待状态DownloadProgramAtom.STATE_WAIT
     * 
     * @param program
     * @param mlayout
     * @return
     */
    public long insertNewProgram(Novel novel);

    /**
     * 数据库中是否存在要下载的项
     * 
     * @param program
     * @return
     */
    public boolean isExistDownload(Novel novel);

    /**
     * 根据ID删除一条下载项
     * 
     * @param programId
     * @return
     */
    public int deleteDownloadProgram(String programId);

    /**
     * 重新下载，需要更新数据库状态
     * 
     * @param program
     */
    public int updateReDownLoadState(Novel novel);

    /**
     * 更新下载完成的Program
     * 
     * @param downloadProgramID
     * @return
     */
    public int updateDownloadOverProgram(Download atom);

    /**
     * 更新正在下载
     * 
     * @param atom
     * @return
     */
    public int updatePauseDownloadingProgram(Download atom);

    /**
     * 更新数据库状态为等待
     */
    public int waitCurDownloadingProgram(Download atom);

}
