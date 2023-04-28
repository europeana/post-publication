package eu.europeana.postpublication.batch;

import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.postpublication.batch.listener.PostPublicationUpdateListener;
import eu.europeana.postpublication.batch.model.PostPublicationFailedMetadata;
import eu.europeana.postpublication.batch.model.PostPublicationJobMetadata;
import eu.europeana.postpublication.batch.processor.RecordProcessor;
import eu.europeana.postpublication.batch.reader.ItemReaderConfig;
import eu.europeana.postpublication.batch.repository.PostPublicationFailedRecordsRepo;
import eu.europeana.postpublication.batch.repository.PostPublicationJobMetadataRepo;
import eu.europeana.postpublication.batch.writer.RecordWriter;
import eu.europeana.postpublication.batch.config.PostPublicationSettings;
import eu.europeana.postpublication.exception.MongoConnnectionException;
import eu.europeana.postpublication.utils.AppConstants;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@EnableBatchProcessing
public class PostPublicationJobConfig {

    private static final Logger logger = LogManager.getLogger(PostPublicationJobConfig.class);

    private static final String POST_PUBLICATION_PIPELINE = "postPublicationPipeline";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    private final RecordProcessor recordProcessor;
    private final RecordWriter recordWriter;
    private final PostPublicationSettings postPublicationSettings;
    private final PostPublicationUpdateListener postPublicationUpdateListener;

    private final ItemReaderConfig itemReaderConfig;
    private final BatchSyncStats stats;

    private final PostPublicationJobMetadataRepo postPublicationJobMetaRepository;
    private final PostPublicationFailedRecordsRepo postPublicationFailedRecordsRepository;

    private final TaskExecutor postPublicationTaskExecutor;


    public PostPublicationJobConfig(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, RecordProcessor recordProcessor,
                                    RecordWriter recordWriter, PostPublicationSettings postPublicationSettings, PostPublicationUpdateListener postPublicationUpdateListener, ItemReaderConfig itemReaderConfig,
                                    BatchSyncStats stats, PostPublicationJobMetadataRepo postPublicationJobMetaRepository, PostPublicationFailedRecordsRepo postPublicationFailedRecordsRepository, @Qualifier(AppConstants.PP_SYNC_TASK_EXECUTOR) TaskExecutor postPublicationTaskExecutor) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.recordProcessor = recordProcessor;
        this.recordWriter = recordWriter;
        this.postPublicationSettings = postPublicationSettings;
        this.postPublicationUpdateListener = postPublicationUpdateListener;
        this.itemReaderConfig = itemReaderConfig;
        this.stats = stats;
        this.postPublicationJobMetaRepository = postPublicationJobMetaRepository;
        this.postPublicationFailedRecordsRepository = postPublicationFailedRecordsRepository;
        this.postPublicationTaskExecutor = postPublicationTaskExecutor;
    }


    @Bean
    public Job syncRecords() {
        if(!postPublicationSettings.IsFrameworkEnabled()) {
            return null;
        }

        PostPublicationFailedMetadata failedMetadata = new PostPublicationFailedMetadata();

        PostPublicationJobMetadata jobMetadata = postPublicationJobMetaRepository.getMostRecentPostPublicationMetadata();
        Instant from = Instant.EPOCH;

        Instant startTime = Instant.now();

        // take from value from previous run if it exists
        if (jobMetadata != null) {
            from = jobMetadata.getLastSuccessfulStartTime();
        } else {
            jobMetadata = new PostPublicationJobMetadata();
        }

        // set the job metadata LastSuccessfulStartTime
        jobMetadata.setLastSuccessfulStartTime(startTime);
        List<String> datasetsToProcess = postPublicationSettings.getDatasetsToProcess();

        if (logger.isInfoEnabled()) {
            logger.info(
                    "Starting post publication pipeline job. Fetching Records update after {}",
                    from);
        }

        return this.jobBuilderFactory
                .get(POST_PUBLICATION_PIPELINE)
                .start(initStats(stats, startTime))
                .next(migrateRecordsStep(from, datasetsToProcess))
                .next(finishStats(stats, startTime))
                .next(updatePostPublicationJobMetadata(jobMetadata))
                .next(updatePostPublicationJobFailedMetadata(failedMetadata))
                .build();
    }

    /**
     *  Few Points :
     *
     *  #processorNonTransactional :: Have marked the item processor as non-transactional (default is the opposite).
     *  If this flag is set the results of item processing are cached across transactions in between retries and
     *  during skip processing, otherwise the processor will be called in every transaction.
     *  Hence, re-processing everything again and duplicating the values.
     *
     * @param start
     * @return
     */
    private Step migrateRecordsStep(Instant start, List<String> datasetsToProcess) {
        return this.stepBuilderFactory
                .get("migrateRecordsStep")
                .<FullBean, FullBean>chunk(postPublicationSettings.getBatchChunkSize())
                .reader(itemReaderConfig.createRecordReader(start, datasetsToProcess))
                .processor(recordProcessor)
                .writer(recordWriter)
                .listener((ItemProcessListener<? super FullBean, ? super FullBean>) postPublicationUpdateListener)
                .faultTolerant()
                .processorNonTransactional()
                .retryLimit(postPublicationSettings.getRetryLimit())
                .retry(MongoConnnectionException.class) // retry if MongoDb is down for some reason
                .skipLimit(postPublicationSettings.getBatchSkipLimit())
                .skip(Exception.class)
                .taskExecutor(postPublicationTaskExecutor)
                .throttleLimit(postPublicationSettings.gePpSyncThrottleLimit())
                .build();
    }

    private Step initStats(BatchSyncStats stats, Instant startTime) {
        return stepBuilderFactory
                .get("initStatsStep")
                .tasklet(
                        ((stepContribution, chunkContext) -> {
                            stats.reset();
                            stats.setStartTime(startTime);
                            return RepeatStatus.FINISHED;
                        }))
                .build();
    }

    private Step finishStats(BatchSyncStats stats, Instant startTime) {
        return stepBuilderFactory
                .get("finishStatsStep")
                .tasklet(
                        ((stepContribution, chunkContext) -> {
                            stats.setElapsedTime(Duration.between(startTime, Instant.now()));
                            return RepeatStatus.FINISHED;
                        }))
                .build();
    }

    private Step updatePostPublicationJobMetadata(PostPublicationJobMetadata jobMetadata) {
        return stepBuilderFactory
                .get("updateJobMetadataStep")
                .tasklet(new PostPublicationMetadataUpdaterTasklet(postPublicationJobMetaRepository, jobMetadata))
                .build();
    }


    private Step updatePostPublicationJobFailedMetadata(PostPublicationFailedMetadata failedMetadata) {
        return stepBuilderFactory
               .get("progressReport")
               .tasklet(new PostPublicationFailedRecordsUpdaterTasklet(postPublicationFailedRecordsRepository, failedMetadata))
               .build();

    }

}

