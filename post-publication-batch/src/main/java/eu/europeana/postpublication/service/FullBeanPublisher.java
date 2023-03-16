package eu.europeana.postpublication.service;

import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.corelib.solr.bean.impl.FullBeanImpl;
import eu.europeana.indexing.mongo.FullBeanUpdater;
import eu.europeana.indexing.utils.TriConsumer;
import eu.europeana.metis.mongo.dao.RecordDao;
import eu.europeana.postpublication.exception.MongoConnnectionException;
import eu.europeana.postpublication.utils.AppConstants;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

@Component
public class FullBeanPublisher extends FullBeanUpdater {

    @Qualifier(AppConstants.RECORD_DAO)
    private final RecordDao edmMongoClient;

    @Qualifier(AppConstants.FULL_BEAN_PRE_PROCESSOR)
    private final TriConsumer<FullBeanImpl, FullBeanImpl, Pair<Date, Date>> fullBeanPreprocessor;


    public FullBeanPublisher(RecordDao edmMongoClient, TriConsumer<FullBeanImpl, FullBeanImpl, Pair<Date, Date>> fullBeanPreprocessor) {
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
    public List<String> publish(List<? extends FullBean> recordList) {
            List<String> recordUpdates = new ArrayList<>();
            for (FullBean record : recordList) {
                // TODO test if something goes wrong here while saving do we still add the saved Full bean about ID,
                //  That should not be added in the record updates list
                //  ALSO see how the dates will be saved
                FullBeanImpl savedFullBean = new FullBeanUpdater(fullBeanPreprocessor).update((FullBeanImpl) record, null,
                        record.getTimestampCreated(), edmMongoClient);
                recordUpdates.add(savedFullBean.getAbout());
            }
            return recordUpdates;
    }
}
