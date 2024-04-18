package eu.europeana.postpublication.batch;

import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.postpublication.batch.model.ExecutionStep;
import eu.europeana.postpublication.batch.model.PostPublicationFailedMetadata;
import eu.europeana.postpublication.batch.model.PostPublicationJobMetadata;
import eu.europeana.postpublication.batch.reader.ItemReaderConfig;
import eu.europeana.postpublication.batch.repository.PostPublicationFailedRecordsRepo;
import eu.europeana.postpublication.batch.repository.PostPublicationJobMetadataRepo;
import eu.europeana.postpublication.batch.utils.BatchUtils;
import eu.europeana.postpublication.batch.config.PostPublicationSettings;
import eu.europeana.postpublication.exception.MongoConnnectionException;
import eu.europeana.postpublication.utils.AppConstants;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@EnableBatchProcessing
public class PostPublicationJobConfig {

    private static final Logger logger = LogManager.getLogger(PostPublicationJobConfig.class);

    private static final String POST_PUBLICATION_PIPELINE = "postPublicationPipeline";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final PostPublicationSettings postPublicationSettings;
    private final PipelineRegistryHandler pipelineRegistryHandler;
    private final ExecutionStep executionStep;
    private final ItemReaderConfig itemReaderConfig;
    private final BatchSyncStats stats;
    private final PostPublicationJobMetadataRepo postPublicationJobMetaRepository;
    private final PostPublicationFailedRecordsRepo postPublicationFailedRecordsRepository;
    private final TaskExecutor postPublicationTaskExecutor;

    public PostPublicationJobConfig(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory,
                                    PostPublicationSettings postPublicationSettings,
                                    PipelineRegistryHandler pipelineRegistryHandler, @Qualifier(AppConstants.EXECUTION_STEPS_BEAN) ExecutionStep executionStep,
                                    ItemReaderConfig itemReaderConfig,
                                    BatchSyncStats stats, PostPublicationJobMetadataRepo postPublicationJobMetaRepository, PostPublicationFailedRecordsRepo postPublicationFailedRecordsRepository,
                                    @Qualifier(AppConstants.PP_SYNC_TASK_EXECUTOR) TaskExecutor postPublicationTaskExecutor) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.postPublicationSettings = postPublicationSettings;
        this.pipelineRegistryHandler = pipelineRegistryHandler;
        this.executionStep = executionStep;
        this.itemReaderConfig = itemReaderConfig;
        this.stats = stats;
        this.postPublicationJobMetaRepository = postPublicationJobMetaRepository;
        this.postPublicationFailedRecordsRepository = postPublicationFailedRecordsRepository;
        this.postPublicationTaskExecutor = postPublicationTaskExecutor;
    }


    // TODO failed repo logic is removed for now.
    @Bean
    public Job syncRecords() {
        if (!postPublicationSettings.IsFrameworkEnabled()) {
            return null;
        }

        Instant from = Instant.EPOCH;

        Instant startTime = Instant.now();
        List<String> datasetsToProcess = postPublicationSettings.getDatasetsToProcess();
        List<String> fieldsToFetch = pipelineRegistryHandler.get(executionStep).getFieldsToFetchFromReader();
        // add the failed sets and records for processing
        List<String> recordsToProcess = new ArrayList<>();

        if (logger.isInfoEnabled()) {
            logger.info(
                    "Starting post publication pipeline job. Fetching datasets - {} , records - {} ",
                    datasetsToProcess, recordsToProcess);
        }

        return this.jobBuilderFactory
                .get(POST_PUBLICATION_PIPELINE)
                .start(initStats(stats, startTime))
                .next(executePipeline(from, datasetsToProcess, recordsToProcess, fieldsToFetch))
                .next(finishStats(stats, startTime))
//                .next(updatePostPublicationJobMetadata(jobMetadata))
//                .next(updatePostPublicationJobFailedMetadata(failedMetadata))
                .build();
    }

    /**
     *
     * Depending on the Execution step in the property file - pipeline is exceuted with the specific processor, writer and listeners
     *
     * Please look the #PipelineRegistryHandler to see the various pipelines configured
     * For now supported ones are - Translations, Debias and Indexing (still work in progress)
     *
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
    private Step executePipeline(Instant start, List<String> datasetsToProcess, List<String> recordsToProcess, List<String> fieldsToFetch) {
        SynchronizedItemStreamReader reader = executionStep.equals(ExecutionStep.DEBIAS)
                ? itemReaderConfig.createItemReader(datasetsToProcess, fieldsToFetch)
                : itemReaderConfig.createRecordReader(start, datasetsToProcess, recordsToProcess, fieldsToFetch);

        return this.stepBuilderFactory
                .get("executePipeline")
                .chunk(postPublicationSettings.getBatchChunkSize())
                .reader(reader)
                .processor(pipelineRegistryHandler.get(executionStep).getItemProcessor())
                .writer(pipelineRegistryHandler.get(executionStep).getItemWriter())
                .listener(pipelineRegistryHandler.get(executionStep).getItemProcessListener())
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

