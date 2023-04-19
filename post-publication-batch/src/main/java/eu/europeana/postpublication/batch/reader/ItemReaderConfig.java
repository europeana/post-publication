package eu.europeana.postpublication.batch.reader;

import dev.morphia.query.experimental.filters.Filter;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.filters.RegexFilter;
import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.postpublication.batch.config.PostPublicationSettings;
import eu.europeana.postpublication.service.BatchRecordService;
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
     * {$or : [{"about" : {$regex : '^/D1/'}},{"about" : {$regex : '^/D2/'}} , {"about" : {$regex : '^/D3/'}}]}
     *
     * @param currentStartTime
     * @param datasetToProcess
     * @return
     */
    public SynchronizedItemStreamReader<FullBean> createRecordReader(Instant currentStartTime, List<String> datasetToProcess) {
        // add the regexFilter
        List<RegexFilter> regexFilters = new ArrayList<>();
        datasetToProcess.stream().forEach(dataset -> regexFilters.add(Filters.regex("about").pattern("^/" + dataset +"/")));

        RecordDatabaseReader reader =
                new RecordDatabaseReader(
                        batchRecordService, postPublicationSettings.getBatchChunkSize(),
                        // Fetch record whose timestampUpdated is more than currentStartTime
                        //Filters.gte("timestampUpdated", currentStartTime),
                        Filters.or(regexFilters.toArray(new Filter[0])));
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
