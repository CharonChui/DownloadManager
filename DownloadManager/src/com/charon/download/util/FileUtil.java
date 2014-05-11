/**
 * 
 */

package com.charon.download.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.prefs.BackingStoreException;

/**
 * @author yangyang
 * @description
 * @date 2011-3-18
 */
public class FileUtil {

    public static String WORK_DIRECTORY = Environment.getExternalStorageDirectory()
            + "/Android/data/com.ifeng.newvideo/files/";

    public static String WORK_DIRECTORY_CAPTURE = WORK_DIRECTORY + ".camera/";

    public static String WORK_DIRECTORY_VIDEO = WORK_DIRECTORY + "video/";

    public static String WORK_DIRECTORY_AUDIO = WORK_DIRECTORY + "audio/";

    public static String WORK_DIRECTORY_CACHE = WORK_DIRECTORY + "cache/";

    public static String WORK_DIRECTORY_CACHE_IMAGE = WORK_DIRECTORY_CACHE + "image/";

    public static String WORK_DIRECTORY_DOWNLOAD = WORK_DIRECTORY + "download/";

    public static String WORK_DIRECTORY_IMAGE = WORK_DIRECTORY + "image/";

    private static final int DAY_MILLSECOND = 24 * 60 * 60 * 1000;

    public static synchronized File newFile(String workDir, String fileName) throws IOException,
            BackingStoreException {
        if (!checkExternalStorageStatus()) {
            throw new BackingStoreException("external storage not found");
        }
        File wd = new File(workDir);
        if (!wd.exists())
            wd.mkdirs();
        File file = new File(wd, fileName);
        if (!file.exists())
            file.createNewFile();
        return file;
    }

    public static synchronized File newFileWithOutCheckSdCard(String workDir, String fileName)
            throws IOException {
        File wd = new File(workDir);
        if (!wd.exists())
            wd.mkdirs();
        File file = new File(wd, fileName);
        if (!file.exists())
            file.createNewFile();
        return file;
    }

    public static File newCacheImagePath(String src) throws IOException, BackingStoreException {
        return newFile(WORK_DIRECTORY_CACHE_IMAGE, identify(src) + "");
    }

    public static File newCaptureImagePath() throws IOException, BackingStoreException {
        return newFile(WORK_DIRECTORY_CAPTURE, System.currentTimeMillis() + ".jpg");
    }

    public static File newImageCacheFile() throws IOException, BackingStoreException {
        return newFile(WORK_DIRECTORY_CACHE_IMAGE, System.currentTimeMillis() + ".jpg");
    }

    public static File newImageCacheFile(String path) throws IOException, BackingStoreException {
        return newFile(WORK_DIRECTORY_CACHE_IMAGE, identify(path) + "");
    }

    public static File newDownloadCacheFile(String fileName) throws IOException,
            BackingStoreException {
        return newFile(WORK_DIRECTORY_DOWNLOAD, fileName);
    }

    public static boolean exsistsDownloadCacheFile(String fileName) throws IOException,
            BackingStoreException {
        return exsistsFile(WORK_DIRECTORY_DOWNLOAD + "fileName");
    }

    public static File newVideoPath(String fileName) throws IOException, BackingStoreException {
        return newFile(WORK_DIRECTORY_VIDEO, "" + fileName + ".mp4");
    }

    /**
     * 获取可用的外部存储设备，优先选择外插sdcard
     * 
     * @param fileName
     * @return
     * @throws IOException
     * @throws BackingStoreException
     */
    public static File newVideoInExternalMountPath(String fileName) throws IOException,
            BackingStoreException {
        return newFile(getExternalMountVideoPath(), "" + fileName + ".mp4");
    }

    public static boolean exsistVideoFile(String fileName) throws IOException,
            BackingStoreException {
        return exsistsFile(WORK_DIRECTORY_VIDEO, "" + fileName + ".mp4");
    }

    public static boolean exsistVideoFile(String fileName, String suffix) throws IOException,
            BackingStoreException {
        return exsistsFile(WORK_DIRECTORY_VIDEO, "" + fileName + suffix);
    }

    public static String getVideoFilePath(String fileName) {
        return WORK_DIRECTORY_VIDEO + fileName + ".mp4";
    }

    // filename contains suffix
    public static String getVideoFilePath(String direction, String fileName) {
        return direction + fileName;
    }

    public static boolean deleteVideoFile(String fileName) {
        return deleteFile(WORK_DIRECTORY_VIDEO, fileName + ".mp4");
    }

    // fileName contains suffix
    public static boolean deleteVideoFile(String directory, String fileNameWithSuffix) {
        return deleteFile(directory, fileNameWithSuffix);
    }

    public static File newAudioPath(String fileName) throws IOException, BackingStoreException {
        return newFile(WORK_DIRECTORY_AUDIO, "" + fileName + ".mp3");
    }

