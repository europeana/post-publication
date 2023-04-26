package eu.europeana.postpublication.batch;

import eu.europeana.postpublication.batch.model.PostPublicationJobMetadata;
import eu.europeana.postpublication.batch.repository.PostPublicationJobMetadataRepo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class PostPublicationMetadataUpdaterTasklet implements Tasklet {

  private final PostPublicationJobMetadataRepo repository;
  private final PostPublicationJobMetadata metadata;
  private static final Logger logger = LogManager.getLogger(PostPublicationMetadataUpdaterTasklet.class);

  public PostPublicationMetadataUpdaterTasklet(
          PostPublicationJobMetadataRepo repository, PostPublicationJobMetadata metadata) {
    this.repository = repository;
    this.metadata = metadata;
  }

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext)
      throws Exception {
    repository.save(metadata);
    logger.info("Saved post publication metadata {}", metadata.getLastSuccessfulStartTime());

    return RepeatStatus.FINISHED;
  }
}
