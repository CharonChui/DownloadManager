
package com.charon.download.dao;

import com.charon.download.dao.inter.IDownloadDao;
import com.charon.download.db.DownloadOpenHelper;
import com.charon.download.db.TableInfo;
import com.charon.download.entity.Download;
import com.charon.download.entity.DownloadThreadInfo;
import com.charon.download.entity.Novel;
import com.charon.download.util.FileUtil;
import com.charon.download.util.SerializableUtil;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DownloadDao implements IDownloadDao {

    public static final String TAG = "DownLoadDaoImpl";
    private Context context;
    private DownloadOpenHelper downloadOpenHelper;

    public DownloadDao(Context context) {
        this.context = context;
        downloadOpenHelper = new DownloadOpenHelper(context);
    }

    @Override
    public List<Download> getDownloadOverList() {
        ArrayList<Download> list = new ArrayList<Download>();
        Cursor c = null;
        Set<String> ids = new HashSet<String>();
        SQLiteDatabase proxy = downloadOpenHelper.getReadableDatabase();
        try {
            c = proxy.query(TableInfo.TABLE_NAME_DOWNLOAD, new String[] {
                    "program",
                    "totalsize"
            }, "downloadstate=?", new String[] {
                    "" + Download.STATE_SUCCESS
            }, null, null, null);
            c.moveToFirst();
            if (c.getCount() <= 0) {
                c.close();
                return list;
            }
            while (!c.isAfterLast()) {
                Novel program = SerializableUtil.<Novel> bytes2Obj(c.getBlob(c
                        .getColumnIndex("program")));

                long totalsize = c.getLong(c.getColumnIndex("totalsize"));

                Download atom = new Download(program,
                        totalsize);

                if (program == null) {
                    Log.e(TAG, "反序列化失败,删除对应存储有序列化对象的表内全部数据");
                    proxy.delete(TableInfo.TABLE_NAME_DOWNLOAD, null, null);
                    break;
                }

                // TODO ... 判断。。。
                list.add(atom);
                // if (FileUtil.existSdOrInnerVideoFile(context,
                // program.getID() + "")) {
                // Log.d(TAG, "存在下载的id=" + program.getID());
                // list.add(atom);
                // } else {
                // Log.d(TAG, "不存在下载的id=" + program.getID());
                // ids.add(program.getID() + "");
                // }
                c.moveToNext();
            }
        } finally {
            if (c != null) {
                c.close();
            }
            proxy.close();
        }
        if (ids.size() > 0) {
            for (String id : ids) {
                deleteDownloadProgram(id);
            }
        }
        return list;
    }

    @Override
    /**
     * 获取所有未下载完的内容
     */
    public List<Download> getDownloadList() {
        List<Download> downloadList = new ArrayList<Download>();
        SQLiteDatabase proxy = downloadOpenHelper.getReadableDatabase();
        Cursor c = proxy.query(TableInfo.TABLE_NAME_DOWNLOAD, null, "downloadstate<"
                + Download.STATE_SUCCESS, null, null, null, null);
        try {
            c.moveToFirst();
            while (!c.isAfterLast()) {
                Novel dowloadProgram = SerializableUtil.<Novel> bytes2Obj(c.getBlob(c
                        .getColumnIndex("program")));
                DownloadThreadInfo[] downloadInfos = SerializableUtil
                        .<DownloadThreadInfo[]> bytes2Obj(c
                                .getBlob(c
                                        .getColumnIndex("downloadInfos")));

                if (dowloadProgram == null) {
                    Log.e(TAG, "反序列化失败,删除对应存储有序列化对象的表内全部数据");
                    proxy.delete(TableInfo.TABLE_NAME_DOWNLOAD, null, null);
                    break;
                }

                Download atom = new Download(dowloadProgram,
                        downloadInfos, c.getInt(c
                                .getColumnIndex("downloadstate")),
                        c.getLong(c.getColumnIndex("totalsize")), null);

                downloadList.add(atom);
                c.moveToNext();
            }
        } finally {
            if (c != null) {
                c.close();
            }
            proxy.close();
        }
        return downloadList;
    }

    @Override
    public boolean isDownloadOver(Novel p) {
        int count = 0;
        Cursor c = null;
        SQLiteDatabase proxy = downloadOpenHelper.getReadableDatabase();
        try {
            c = proxy.query(TableInfo.TABLE_NAME_DOWNLOAD, null, "programid=? and downloadstate=?",
                    new String[] {
                            p.getID() + "", "" + Download.STATE_SUCCESS
                    }, null, null, null);
            count = c.getCount();
        } catch (Exception e) {
            Log.e(TAG, "查询是否下载完成发生异常" + e.toString());
        } finally {
            if (c != null) {
                c.close();
            }
            proxy.close();
        }
        if (count > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public long insertNewProgram(Novel program) {
        SQLiteDatabase database = downloadOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("programid", program.getID());
        values.put("program", SerializableUtil.obj2Bytes(program));
        values.put("downloadstate", Download.STATE_WAIT);
        return database.insert(TableInfo.TABLE_NAME_DOWNLOAD, null, values);
    }

    @Override
    public boolean isExistDownload(Novel program) {
        int count = 0;
        Cursor c = null;
        SQLiteDatabase database = downloadOpenHelper.getReadableDatabase();
        try {
            c = database.query(TableInfo.TABLE_NAME_DOWNLOAD, null, "programid=?",
                    new String[] {
                        program.getID() + ""
                    }, null, null, null);
            count = c.getCount();
            Log.d(TAG, "离线列表的个数=" + count);
        } finally {
            if (c != null) {
                c.close();
            }
            database.close();
        }
        if (count > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int deleteDownloadProgram(String programId) {
        SQLiteDatabase database = downloadOpenHelper.getWritableDatabase();
        return database.delete(TableInfo.TABLE_NAME_DOWNLOAD, "programid=?", new String[] {
                programId
        });
    }

    @Override
    public int updateReDownLoadState(Novel program) {
        SQLiteDatabase database = downloadOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("downloadstate", Download.STATE_WAIT);
        values.putNull("downloadInfos");

        return database.update(TableInfo.TABLE_NAME_DOWNLOAD, values, "programid = ?",
                new String[] {
                    program.getID() + ""
                });
    }

    @Override
    public int updateDownloadOverProgram(Download atom) {
        SQLiteDatabase database = downloadOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("totalsize", atom.getTotalLenght());
        values.put("downloadstate", Download.STATE_SUCCESS);
        values.putNull("downloadInfos");
        return database.update(TableInfo.TABLE_NAME_DOWNLOAD, values, "programid = ?",
                new String[] {
                    atom.getNovel().getID() + ""
                });
    }

    @Override
    public int updatePauseDownloadingProgram(Download atom) {
        SQLiteDatabase database = downloadOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("downloadstate", atom.getDownloadState());
        values.put("downloadInfos", SerializableUtil.obj2Bytes(atom.getDownloadThreadInfos()));
        values.put("totalsize", atom.getTotalLenght());
        return database.update(TableInfo.TABLE_NAME_DOWNLOAD, values, "programid = ?",
                new String[] {
                    atom.getNovel().getID() + ""
                });
    }

    @Override
    public int waitCurDownloadingProgram(Download atom) {
        SQLiteDatabase database = downloadOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("downloadstate", atom.getDownloadState());
        values.put("downloadInfos", SerializableUtil.obj2Bytes(atom.getDownloadThreadInfos()));
        values.put("totalsize", atom.getTotalLenght());
        return database.update(TableInfo.TABLE_NAME_DOWNLOAD, values, "programid = ?",
                new String[] {
                    atom.getNovel().getID() + ""
                });
    }

}
