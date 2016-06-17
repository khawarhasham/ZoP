package fi.aalto.legroup.zop.storage.remote.strategies;

import android.net.Uri;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.UUID;

import fi.aalto.legroup.zop.app.App;
import fi.aalto.legroup.zop.entities.Video;
import fi.aalto.legroup.zop.storage.remote.upload.ThumbnailUploader;
import fi.aalto.legroup.zop.storage.remote.upload.VideoUploader;

public class DumbPhpStrategy implements VideoUploader, ThumbnailUploader {

    private final Uri endpointUri;
    private final boolean doesNormalizeRotation;

    public DumbPhpStrategy(Uri endpointUri, boolean doesNormalizeRotation) {
        this.endpointUri = endpointUri.buildUpon().appendPath("upload.php").build();
        this.doesNormalizeRotation = doesNormalizeRotation;
    }

    private Uri uploadFile(UUID id, String type, Uri sourceUri) throws IOException {
        File file = new File(sourceUri.getPath());
        String mimeType = URLConnection.guessContentTypeFromName(file.getPath());

        Uri uri = endpointUri.buildUpon()
                .appendQueryParameter("id", id.toString())
                .appendQueryParameter("type", type)
                .build();

        System.out.println("Khawar ZoP: DumbPhpStrategy.uploadFile " + uri.toString());

        Request request = new Request.Builder()
                .url(uri.toString())
                .put(RequestBody.create(MediaType.parse(mimeType), file))
                .build();

        Response response = App.httpClient.newCall(request).execute();
        if (!response.isSuccessful())
            throw new IOException(response.body().string());

        return Uri.parse(response.body().string().trim());
    }

    @Override
    public Uri uploadThumb(Video video) throws IOException {
        return uploadFile(video.getId(), "thumbnail", video.getThumbUri());
    }

    @Override
    public VideoUploadResult uploadVideo(Video video) throws IOException {
        Uri uri = uploadFile(video.getId(), "video", video.getVideoUri());
        return new VideoUploadResult(uri, doesNormalizeRotation);
    }

    @Override
    public void deleteThumb(Video video) throws IOException {
        // Not supported.
    }
    @Override
    public void deleteVideo(Video video) throws IOException {
        // Not supported.
    }
}
