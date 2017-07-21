/*
 * Clover - 4chan browser https://github.com/Floens/Clover/
 * Copyright (C) 2014  Floens
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.floens.flutter.core.saver;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;

import org.floens.flutter.Chan;
import org.floens.flutter.core.cache.FileCache;
import org.floens.flutter.core.model.PostImage;
import org.floens.flutter.utils.AndroidUtils;
import org.floens.flutter.utils.IOUtils;
import org.floens.flutter.utils.ImageDecoder;
import org.floens.flutter.utils.Logger;

import java.io.File;
import java.io.IOException;

import static org.floens.flutter.utils.AndroidUtils.dp;
import static org.floens.flutter.utils.AndroidUtils.getAppContext;

public class ImageSaveTask implements Runnable, FileCache.DownloadedCallback {
    private static final String TAG = "ImageSaveTask";

    private PostImage postImage;
    private ImageSaveTaskCallback callback;
    private File destination;
    private boolean share;
    private boolean makeBitmap;
    private Bitmap bitmap;
    private boolean showToast;
    private String subFolder;

    private boolean success = false;

    public void setSubFolder(String boardName) {
        this.subFolder = boardName;
    }

    public String getSubFolder() {
        return subFolder;
    }

    public ImageSaveTask(PostImage postImage) {
        this.postImage = postImage;
    }

    public void setCallback(ImageSaveTaskCallback callback) {
        this.callback = callback;
    }

    public PostImage getPostImage() {
        return postImage;
    }

    public void setDestination(File destination) {
        this.destination = destination;
    }

    public File getDestination() {
        return destination;
    }

    public void setShare(boolean share) {
        this.share = share;
    }

    public void setMakeBitmap(boolean makeBitmap) {
        this.makeBitmap = makeBitmap;
    }

    public boolean isMakeBitmap() {
        return makeBitmap;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setShowToast(boolean showToast) {
        this.showToast = showToast;
    }

    public boolean isShowToast() {
        return showToast;
    }

    @Override
    public void run() {
        try {
            if (destination.exists()) {
                onDestination();
                // Manually call postFinished()
                postFinished(success);
            } else {
                // Both onSuccess and onFail call postFinished()
                FileCache.FileCacheDownloader fileCacheDownloader = Chan.getFileCache().downloadFile(postImage.imageUrl, this);
                // If the fileCacheDownloader is null then the destination already existed and onSuccess() has been called.
                // Wait otherwise for the download to finish to avoid that the next task is immediately executed.
                if (fileCacheDownloader != null) {
                    // If the file is now downloading
                    fileCacheDownloader.getFuture().get();
                }
            }
        } catch (InterruptedException e) {
            onInterrupted();
        } catch (Exception e) {
            Logger.e(TAG, "Uncaught exception", e);
        }
    }

    @Override
    public void onProgress(long downloaded, long total, boolean done) {
    }

    @Override
    public void onFail(boolean notFound) {
        postFinished(success);
    }

    @Override
    public void onSuccess(File file) {
        if (copyToDestination(file)) {
            onDestination();
        } else {
            deleteDestination();
        }
        postFinished(success);
    }

    private void onInterrupted() {
        deleteDestination();
    }

    private void deleteDestination() {
        if (destination.exists()) {
            if (!destination.delete()) {
                Logger.e(TAG, "Could not delete destination after an interrupt");
            }
        }
    }

    private void onDestination() {
        success = true;
        scanDestination();
        if (makeBitmap) {
            bitmap = ImageDecoder.decodeFile(destination, dp(512), dp(256));
        }
    }

    private boolean copyToDestination(File source) {
        boolean result = false;

        try {
            File parent = destination.getParentFile();
            if (!parent.mkdirs() && !parent.isDirectory()) {
                throw new IOException("Could not create parent directory");
            }

            if (destination.isDirectory()) {
                throw new IOException("Destination file is already a directory");
            }

            IOUtils.copyFile(source, destination);

            result = true;
        } catch (IOException e) {
            Logger.e(TAG, "Error writing to file", e);
        }

        return result;
    }

    private void scanDestination() {
        MediaScannerConnection.scanFile(getAppContext(), new String[]{destination.getAbsolutePath()}, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String path, final Uri uri) {
                // Runs on a binder thread
                AndroidUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        afterScan(uri);
                    }
                });
            }
        });
    }

    private void afterScan(final Uri uri) {
        Logger.d(TAG, "Media scan succeeded: " + uri);

        if (share) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            AndroidUtils.openIntent(intent);
        }
    }

    private void postFinished(final boolean success) {
        AndroidUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callback.imageSaveTaskFinished(ImageSaveTask.this, success);
            }
        });
    }

    public interface ImageSaveTaskCallback {
        void imageSaveTaskFinished(ImageSaveTask task, boolean success);
    }
}
