package eu.europeana.postpublication.batch.reader;

import dev.morphia.query.filters.Filter;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.filters.RegexFilter;
import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.postpublication.batch.config.PostPublicationSettings;
import eu.europeana.postpublication.service.BatchRecordService;
import static eu.europeana.postpublication.utils.AppConstants.ABOUT;

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
     * {$or : [{"about" : {$regex : '^/D1/'}},{"about" : {$regex : '^/D2/'}} , {"about" : {$regex : '^/D3/'}}}}]}
     *
     * @param datasetToProcess datasets to be processed
     * @param fieldsToFetch projection fields to be fetched
     * @return
     */
    public SynchronizedItemStreamReader<List<FullBean>> createItemReader(List<String> datasetToProcess, List<String> fieldsToFetch) {
        // create filters for datasets only
        List<Filter> filters = createFilterForMongoReader(null, datasetToProcess, new ArrayList<>());

        RecordDbReaderItem itemReader = new RecordDbReaderItem(batchRecordService, postPublicationSettings, fieldsToFetch, filters.toArray(new Filter[0]));
        return  threadSafeReader(itemReader);
    }

    /**
     * Creates a database reader with query filters
     * {$match : {timestampUpdated :{$gte : "date"}}}
     * {$or : [{"about" : {$regex : '^/D1/'}},{"about" : {$regex : '^/D2/'}} , {"about" : {$regex : '^/D3/'}}, {"about" : {$in : ["record1", "record2" ]}}]}
     *
     * @param currentStartTime
     * @param datasetToProcess
     * @param recordsToProcess records to be processed
     * @param fieldsToFetch projection fields to be fetched
     * @return
     */
    public SynchronizedItemStreamReader<FullBean> createRecordReader(Instant currentStartTime, List<String> datasetToProcess, List<String> recordsToProcess,
                                                                     List<String> fieldsToFetch) {
        List<Filter> filters = createFilterForMongoReader(currentStartTime, datasetToProcess, recordsToProcess);
        RecordDbReaderPaginated reader = new RecordDbReaderPaginated(
                batchRecordService,
                postPublicationSettings.getBatchChunkSize(),
                fieldsToFetch, filters.toArray(new Filter[0]));

        return threadSafeReader(reader);
    }

    /** Makes ItemReader thread-safe */
    private <T> SynchronizedItemStreamReader<T> threadSafeReader(ItemStreamReader<T> reader) {
        final SynchronizedItemStreamReader<T> synchronizedItemStreamReader =
                new SynchronizedItemStreamReader<>();
        synchronizedItemStreamReader.setDelegate(reader);
        return synchronizedItemStreamReader;
    }

    /**
     * Creates query filters
     * {$match : {timestampUpdated :{$gte : "date"}}}
     * {$or : [{"about" : {$regex : '^/D1/'}},{"about" : {$regex : '^/D2/'}} , {"about" : {$regex : '^/D3/'}}, {"about" : {$in : ["record1", "record2" ]}}]}
     *
     * @param currentStartTime
     * @param datasetToProcess
     * @param recordsToProcess
     * @return
     */
    private List<Filter> createFilterForMongoReader(Instant currentStartTime, List<String> datasetToProcess, List<String> recordsToProcess) {
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
        return filters;
    }
}
