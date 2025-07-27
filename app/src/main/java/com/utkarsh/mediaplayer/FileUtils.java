package com.utkarsh.mediaplayer;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {

    public static String getPathFromUri(Context context, Uri uri) {
        // Check for null URI
        if (uri == null) return null;

        // File scheme (direct file path)
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            return uri.getPath();
        }

        // MediaStore (images/videos/audio)
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            // MediaStore provider
            if (isMediaStoreUri(uri)) {
                String[] projection = {MediaStore.MediaColumns.DATA};
                try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                        return cursor.getString(columnIndex);
                    }
                }
            }
            // Other content providers (like Downloads/Documents)
            return getFilePathFromContentUri(context, uri);
        }

        return null;
    }

    private static boolean isMediaStoreUri(Uri uri) {
        return uri.getAuthority() != null && uri.getAuthority().startsWith("media");
    }

    private static String getFilePathFromContentUri(Context context, Uri uri) {
        String filePath = null;
        String[] projection = {OpenableColumns.DISPLAY_NAME};

        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                File file = new File(context.getExternalCacheDir(), fileName);

                try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                     OutputStream outputStream = new FileOutputStream(file)) {

                    byte[] buffer = new byte[4 * 1024];
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, read);
                    }
                    outputStream.flush();
                    filePath = file.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filePath;
    }
}