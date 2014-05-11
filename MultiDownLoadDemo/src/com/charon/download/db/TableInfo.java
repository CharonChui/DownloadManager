
package com.charon.download.db;

public class TableInfo {
    public static final String TABLE_NAME_DOWNLOAD = "downloadProgramList";
    public static final String CREAT_D_TABLE = "CREATE TABLE "
            + TABLE_NAME_DOWNLOAD
            + "("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,programid INTEGER,program BLOB,downloadstate INTEGER,"
            + "downloadInfos BLOB,totalsize LONG);";
}
