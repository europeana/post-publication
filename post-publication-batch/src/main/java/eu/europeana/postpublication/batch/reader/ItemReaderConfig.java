package eu.europeana.postpublication.batch.reader;

import dev.morphia.query.experimental.filters.Filter;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.filters.RegexFilter;
import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.postpublication.batch.config.PostPublicationSettings;
import eu.europeana.postpublication.service.BatchRecordService;
import static eu.europeana.postpublication.utils.AppConstants.ABOUT;
import static eu.europeana.postpublication.utils.AppConstants.TIMESTAMP_UPDATED;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class ItemReaderConfig {

    private final BatchRecordService batchRecordService;
    private final PostPublicationSettings postPublicationSettings;

    public ItemReaderConfig(BatchRecordService batchRecordService, PostPublicationSettings postPublicationSettings) {
        this.batchRecordService = batchRecordService;
        this.postPublicationSettings = postPublicationSettings;
    }

    /**
     * Creates a database reader with query filters
     * {$match : {timestampUpdated :{$gte : "date"}}}
     * {$or : [{"about" : {$regex : '^/D1/'}},{"about" : {$regex : '^/D2/'}} , {"about" : {$regex : '^/D3/'}}, {"about" : {$in : ["record1", "record2" ]}}]}
     *
     * @param currentStartTime
     * @param datasetToProcess
     * @return
     */
    public SynchronizedItemStreamReader<FullBean> createRecordReader(Instant currentStartTime, List<String> datasetToProcess, List<String> recordsToProcess) {
        List<Filter> filters = new ArrayList<>();
        List<Filter> orFilters = new ArrayList<>();

        // TODO commnted out for now for the First DB migration with translations
        // Fetch record whose timestampUpdated is more than currentStartTime
//        if(currentStartTime != null) {
//            filters.add(Filters.gte(TIMESTAMP_UPDATED, currentStartTime));
//        }

        // add the regexFilter on about fields if datasets are present
        if (!datasetToProcess.isEmpty()) {
            List<RegexFilter> regexFilters = new ArrayList<>();
            datasetToProcess.stream().forEach(dataset -> regexFilters.add(Filters.regex(ABOUT).pattern("^/" + dataset + "/")));
            orFilters.addAll(regexFilters);
        }

        // add $in filter for records in the orFilter
        if (!recordsToProcess.isEmpty()) {
            orFilters.add(Filters.in(ABOUT, recordsToProcess));
        }

        // prepare the or filter
        filters.add(Filters.or(orFilters.toArray(new Filter[0])));

            RecordDatabaseReader reader =
                    new RecordDatabaseReader(
                            batchRecordService, postPublicationSettings.getBatchChunkSize(),
                            filters.toArray(new Filter[0]));
            return threadSafeReader(reader);

    }

    /** Makes ItemReader thread-safe */
    private <T> SynchronizedItemStreamReader<T> threadSafeReader(ItemStreamReader<T> reader) {
        final SynchronizedItemStreamReader<T> synchronizedItemStreamReader =
                new SynchronizedItemStreamReader<>();
        synchronizedItemStreamReader.setDelegate(reader);
        return synchronizedItemStreamReader;
    }
}
