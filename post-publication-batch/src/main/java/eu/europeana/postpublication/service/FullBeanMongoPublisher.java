package eu.europeana.postpublication.service;

import com.mongodb.MongoException;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.experimental.filters.Filters;
import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.corelib.solr.bean.impl.FullBeanImpl;
import eu.europeana.indexing.mongo.FullBeanUpdater;
import eu.europeana.indexing.utils.TriConsumer;
import eu.europeana.metis.mongo.dao.RecordDao;
import eu.europeana.metis.mongo.utils.MorphiaUtils;
import eu.europeana.postpublication.exception.MongoConnnectionException;
import eu.europeana.postpublication.utils.AppConstants;;
import org.springframework.beans.factory.annotation.Qualifier;
import static eu.europeana.postpublication.utils.AppConstants.ABOUT;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

@Component
public class FullBeanMongoPublisher extends FullBeanUpdater {

    @Qualifier(AppConstants.RECORD_DAO)
    private final RecordDao edmMongoClient;

    @Qualifier(AppConstants.FULL_BEAN_PRE_PROCESSOR)
    private final TriConsumer<FullBeanImpl, FullBeanImpl, Pair<Date, Date>> fullBeanPreprocessor;


    public FullBeanMongoPublisher(RecordDao edmMongoClient, TriConsumer<FullBeanImpl, FullBeanImpl, Pair<Date, Date>> fullBeanPreprocessor) {
        super(fullBeanPreprocessor);
        this.edmMongoClient = edmMongoClient;
        this.fullBeanPreprocessor = fullBeanPreprocessor;
    }

    /**
     * Publishes the list of records via edmMongoClient
     * @param recordList
     * @return
     * @throws MongoConnnectionException
     */
    public List<String> publishToMongo(List<? extends FullBean> recordList) throws MongoConnnectionException {
            List<String> recordUpdates = new ArrayList<>();
            try {
                for (FullBean fullBean : recordList) {
                    FullBeanImpl savedFullBean = new FullBeanUpdater(fullBeanPreprocessor).update((FullBeanImpl) fullBean, null,
                            fullBean.getTimestampCreated(), edmMongoClient);
                    recordUpdates.add(savedFullBean.getAbout()); // only add the processed ones
                }
            } catch (MongoException e) { // for retry for connection issues throw MongoConnnectionException
                if (e instanceof MongoSocketException || e instanceof MongoTimeoutException) {
                    throw new MongoConnnectionException("Error while connecting to Mongo -"  +e.getMessage());
                }
            }
            return recordUpdates;
    }

    /**
     * Get the total records exists for the set
     * @param datasetId datasets for records count
     * @return
     */
    public long getTotalRecordsForSet(String datasetId) {
        Query<FullBeanImpl> query = this.edmMongoClient.getDatastore().find(FullBeanImpl.class);
        query.filter(Filters.regex(ABOUT).pattern("^/" + datasetId + "/"));
        return query.count();
    }

    /**
     * Get the records
     * @param recordIds datasets for records count
     * @return
     */
    public List<String> getRecordsIfExists(List<String> recordIds) {
        List<String> projectionFields = new ArrayList<>();
        projectionFields.add(ABOUT);

        Query<FullBeanImpl> query = this.edmMongoClient.getDatastore().find(FullBeanImpl.class)
                .filter(Filters.in(ABOUT, recordIds));

        return MorphiaUtils.getListOfQueryRetryable(query, new FindOptions().projection().include(projectionFields.toArray(String[]::new)))
                .stream().map(FullBeanImpl :: getAbout).collect(Collectors.toList());
    }

    /**
     * Get the number of Records for the dataset
     * @param datasetId datasets for records count
     * @return
     */
    public List<String> getMigratedRecords(String datasetId) {
        List<String> projectionFields = new ArrayList<>();
        projectionFields.add(ABOUT);

        Query<FullBeanImpl> query = this.edmMongoClient.getDatastore().find(FullBeanImpl.class)
                .filter(Filters.regex(ABOUT).pattern("^/" + datasetId + "/"));

        return MorphiaUtils.getListOfQueryRetryable(query, new FindOptions().projection().include(projectionFields.toArray(String[]::new)))
                .stream().map(FullBeanImpl :: getAbout).collect(Collectors.toList());
    }
}
