package fi.aalto.legroup.zop.storage.remote;

import fi.aalto.legroup.zop.storage.VideoRepository;

public class SyncRequiredEvent {
    private final VideoRepository repository;

    public SyncRequiredEvent(VideoRepository repository) {
        this.repository = repository;
    }

    public VideoRepository getRepository() {
        return repository;
    }
}

