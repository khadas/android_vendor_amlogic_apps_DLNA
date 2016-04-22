
package com.droidlogic.mediacenter.dlna;

import java.io.File;
import java.io.IOException;

import android.os.Environment;
import android.util.Log;

public class FileUtil {
    private static final String TAG = "FileUtil";

    public static File getCacheFile(String imageUri) {
        File cacheFile = null;
        File nandDir = Environment.getExternalStorageDirectory();
        String fileName = getFileName(imageUri);
        // Log.d(TAG,"File Name path:"+imageUri+" FileName:"+fileName);
        File dir = new File(nandDir.getAbsoluteFile(), AsynImageLoader.CACHE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        cacheFile = new File(dir, fileName);
        // Log.d(TAG,"exists:" + cacheFile.exists() + ",dir:" + dir + ",file:" +
        // fileName);
        return cacheFile;
    }

    public static void delCacheFile() {
        File cacheFile = null;
        File nandDir = Environment.getExternalStorageDirectory();
        File dir = new File(nandDir.getAbsoluteFile(), AsynImageLoader.CACHE_DIR);
        if (dir.exists()) {
            deleteAllDir(dir);
        }

    }

    private static void deleteAllDir(File dir) {
        if (dir.isDirectory() && !dir.delete()) {
            for (File f : dir.listFiles()) {
                if (f.isDirectory()) {
                    deleteAllDir(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }

    public static String getFileName(String path) {
        int index = path.lastIndexOf("/");
        return path.substring(index + 1);
    }
}