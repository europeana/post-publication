package eu.europeana.postpublication.batch.repository;

import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.experimental.filters.Filters;
import eu.europeana.postpublication.batch.config.PostPublicationSettings;
import eu.europeana.postpublication.batch.model.PostPublicationFailedMetadata;
import eu.europeana.postpublication.batch.utils.BatchUtils;
import eu.europeana.postpublication.service.BatchRecordService;
import eu.europeana.postpublication.service.FullBeanPublisher;
import eu.europeana.postpublication.utils.AppConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Repository
public class PostPublicationFailedRecordsRepo {

    private static final Logger logger = LogManager.getLogger(PostPublicationFailedRecordsRepo.class);

    private final Datastore datastore;
    private final BatchRecordService batchRecordService;
    private final FullBeanPublisher publisher;
    private final PostPublicationSettings settings;

    public PostPublicationFailedRecordsRepo(@Qualifier(AppConstants.BEAN_WRITER_DATA_STORE) Datastore datastore, BatchRecordService batchRecordService, FullBeanPublisher publisher, PostPublicationSettings settings) {
        this.datastore = datastore;
        this.batchRecordService = batchRecordService;
        this.publisher = publisher;
        this.settings = settings;
    }

    public PostPublicationFailedMetadata getPostPublicationFailedMetadata() {
        return datastore
                .find(PostPublicationFailedMetadata.class)
                .filter(Filters.eq("processed", false))
                .iterator(new FindOptions().limit(1))
                .tryNext();
    }

    public void save(PostPublicationFailedMetadata failedMetadata) {
        if (!failedMetadata.getFailedRecords().isEmpty()) {
            datastore.save(failedMetadata);
        }
    }

    public void delete(PostPublicationFailedMetadata failedMetadata) {
        datastore.delete(failedMetadata);
    }

    /**
     * Logs the progress of each set and record that was added for processing
     *
     * if failed data was present for processing, that is also included in the progress report
     *
     * It determines if the set is succesfully migrated to target DB
     * if not, check for the number of records that are missing.
     * If the number of records missing is more than 1/4 of the size of the set - set is identified a failed
     * If number of records missing is less than 1/4 of the size of the set - records that are missing are identified as failed
     *
     * Both the failed sets and records are added in Failed Metadata
     *
     * @param metadataToProcess
     * @return
     */
    public PostPublicationFailedMetadata progress(PostPublicationFailedMetadata metadataToProcess) {
        PostPublicationFailedMetadata failedMetadata = new PostPublicationFailedMetadata();
        List<String> datasetsProcessed =  settings.getDatasetsToProcess();
        List<String> recordsProcessed = new ArrayList<>();

        // add the failed sets and records
        if(metadataToProcess != null && !metadataToProcess.getFailedRecords().isEmpty()) {
            datasetsProcessed.addAll(BatchUtils.getSetsToProcess(metadataToProcess.getFailedRecords()));
            recordsProcessed = BatchUtils.getRecordsToProcess(metadataToProcess.getFailedRecords());
        }

        Map<String, List<String>> failedRecordsOrSets = new HashMap<>();

        // datasets progress
        datasetsProcessed.stream().forEach( set -> {
            long sourceRecords =  batchRecordService.getTotalRecordsForSet(set);
            long targetRecords = publisher.getTotalRecordsForSet(set);
            long failed = sourceRecords-targetRecords;
            logger.info("For dataset {} , Total records - {}, Migrated - {} , Failed - {}", set, sourceRecords, targetRecords, failed);
            if (failed > 0) {
                // if failed records are more than 1/4 of the size of the set then fail the set
                if (failed > (sourceRecords/4)) {
                    logger.info("Failing the set - {}", set);
                    failedRecordsOrSets.put(set, new ArrayList<>());
                } else {
                    //  otherwise add failed records for the set
                    List<String> recordIdsInSource =  batchRecordService.getRecordsIds(set);
                    recordIdsInSource.removeAll(new HashSet<>(publisher.getMigratedRecords(set)));
                    failedRecordsOrSets.put(set, recordIdsInSource);
                }
           }
        });

        failedMetadata.setFailedRecords(failedRecordsOrSets);
        logger.info("Total Failed - {}", BatchUtils.getTotalFailed(failedRecordsOrSets));
        logger.info("Failed sets to process - {}", BatchUtils.getSetsToProcess(failedRecordsOrSets));
    

        // records progress
        if (!recordsProcessed.isEmpty()) {
            List<String> processedSuccessfully = publisher.getRecordsIfExists(recordsProcessed);
            logger.info("Records to process - {}, Successfully processed - {}", recordsProcessed.size(), processedSuccessfully.size());

            if (processedSuccessfully.size() != recordsProcessed.size()) {
                recordsProcessed.removeAll(processedSuccessfully);
                failedRecordsOrSets.putAll(BatchUtils.convertListIntoMap(recordsProcessed));
            }
        }

        failedMetadata.setFailedRecords(failedRecordsOrSets);
        return failedMetadata;
    }

}

