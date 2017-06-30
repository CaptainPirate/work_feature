/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.wingtech.note.AttachmentUtils.ImageInfo;
import com.wingtech.note.data.NoteConstant;
import com.wingtech.note.data.NoteConstant.AttachmentColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class AttachmentUtils {
    private static final String TAG = AttachmentUtils.class.getSimpleName();

    public static final String getAttachmentPath(Context context, String name) {
        return getAttachmentDir(context) + "/" + name;
    }

    private static String getAttachmentDir(Context context) {
        // TODO Auto-generated method stub
        return context.getFilesDir().getAbsolutePath() + "/attachment";
    }

    public static String getImageMimeType(InputStream input) {
        BitmapFactory.Options localOptions = new BitmapFactory.Options();
        localOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, localOptions);
        return localOptions.outMimeType;
    }

    public static String getImageMimeType(String path) {
        BitmapFactory.Options localOptions = new BitmapFactory.Options();
        localOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, localOptions);
        return localOptions.outMimeType;
    }

    private static boolean saveAttachmentFile(Context context, Uri uri, File file) {
        InputStream input = null;
        boolean result = false;
        try {
            input = context.getContentResolver().openInputStream(uri);
            result = Utils.copyToFile(input, file);
        } catch (FileNotFoundException e) {
            Log.w(TAG, "open attachment uri failed", e);
        }
        if (input != null)
            try {
                input.close();
            } catch (IOException e) {
                Log.w(TAG, "Save attachment file failed", e);
            }
        return result;
    }

    public static boolean saveBitmap(Bitmap bmp, File file) {
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            bmp.compress(CompressFormat.JPEG, 80, out);
            out.flush();
            out.close();
            return true;
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }

    }

    public static File getImageFileByName(Context context, String name) {
        return new File(AttachmentUtils.getAttachmentPath(context, name));
    }

    public static String saveImageFile(Context context, Uri uri) {
        File srcFile = new File(getAttachmentDir(context));
        try {
            File tempFile = File.createTempFile(Long.toString(System.currentTimeMillis()), null,
                    srcFile);
            if (saveAttachmentFile(context, uri, tempFile)) {
                Bitmap thumbnail = Utils.createThumbnail(tempFile.getAbsolutePath());
                String name = null;
                if (thumbnail != null) {
                    if (saveBitmap(thumbnail, tempFile)) {
                        name = Utils.getFileSha1(tempFile.getAbsolutePath());
                        File file = new File(getAttachmentPath(context, name));
                        if (!file.exists()) {
                            tempFile.renameTo(file);
                        }
                        tempFile.delete();
                        file.setReadable(true, false);
                    }
                    thumbnail.recycle();
                    return name;
                }
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "saveImageFile: create temp file fail");
            e.printStackTrace();
        }
        return null;
    }

    public static String saveImageFile(Context context, Bitmap bmp) {
        File srcFile = new File(getAttachmentDir(context));
        try {
            File tempFile = File.createTempFile(Long.toString(System.currentTimeMillis()), null,
                    srcFile);

            if (bmp != null) {
                String name = null;
                if (saveBitmap(bmp, tempFile)) {
                    name = Utils.getFileSha1(tempFile.getAbsolutePath());
                    File file = new File(getAttachmentPath(context, name));
                    if (!file.exists()) {
                        tempFile.renameTo(file);
                    }
                    tempFile.delete();
                    file.setReadable(true, false);
                }
                bmp.recycle();
                return name;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "saveImageFile: create temp file fail");
            e.printStackTrace();
        }
        return null;
    }

    public static class ImageInfo {
        public final String imageName;
        public final String mimeType;

        public ImageInfo(String name, String mimeType) {
            this.imageName = name;
            this.mimeType = mimeType;
        }
    }

    public static HashMap<String, ImageInfo> getImageInfosByName(Context context, long noteId,
            List<String> names) {
        HashMap<String, ImageInfo> out;
        if ((names == null) || (names.isEmpty()))
            out = null;
        else {
            out = new HashMap<String, ImageInfo>(names.size());
            Iterator<String> iterator = names.iterator();
            while (iterator.hasNext()) {
                String name = iterator.next();
                String path = getAttachmentPath(context, name);
                String mimeType = getImageMimeType(path);
                ImageInfo info = new ImageInfo(name, mimeType);
                out.put(name, info);
            }
        }
        return out;
    }

    public static void checkAttachmentDir(Context context) {
        File dir = new File(getAttachmentDir(context));
        if (!dir.exists()){
            dir.mkdirs();
            dir.setExecutable(true, false);
        }
    }

    public static String getTmpFile(Context context) {
        return context.getCacheDir() + "/.temp.jpg";
    }

    public static String getTmpFile(Context context, String fileName) {
        return context.getCacheDir() + File.separator + fileName;
    }

    public static String getCachedImgpath(Context context, String fileName) {
        File file = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        if (file != null)
            return file.getAbsolutePath() + File.separator + fileName;
        return getTmpFile(context, fileName);
    }

    public static String saveSketchBmp(Context context, Bitmap bmp, long sketchId) {

        String path = getCachedImgpath(context, Long.toString(sketchId) + ".jpg");
        File file = new File(path);
        if (saveBitmap(bmp, file))
            return path;
        else
            return null;
    }

    public static void checkAttachmentFile(Context context) {
        // TODO Auto-generated method stub
        File dir = new File(getAttachmentDir(context));
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (!checkImgNameInDataBase(context, file.getName())) {
                    file.delete();
                }else{
                    file.setReadable(true, false);
                }
            }
        }
    }

    public static boolean checkImgNameInDataBase(Context context, String name) {
        String[] projection = {
                AttachmentColumns.ID
        };
        String selection = AttachmentColumns.NAME + " like '" + name + "'";
        Cursor cursor = context.getContentResolver().query(NoteConstant.CONTENT_ATTACHMENT_URI,
                projection, selection, null, null);
        boolean b = false;
        try {
            if (cursor.moveToNext())
                b = true;
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return b;
    }
}
