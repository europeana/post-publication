package eu.europeana.postpublication.batch.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

import java.time.Instant;

@Entity("PostPublicationJobMetadata")
public class PostPublicationJobMetadata {

    @Id
    private ObjectId dbId;
    private Instant lastSuccessfulStartTime;

    public PostPublicationJobMetadata() {
        // no-arg constructor
    }

    public PostPublicationJobMetadata(Instant lastSuccessfulStartTime) {
        this.lastSuccessfulStartTime = lastSuccessfulStartTime;
    }

    public Instant getLastSuccessfulStartTime() {
        return lastSuccessfulStartTime;
    }

    public void setLastSuccessfulStartTime(Instant lastSuccessfulStartTime) {
        this.lastSuccessfulStartTime = lastSuccessfulStartTime;
    }

    @Override
    public String toString() {
        return "PostPublicationJobMetadata{"
                + "lastSuccessfulStartTime="
                + lastSuccessfulStartTime
                + '}';
    }
}
