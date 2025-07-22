package com.g3.soundify_musicplayer.utils;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for file operations
 * Handles file copying, metadata extraction, and file name operations
 */
public class FileUtils {
    
    private static final String TAG = "FileUtils";
    
    /**
     * Get file name from URI
     */
    public static String getFileName(Context context, Uri uri) {
        String fileName = null;
        
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name from content URI", e);
            }
        }
        
        if (fileName == null) {
            fileName = uri.getPath();
            if (fileName != null) {
                int cut = fileName.lastIndexOf('/');
                if (cut != -1) {
                    fileName = fileName.substring(cut + 1);
                }
            }
        }
        
        return fileName != null ? fileName : "unknown_file";
    }
    
    /**
     * Get file size from URI
     */
    public static long getFileSize(Context context, Uri uri) {
        long size = 0;
        
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file size from content URI", e);
            }
        } else if (uri.getScheme().equals("file")) {
            try {
                File file = new File(uri.getPath());
                size = file.length();
            } catch (Exception e) {
                Log.e(TAG, "Error getting file size from file URI", e);
            }
        }
        
        return size;
    }
    
    /**
     * Copy file from URI to internal storage
     */
    public static String copyFileToInternalStorage(Context context, Uri sourceUri, String fileName) {
        try {
            // Determine directory based on file type
            String dirName = "audio"; // default
            if (fileName.toLowerCase().matches(".*\\.(jpg|jpeg|png|webp)$")) {
                dirName = "images";
            }

            // Create directory
            File targetDir = new File(context.getFilesDir(), dirName);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            // Create destination file
            File destFile = new File(targetDir, fileName);

            // Copy file
            try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
                 FileOutputStream outputStream = new FileOutputStream(destFile)) {

                if (inputStream != null) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
            }

            return destFile.getAbsolutePath();

        } catch (IOException e) {
            Log.e(TAG, "Error copying file to internal storage", e);
            return null;
        }
    }
    
    /**
     * Get audio duration from file URI
     */
    public static long getAudioDuration(Context context, Uri audioUri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, audioUri);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                return Long.parseLong(durationStr);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting audio duration", e);
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever", e);
            }
        }
        return 0;
    }
    
    /**
     * Check if file is a valid audio file
     */
    public static boolean isValidAudioFile(Context context, Uri audioUri) {
        try {
            String mimeType = context.getContentResolver().getType(audioUri);
            return mimeType != null && mimeType.startsWith("audio/");
        } catch (Exception e) {
            Log.e(TAG, "Error checking audio file validity", e);
            return false;
        }
    }
    
    /**
     * Check if file is a valid image file
     */
    public static boolean isValidImageFile(Context context, Uri imageUri) {
        try {
            String mimeType = context.getContentResolver().getType(imageUri);
            return mimeType != null && mimeType.startsWith("image/");
        } catch (Exception e) {
            Log.e(TAG, "Error checking image file validity", e);
            return false;
        }
    }
    
    /**
     * Format file size to human readable string
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Delete file from internal storage
     */
    public static boolean deleteFile(String filePath) {
        try {
            File file = new File(filePath);
            return file.delete();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting file: " + filePath, e);
            return false;
        }
    }
    
    /**
     * Check if file exists
     */
    public static boolean fileExists(String filePath) {
        try {
            File file = new File(filePath);
            return file.exists();
        } catch (Exception e) {
            Log.e(TAG, "Error checking file existence: " + filePath, e);
            return false;
        }
    }

    /**
     * List all files in app's internal storage
     * Call this to see what files are stored
     */
    public static void listAllStoredFiles(Context context) {
        // List audio files
        File audioDir = new File(context.getFilesDir(), "audio");
        if (audioDir.exists()) {
            File[] audioFiles = audioDir.listFiles();
            if (audioFiles != null) {
                for (File file : audioFiles) {
                    // Files exist but no debug logging
                }
            }
        }

        // List image files
        File imageDir = new File(context.getFilesDir(), "images");
        if (imageDir.exists()) {
            File[] imageFiles = imageDir.listFiles();
            if (imageFiles != null) {
                for (File file : imageFiles) {
                    // Files exist but no debug logging
                }
            }
        }
    }

    /**
     * Get app's internal storage path for debugging
     */
    public static String getInternalStoragePath(Context context) {
        return context.getFilesDir().getAbsolutePath();
    }
}
