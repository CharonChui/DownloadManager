
package com.charon.download.util;

import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

import android.os.Environment;
import android.os.StatFs;

public class MemoryStatus {
    static final int ERROR = -1;

    /**
     * 外部存储是否可用
     * 
     * @return
     */
    static public boolean externalMemoryAvailable() {
        return android.os.Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED);
    }

    /**
     * 获取手机内部可用空间大小
     * 
     * @return
     */
    static public long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

    /**
     * 获取手机内部空间大小
     * 
     * @return
     */
    static public long getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return totalBlocks * blockSize;
    }

    /**
     * 获取手机外部可用空间大小|手机虚拟Sdcard的可用空间大小
     * 
     * @return
     */
    static public long getAvailableExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long availableBlocks = stat.getAvailableBlocks();
            return availableBlocks * blockSize;
        } else {
            return ERROR;
        }
    }

    public static long getRealAvailableExternalMemorySize() {
        if (externalMemoryAvailable()) {
            boolean same = externalMountVSExternalStoragePath();
            if (same) {
                return getAvailableExternalMemorySize();
            } else {
                StatFs stat = new StatFs(getFirstExternalMountPath());
                long blockSize = stat.getBlockSize();
                long availableBlocks = stat.getAvailableBlocks();
                return availableBlocks * blockSize + getAvailableExternalMemorySize();
            }
        } else {
            return ERROR;
        }
    }

    /**
     * 获取手机外部空间大小|手机虚拟Sdcard的空间大小
     * 
     * @return
     */
    static public long getTotalExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long totalBlocks = stat.getBlockCount();
            return totalBlocks * blockSize;
        } else {
            return ERROR;
        }
    }

    public static long getRealTotalExternalMemorySize() {
        if (externalMemoryAvailable()) {
            boolean same = externalMountVSExternalStoragePath();
            if (same) {
                return getTotalExternalMemorySize();
            } else {
                StatFs stat = new StatFs(getFirstExternalMountPath());
                long blockSize = stat.getBlockSize();
                long totalBlocks = stat.getBlockCount();
                return totalBlocks * blockSize + getTotalExternalMemorySize();
            }
        } else {
            return ERROR;
        }
    }

    static public String formatSize(long size) {
        String suffix = null;

        if (size >= 1024) {
            suffix = "KiB";
            size /= 1024;
            if (size >= 1024) {
                suffix = "MiB";
                size /= 1024;
            }
        }

        StringBuilder resultBuffer = new StringBuilder(Long.toString(size));
        int commaOffset = resultBuffer.length() - 3;
        while (commaOffset > 0) {
            resultBuffer.insert(commaOffset, ',');
            commaOffset -= 3;
        }

        if (suffix != null)
            resultBuffer.append(suffix);
        return resultBuffer.toString();
    }

    public static long getExternalMountsSize() {
        if (externalMemoryAvailable()) {
            long size = 0;
            HashSet<String> externalMounts = getExternalMounts();
            for (String path : externalMounts) {
                size += FileUtil.getFileSize(path);
            }
            return size;
        } else {
            return ERROR;
        }
    }

    public static String getFirstExternalMountPath() {
        if (externalMemoryAvailable()) {
            HashSet<String> mounts = getExternalMounts();
            Iterator<String> iterator = mounts.iterator();
            if (iterator.hasNext()) {
                return iterator.next();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static HashSet<String> getExternalMounts() {
        final HashSet<String> out = new HashSet<String>();
        String reg = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";
        String s = "";
        try {
            final Process process = new ProcessBuilder().command("mount")
                    .redirectErrorStream(true).start();
            process.waitFor();
            final InputStream is = process.getInputStream();
            final byte[] buffer = new byte[1024];
            while (is.read(buffer) != -1) {
                s = s + new String(buffer);
            }
            is.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        // parse output
        final String[] lines = s.split("\n");
        for (String line : lines) {
            if (!line.toLowerCase(Locale.US).contains("asec")) {
                if (line.matches(reg)) {
                    String[] parts = line.split(" ");
                    for (String part : parts) {
                        if (part.startsWith("/"))
                            if (!part.toLowerCase(Locale.US).contains("vold"))
                                out.add(part);
                    }
                }
            }
        }
        return out;
    }

    /**
     * @return true : if the externalMounts num == 1 && equals inner sdcardpath
     */
    public static boolean externalMountEqualsExternalStoragePath() {
        if (!externalMemoryAvailable()) {
            return false;
        }
        HashSet<String> externalMounts = getExternalMounts();
        if (externalMounts.size() == 1) {
            for (String mount : externalMounts) {
                if (mount.equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean externalMountVSExternalStoragePath() {
        HashSet<String> externalMounts = getExternalMounts();
        if (externalMounts.isEmpty()) {
            return true;
        }
        if (externalMounts.size() == 1) {
            for (String mount : externalMounts) {
                if (mount.equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                    return true;
                }
            }
        }
        return false;
    }

}
