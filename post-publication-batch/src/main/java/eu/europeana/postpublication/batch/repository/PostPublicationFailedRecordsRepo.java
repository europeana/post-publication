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

    public void progress(PostPublicationFailedMetadata failedMetadata) {
        List<String> datasetsProcessed =  settings.getDatasetsToProcess();

        Map<String, List<String>> failedRecordsOrSets = failedMetadata.getFailedRecords();

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
        logger.info("Sets to process - {}", BatchUtils.getSetsToProcess(failedRecordsOrSets));
    }



}

