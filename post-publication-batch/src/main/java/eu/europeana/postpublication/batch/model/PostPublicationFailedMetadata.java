package eu.europeana.postpublication.batch.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity("PostPublicationFailedMetadata")
public class PostPublicationFailedMetadata {

    @Id
    private ObjectId dbId;

    Map<String, List<String>> failedRecords = new HashMap<>();

    boolean processed; // be default false

    public PostPublicationFailedMetadata() {
        // no-arg constructor
    }

    public Map<String, List<String>> getFailedRecords() {
        return failedRecords;
    }

    public void setFailedRecords(Map<String, List<String>> failedRecords) {
        this.failedRecords = failedRecords;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }
}
