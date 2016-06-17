package fi.aalto.legroup.zop.storage.remote.strategies;

import android.accounts.Account;
import android.net.Uri;

import com.google.common.io.Files;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;

import fi.aalto.legroup.zop.app.App;
import fi.aalto.legroup.zop.entities.Video;
import fi.aalto.legroup.zop.storage.remote.upload.VideoUploader;

/**
 * Uploads raw videos to the Cloud Video Transcoder.
 */
public class ClViTra2Strategy implements VideoUploader {

    private Uri endpointUrl;

    public ClViTra2Strategy(Uri endpointUrl) {
        this.endpointUrl = App.getLayersServiceUrl(endpointUrl);
    }

    /**
     * Uploads the data of a video in a blocking fashion.
     *
     * @param video Video whose data will be uploaded
     * @throws IOException On failure, with a user-friendly error message.
     */
    @Override
    public VideoUploadResult uploadVideo(Video video) throws IOException {
        try {
            return doUploadVideo(video);
        } catch (Exception e) {
            throw new IOException("Couldn't upload to ClViTra: " + e.getMessage(), e);
        }
    }

    private VideoUploadResult doUploadVideo(Video video) throws Exception {
        File videoFile = new File(video.getVideoUri().getPath());

        String extension = Files.getFileExtension(videoFile.getName());
        String fileName = video.getId() + "." + extension;

        // Resolve the mime type of the video file to include it in the request
        String mimeType = URLConnection.guessContentTypeFromName(videoFile.getPath());

        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        MediaType mediaType = MediaType.parse(mimeType);

        // Build a multi-part request body with the file
        RequestBody body = new MultipartBuilder()
                .type(MultipartBuilder.FORM)
                .addPart(
                        Headers.of(
                                "Content-Disposition",
                                "form-data; name=\"file\"; filename=\"" + fileName + "\""
                        ),
                        RequestBody.create(mediaType, videoFile)
                )
                .build();

        Uri requestUrl = endpointUrl.buildUpon().appendEncodedPath("videos").build();

        Request request = new Request.Builder()
                .url(requestUrl.toString())
                .post(body)
                .build();

        Account account = App.loginManager.getAccount();
        Response response = App.authenticatedHttpClient.execute(request, account);

        if (response.isSuccessful()) {
            JsonObject videoDetails = getDetails(fileName);

            // FIXME: Save thumbnail URIs again when Picasso can handle EXIF data in JPEGs.
            //        Or when ClViTra rotates JPEGs for us?

            Uri videoUri = Uri.parse(videoDetails.get("Video_URL").getAsString());
            // Uri thumbUri = Uri.parse(videoDetails.get("Thumbnail_URL").getAsString());

            // video.setVideoUri(videoUri);
            // video.setThumbUri(thumbUri);
            
            return new VideoUploadResult(videoUri, true);

        } else {
            throw new IOException(response.code() + " " + response.message());
        }
    }

    private JsonObject getDetails(String videoFileName) throws IOException {
        Request request = new Request.Builder()
                .url(endpointUrl + "videos")
                .get()
                .build();

        Account account = App.loginManager.getAccount();
        Response response = App.authenticatedHttpClient.execute(request, account);

        String jsonString = response.body().string();
        JsonObject body = new JsonParser().parse(jsonString).getAsJsonObject();
        JsonObject video = null;

        for (JsonElement element : body.get("Videos").getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject object = element.getAsJsonObject();

            if (!object.has("Video_Name")) {
                continue;
            }

            String videoName = object.get("Video_Name").getAsString();

            if (videoName.endsWith(videoFileName)) {
                video = object;
                break;
            }
        }

        if (video == null) {
            throw new IOException("Video not found in response.");
        }

        return video;
    }

    @Override
    public void deleteVideo(Video video) {

        // TODO?
    }
}
