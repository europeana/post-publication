package eu.europeana.postpublication.batch.reader;

import dev.morphia.query.experimental.filters.Filters;
import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.postpublication.batch.config.PostPublicationSettings;
import eu.europeana.postpublication.service.BatchRecordService;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Configuration
public class ItemReaderConfig {

    private final BatchRecordService batchRecordService;
    private final PostPublicationSettings postPublicationSettings;

    public ItemReaderConfig(BatchRecordService batchRecordService, PostPublicationSettings postPublicationSettings) {
        this.batchRecordService = batchRecordService;
        this.postPublicationSettings = postPublicationSettings;
    }

    public SynchronizedItemStreamReader<FullBean> createRecordReader(Instant currentStartTime, List<String> datasetToProcess) {

        RecordDatabaseReader reader =
                new RecordDatabaseReader(
                        batchRecordService, postPublicationSettings.getBatchChunkSize(),
                        // Fetch record whose timestampUpdated is more than currentStartTime
                        Filters.gte("timestampUpdated", currentStartTime),
                        // TODO this is added for trial testing. Will be removed later
                        Filters.in("europeanaCollectionName", datasetToProcess),
                        Filters.in("about", Arrays.asList("/298/item_3181765", "/298/item_3181763", "/298/item_2439914")));
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
