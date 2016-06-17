package fi.aalto.legroup.zop.storage.remote.upload;

import java.io.IOException;

import fi.aalto.legroup.zop.entities.Video;

public interface MetadataUploader {

    /**
     * Upload some custom data about the video. May do blocking network stuff, guaranteed not to be called from the UI thread.
     * @param video The video whose manifest to upload, should be treated as immutable.
     */
    void uploadMetadata(Video video) throws IOException;
}

