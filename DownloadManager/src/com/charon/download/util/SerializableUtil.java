
package com.charon.download.util;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * 序列化与反序列化的工具类
 * 
 * @author xuchuanren
 */
public class SerializableUtil {
    private static final String TAG = "SerializableUtil";
    
    /**
     * 将对象进行序列化
     * @param obj
     * @return
     */
    public static byte[] obj2Bytes(Object obj) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ObjectOutputStream oo = null;
        try {
            oo = new ObjectOutputStream(bo);
            oo.writeObject(obj);
            byte[] bytes = bo.toByteArray();
            oo.close();
            bo.close();
            return bytes;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将对象进行反序列化
     * @param objBytes
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T bytes2Obj(byte[] objBytes) {
        T t = null;
        if (objBytes == null)
            return null;
        ByteArrayInputStream bi = new ByteArrayInputStream(objBytes);
        ObjectInputStream oi = null;
        try {
            oi = new ObjectInputStream(bi);
            t = (T) oi.readObject();
            bi.close();
            oi.close();
        } catch (java.io.InvalidClassException e) {
            Log.e(TAG, "反序列化失败");
            t = null;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return t;
    }
}
