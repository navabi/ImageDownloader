package ir.noghteh.imagedownloaderlibrary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;

public class CacheManager {

    private static final long MAX_SIZE = 5242880L; // 5MB

    private CacheManager() {

    }

    public static void cacheData(Context context, byte[] data, String name) throws IOException {

        File cacheDir = context.getCacheDir();
        long size = getDirSize(cacheDir);
        long newSize = data.length + size;

        if (newSize > MAX_SIZE) {
            cleanDir(cacheDir, newSize - MAX_SIZE);
        }
        File file = new File(cacheDir, name);
        FileOutputStream os = new FileOutputStream(file);
        try {
            os.write(data);
        }finally {
            os.flush();
            os.close();
        }
    }

    public static byte[] retrieveData(Context context, String name) throws IOException {

        File cacheDir = context.getCacheDir();
        File file = new File(cacheDir, name);

        if (!file.exists()) {
            return null;
        }        
        byte[] data = new byte[(int) file.length()];
        FileInputStream is = new FileInputStream(file);
        try {
            is.read(data);
        }
        finally {
            is.close();
        }

        return data;
    }

    private static void cleanDir(File dir, long bytes) {

        long bytesDeleted = 0;
        File[] files = dir.listFiles();

        for (File file : files) {
            bytesDeleted += file.length();
            file.delete();

            if (bytesDeleted >= bytes) {
                break;
            }
        }
    }

    private static long getDirSize(File dir) {

        long size = 0;
        File[] files = dir.listFiles();

        for (File file : files) {
            if (file.isFile()) {
                size += file.length();
            }
        }

        return size;
    }
    
    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte b : data) {
            int halfbyte = (b >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) : (char) ('a' + (halfbyte - 10)));
                halfbyte = b & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        byte[] sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

	public static void claerCache(Context context) {
		cleanDir(context.getCacheDir(), 5242880L);
		
	}
    
}