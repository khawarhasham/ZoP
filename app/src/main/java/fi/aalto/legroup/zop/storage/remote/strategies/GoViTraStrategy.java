package fi.aalto.legroup.zop.storage.remote.strategies;

import android.accounts.Account;
import android.net.Uri;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;

import fi.aalto.legroup.zop.app.App;
import fi.aalto.legroup.zop.entities.Video;
import fi.aalto.legroup.zop.entities.serialization.json.JsonSerializable;
import fi.aalto.legroup.zop.entities.serialization.json.JsonSerializer;
import fi.aalto.legroup.zop.storage.remote.upload.VideoUploader;

public class GoViTraStrategy implements VideoUploader {

    private JsonSerializer serializer;
    private final Uri endpointUri;

    private static class JsonResult implements JsonSerializable {
        public Uri video;
        public Uri thumbnail;
        public Uri deleteUrl;
        public String title;
    }

    public GoViTraStrategy(JsonSerializer serializer, Uri endpointUri) {
        this.serializer = serializer;
        this.endpointUri = endpointUri.buildUpon().appendPath("uploads").build();
    }

    public VideoUploader.VideoUploadResult uploadVideo(Video video) throws IOException {
        File file = new File(video.getVideoUri().getPath());
        String mimeType = URLConnection.guessContentTypeFromName(file.getPath());

        System.out.println("Khawar ZoP: GoViTraStrategy.uploadVideo " + endpointUri.toString());

        Request request = new Request.Builder()
                .url(endpointUri.toString())
                .post(RequestBody.create(MediaType.parse(mimeType), file))
                .build();

        Account account = App.loginManager.getAccount();
        Response response = App.authenticatedHttpClient.execute(request, account);
        if (!response.isSuccessful())
            throw new IOException(response.body().string());
        JsonResult result = serializer.read(JsonResult.class, response.body().byteStream());

        return new VideoUploadResult(result.video, result.thumbnail, result.deleteUrl, true);
    }

    @Override
    public void deleteVideo(Video video) throws IOException {
        String token = video.getVideoUri().getLastPathSegment().replaceAll("\\..*", "");
        Request request = new Request.Builder()
                .url(endpointUri.buildUpon().appendPath(token).build().toString())
                .delete()
                .build();

        Account account = App.loginManager.getAccount();
        Response response = App.authenticatedHttpClient.execute(request, account);
        if (!response.isSuccessful())
            throw new IOException(response.body().string());
    }
}
