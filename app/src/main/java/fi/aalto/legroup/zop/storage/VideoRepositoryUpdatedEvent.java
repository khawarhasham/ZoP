package fi.aalto.legroup.zop.storage;

/**
 * TODO: Split this event into something more specific.
 */
public class VideoRepositoryUpdatedEvent {

    private VideoRepository repository;

    public VideoRepositoryUpdatedEvent(VideoRepository repository) {
        this.repository = repository;
    }

    public VideoRepository getRepository() {
        return this.repository;
    }

}
