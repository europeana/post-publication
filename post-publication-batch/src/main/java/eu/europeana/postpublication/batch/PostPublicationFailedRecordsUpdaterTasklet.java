package eu.europeana.postpublication.batch;

import eu.europeana.postpublication.batch.model.PostPublicationFailedMetadata;
import eu.europeana.postpublication.batch.repository.PostPublicationFailedRecordsRepo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class PostPublicationFailedRecordsUpdaterTasklet implements Tasklet {

    private final PostPublicationFailedRecordsRepo repository;
    private final PostPublicationFailedMetadata metadata;
    private static final Logger logger = LogManager.getLogger(PostPublicationFailedRecordsUpdaterTasklet.class);

    public PostPublicationFailedRecordsUpdaterTasklet(
            PostPublicationFailedRecordsRepo repository, PostPublicationFailedMetadata metadata) {
        this.repository = repository;
        this.metadata = metadata;
    }

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        // save the new metadata. Only if there will be failed data this will be saved
        repository.save(repository.progress(metadata));

        // update the old metadata
        if (metadata != null && !metadata.getFailedRecords().isEmpty()) {
            metadata.setProcessed(true);
            repository.save(metadata);
           // repository.delete(metadata); // remove old entry
        }
        logger.info("Saved failed metadata");
        return RepeatStatus.FINISHED;
    }
}