    public static boolean exsistAudioFile(String fileName) throws IOException,
            BackingStoreException {
        return exsistsFile(WORK_DIRECTORY_AUDIO, "" + fileName + ".mp3");
    }

    public static String getAudioFilePath(String fileName) {
        return WORK_DIRECTORY_AUDIO + fileName + ".mp3";
    }

    public static boolean deleteAudioFile(String fileName) {
        return deleteFile(WORK_DIRECTORY_AUDIO, fileName + ".mp3");
    }

    public static boolean exsistsImageCache(String src) throws IOException, BackingStoreException {
        return exsistsFile(WORK_DIRECTORY_CACHE_IMAGE, identify(src) + "");
    }

    public static boolean exsistsFile(String workDir, String fileName) throws IOException,
            BackingStoreException {
        if (!checkExternalStorageStatus()) {
            throw new BackingStoreException("external storage not found");
        }
        File file = new File(workDir, fileName);
        return file.exists();
    }

    public static boolean exsistsFile(String path) throws IOException, BackingStoreException {
        if (!checkExternalStorageStatus()) {
            throw new BackingStoreException("external storage not found");
        }
        File file = new File(path);
        return file.exists();
    }

    public static void emptyImageCacheDirectory(int day) {
        File file = new File(WORK_DIRECTORY_CACHE_IMAGE);
        deleteFile(file, day);
    }

    public static boolean deleteFile(String workDir, String fileName) {
        File file = new File(workDir, fileName);
        return file.exists() ? file.delete() : true;
    }

    /**
     * @description check the ExternalStorage status
     * @return true if exsists and can write, false on others
     */
    public static boolean checkExternalStorageStatus() {
        boolean mExternalStorageAvailable, mExternalStorageWriteable;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
        return mExternalStorageAvailable && mExternalStorageWriteable;
    }

    public static void copyFile(InputStream is, OutputStream os) throws IOException {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        byte[] buffer = new byte[512];
        try {
            bis = new BufferedInputStream(is, 8192);
            bos = new BufferedOutputStream(os, 8192);
            int len;
            while ((len = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            bos.flush();
        } catch (IOException e) {
            throw e;
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void copyFile(ContentResolver cr, Uri uri, Uri dest) throws IOException {
        copyFile(cr.openInputStream(uri), cr.openOutputStream(dest));
    }

    public static void copyFile(ContentResolver cr, Uri uri, String filePath) throws IOException {
        Uri dest = Uri.fromFile(new File(filePath));
        copyFile(cr, uri, dest);
    }

    public static int identify(Object obj) {
        return obj.hashCode();
    }

    public static String getCacheImagePath(String src) {
        return WORK_DIRECTORY_CACHE_IMAGE + identify(src);
    }

    public static String getCacheVideoPath() {
        return WORK_DIRECTORY_VIDEO + System.currentTimeMillis() + ".3gp";
    }

    public static byte[] fileToByte(String path) throws FileNotFoundException {
        return InputStreamToByte(new FileInputStream(path));
    }

    public static byte[] InputStreamToByte(InputStream is) {
        BufferedInputStream bis = new BufferedInputStream(is);
        byte[] buffer = new byte[10 * 1024];
        ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
        int len;
        byte imgdata[] = null;
        try {
            while ((len = bis.read(buffer)) != -1) {
                bytestream.write(buffer, 0, len);
            }
            imgdata = bytestream.toByteArray();
            bytestream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imgdata;
    }

    public static String getVideoPathFromURI(ContentResolver cr, Uri contentUri) {
        return getRealPathFromURI(cr, contentUri, MediaStore.MediaColumns.MIME_TYPE
                + " like 'video/%'");
    }

    public static String getImagePathFromURI(ContentResolver cr, Uri contentUri) {
        return getRealPathFromURI(cr, contentUri, MediaStore.MediaColumns.MIME_TYPE
                + " like 'image/%'");
    }

    public static String getRealPathFromURI(ContentResolver cr, Uri contentUri, String selection) {
        String path = contentUri.getEncodedPath();
        String[] proj = {
            MediaStore.MediaColumns.DATA
        };
        Cursor cursor = cr.query(contentUri, proj, selection, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            int data_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            path = cursor.getString(data_index);
        }
        return path;
    }

    public static boolean deleteFile(File file) {
        return deleteFile(file, 0);
    }

    public static boolean deleteFile(File file, int day) {
        if (file == null || !file.exists())
            return true;
        if (file.isFile()) {
            if (day <= 0)
                file.delete();
            else if (day > 0
                    && (System.currentTimeMillis() - file.lastModified()) > day
                            * DAY_MILLSECOND)
                file.delete();
        } else if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                deleteFile(subFile, day);
            }
            file.delete(); // xcr add
        }
        return true;
    }

    public static long getFileSize(String path) {
        File filedir = new File(path);
        long totoalLength = 0;
        File[] files = filedir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    totoalLength = totoalLength + getFileSize(file.getAbsolutePath());
                } else {
                    totoalLength += file.length();
                }
            }
        }
        return totoalLength;
    }

