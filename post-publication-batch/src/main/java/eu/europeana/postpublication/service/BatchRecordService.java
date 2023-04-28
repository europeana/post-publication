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

import static eu.europeana.postpublication.utils.AppConstants.ABOUT;
import static eu.europeana.postpublication.utils.AppConstants.TIMESTAMP_UPDATED;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


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
               .iterator(new FindOptions().skip(start).sort(Sort.ascending(TIMESTAMP_UPDATED)).limit(count))
                .toList();
    }

    /**
     * Uses pagination to fetch the records using MorphiaUtils from metis-common-mongo
     * @param start value corresponds to page * pageSize
     * @param pageSize  number of records to fetch
     * @param queryFilters filters applied
     * @return
     */
    public List<FullBeanImpl> getNextPageOfRecords(int start, int pageSize, Filter[] queryFilters) {
        Query<FullBeanImpl> query = this.datastore.find(FullBeanImpl.class);
        query.filter(queryFilters);
        return MorphiaUtils.getListOfQueryRetryable(query, new FindOptions().skip(start).limit(pageSize));
    }

    /**
     * Get the list of recordsIds for the set
     * @param datasetId datasets for records count
     * @return
     */
    public List<String> getRemainingRecords(String datasetId, List<String> europeanaIds) {
        List<String> projectionFields = new ArrayList<>();
        projectionFields.add(ABOUT);

        List<Filter> filters = new ArrayList<>();
        filters.add(Filters.regex(ABOUT).pattern("^/" + datasetId + "/"));
        // add the id's
        if (!europeanaIds.isEmpty()) {
            filters.add(Filters.nin(ABOUT, europeanaIds));
        }

        Query<FullBeanImpl> query = this.datastore.find(FullBeanImpl.class)
                .filter(filters.toArray(new Filter[0]));

        return MorphiaUtils.getListOfQueryRetryable(query, new FindOptions().projection().include(projectionFields.toArray(String[]::new)))
                .stream().map(FullBeanImpl::getAbout).collect(Collectors.toList());
    }


    /**
     * Get the number of Records for the dataset
     * @param datasetId datasets for records count
     * @return
     */
    public long getTotalRecordsForSet(String datasetId) {
        Query<FullBeanImpl> query = this.datastore.find(FullBeanImpl.class);
        query.filter(Filters.regex("about").pattern("^/" + datasetId + "/"));
        return query.count();
    }

}
