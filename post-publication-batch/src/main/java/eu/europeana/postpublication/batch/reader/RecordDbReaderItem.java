package eu.europeana.postpublication.batch.reader;

import dev.morphia.query.filters.Filter;
import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.postpublication.batch.config.PostPublicationSettings;
import eu.europeana.postpublication.service.BatchRecordService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.*;

@Component
public class RecordDbReaderItem extends AbstractItemCountingItemStreamItemReader<List<FullBean>>  {

    private static final Logger LOG = LogManager.getLogger(RecordDbReaderItem.class);

    private volatile int page = 0;

    private final Filter[] queryFilters;
    private final List<String> projectionFields;
    private final BatchRecordService recordService;
    private final PostPublicationSettings settings;


    public RecordDbReaderItem(BatchRecordService recordService, PostPublicationSettings settings, List<String> projectionFields, Filter... queryFilters) {
        this.recordService = recordService;
        this.projectionFields = projectionFields;
        this.queryFilters = queryFilters;
        this.settings = settings;
    }


    @Override
    protected void doOpen() {
        setSaveState(false); // mongo reader is not fault tolerant
        setName(RecordDbReaderItem.class.getSimpleName());
    }

    @PreDestroy
    @Override
    protected void doClose() {
        LOG.debug("Application is shutting down...");
    }

    @Override
    // Spring-Batch requires us to return null when we're done (S1168)
    // The start variable needs to be where it is, cannot be moved (S1941)
    @SuppressWarnings({"java:S1168", "java:S1941" })
    protected List<FullBean> doRead() {
        int start = page * settings.getBatchChunkSize();
        List<? extends FullBean> result = recordService.getNextPageOfRecords(start, settings.getBatchChunkSize(), queryFilters, projectionFields);



        if (result.isEmpty() || result.size() < settings.getBatchChunkSize()) {
            return null;
        }
        this.page++;

        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "Retrieved {} Records from database. skip={}, limit={}",
                    result.size(),
                    start,
                    settings.getBatchChunkSize());
        }
        return (List<FullBean>) result;
    }
}