    public static long getCacheImageLength() {
        File filedir = new File(WORK_DIRECTORY_CACHE_IMAGE);
        long totoalLength = 0;
        File[] files = filedir.listFiles();
        if (files != null)
            for (File file : files) {
                if (file.isFile()) {
                    totoalLength += file.length();
                }
            }
        return totoalLength;
    }

    public static long getVideoFilesSize() {
        return getFileSize(WORK_DIRECTORY_VIDEO);
    }

    public static File getInnerVideoFileDir(Context context) {
        return new File(context.getFilesDir(), "video");
    }

    public static File newInnerVideoFile(Context context, String fileName) throws IOException {
        return newFileWithOutCheckSdCard(getInnerVideoFileDir(context).getAbsolutePath(), ""
                + fileName + ".mp4");
    }

    public static boolean existInnerVideoFile(Context context, String fileName) {
        File file = new File(getInnerVideoFileDir(context), "" + fileName + ".mp4");
        return file.exists();
    }

    public static long getInnerVideoFileSize(Context context) {
        File filedir = getInnerVideoFileDir(context);
        long totoalLength = 0;
        File[] files = filedir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    totoalLength = totoalLength + getFileSize(file.getAbsolutePath());
                } else {
                    totoalLength += file.length();
                }
            }
        }
        return totoalLength;
    }

    public static boolean existSdOrInnerVideoFile(Context context, String fileName) {
        try {
            return existInnerVideoFile(context, fileName) || exsistVideoFile(fileName)
                    || exsistVideoFileInExternalMount(fileName);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getInnerVideoPath(Context context, String fileName) {
        File file = new File(getInnerVideoFileDir(context), "" + fileName + ".mp4");
        return file.getPath();
    }

    /**
     * 首先从内部存储设备中查找，然后再从外部存储设备中查找，返回第一个有效的地址，如果都没有则
     * 返回null,这个顺序不能变，防止用户缓存到sdcard一半后又拔出，重新缓存到内部sdcard完毕，在插入外部sdcard 读取
     * 
     * @param context
     * @param fileName
     * @return 不存在将返回null
     */
    public static String getInnerOrSDVideoPath(Context context, String fileName) {
        if (existInnerVideoFile(context, fileName)) {
            return getInnerVideoPath(context, fileName);
        }
        try {
            if (exsistVideoFile(fileName)) {
                return getVideoFilePath(fileName);
            }
            if (exsistVideoFileInExternalMount(fileName)) {
                return new File(getExternalMountVideoPath(), fileName + ".mp4").getAbsolutePath();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static File getExternalMountVideoFile(String fileName) {
        if (MemoryStatus.getFirstExternalMountPath() == null) {
            return null;
        } else {
            return new File(getExternalMountVideoPath(), "" + fileName + ".mp4");
        }
    }

    public static boolean deleteSdOrInnerVideoFile(Context context, String fileName) {
        boolean flag = MemoryStatus.getFirstExternalMountPath() == null;
        if (!flag) {
            flag = deleteFile(getExternalMountVideoFile(fileName));
        }
        return deleteFile(WORK_DIRECTORY_VIDEO, fileName + ".mp4") &&
                deleteFile(getInnerVideoFileDir(context).getPath(), fileName + ".mp4") &&
                flag;
    }

    public static String getExternalMountFilesPath() {
        String filePath = MemoryStatus.getFirstExternalMountPath()
                + "/Android/data/com.ifeng.newvideo/files";
        File mountFilesPath = new File(filePath);
        if (!mountFilesPath.exists()) {
            mountFilesPath.mkdirs();
        }
        return filePath;
    }

    /**
     * 调用此方法，确保MemoryStatus.getFirstExternalMountPath() 非null
     * 
     * @return 如果外插设备不存在，会使用内存虚拟sdcard路径
     */
    public static String getExternalMountVideoPath() {
        if (MemoryStatus.getFirstExternalMountPath() == null) {
            return WORK_DIRECTORY_VIDEO;
        }
        String mountVideoPath = MemoryStatus.getFirstExternalMountPath()
                + "/Android/data/com.ifeng.newvideo/files/video";
        File mountVideoFile = new File(mountVideoPath);
        if (!mountVideoFile.exists()) {
            mountVideoFile.mkdirs();
        }
        return mountVideoPath;
    }

    public static boolean exsistVideoFileInExternalMount(String videoName) {
        if (MemoryStatus.getFirstExternalMountPath() == null) {
            return false;
        }
        File f = new File(getExternalMountVideoPath(), videoName + ".mp4");
        return f.exists();
    }

}
