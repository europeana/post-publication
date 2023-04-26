package eu.europeana.postpublication.batch.reader;

import dev.morphia.query.experimental.filters.Filter;
import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.postpublication.batch.utils.BatchUtils;
import eu.europeana.postpublication.service.BatchRecordService;
import org.springframework.batch.item.ItemReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * {@link ItemReader} that reads documents from MongoDB via a paging technique.
 * @author srishti singh
 * */

public class RecordDatabaseReader extends BaseDatabaseReader<FullBean> {

    private static final Logger logger = LogManager.getLogger(RecordDatabaseReader.class);

    private final Filter[] queryFilters;
    private final BatchRecordService recordService;

    public RecordDatabaseReader(BatchRecordService recordService, int pageSize, Filter... queryFilters) {
        super(pageSize);
        this.recordService = recordService;
        this.queryFilters = queryFilters;
    }

    @Override
    protected Iterator<FullBean> doPageRead() {
        // number of items to skip when reading. pageSize is incremented in parent class every time
        // this method is invoked
        int start = page * pageSize;

        List<? extends FullBean> result = recordService.getNextPageOfRecords( start, pageSize, queryFilters);
        if (result == null || result.isEmpty()) {
            return null;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Retrieved {} Records from database. skip={}, limit={}",
                    result.size(),
                    start,
                    pageSize);
        }

        return (Iterator<FullBean>) result.iterator();
    }

    @Override
    String getClassName() {
        return RecordDatabaseReader.class.getSimpleName();
    }
}
