package eu.europeana.postpublication.service;

import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import dev.morphia.query.experimental.filters.Filters;
import eu.europeana.corelib.solr.bean.impl.FullBeanImpl;
import eu.europeana.metis.mongo.utils.MorphiaUtils;
import eu.europeana.postpublication.utils.AppConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import dev.morphia.query.experimental.filters.Filter;
import java.util.List;


@Service(AppConstants.BEAN_BATCH_RECORD_SERVICE)
public class BatchRecordService {

    @Qualifier(AppConstants.BEAN_BATCH_DATA_STORE)
    private final Datastore datastore;


    @Autowired
    public BatchRecordService(Datastore datastore) {
        this.datastore = datastore;
    }

    /**
     * Fetches records matching the provided filter(s)
     *
     * @param start
     * @param count
     * @param queryFilters
     * @return
     */
    public List<FullBeanImpl> findRecordWithFilter(int start, int count, Filter[] queryFilters) {
        return this.datastore.find(FullBeanImpl.class).filter(queryFilters)
               .iterator(new FindOptions().skip(start).sort(Sort.ascending("timestampUpdated")).limit(count))
                .toList();
    }

    // start = nextPage*pagesize
    public List<FullBeanImpl> getNextPageOfRecords(int start, int PAGE_SIZE, Filter[] queryFilters) {
        Query<FullBeanImpl> query = this.datastore.find(FullBeanImpl.class);
        query.filter(queryFilters);
       // query.filter(Filters.regex("about").pattern("^/" + datasetId + "/"));
        return MorphiaUtils.getListOfQueryRetryable(query, new FindOptions().skip(start).limit(PAGE_SIZE));
    }

}
