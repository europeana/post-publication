package eu.europeana.postpublication.batch.config;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import eu.europeana.batch.entity.JobExecutionEntity;
import eu.europeana.corelib.solr.bean.impl.FullBeanImpl;
import eu.europeana.indexing.exception.SetupRelatedIndexingException;
import eu.europeana.indexing.solr.SolrIndexingSettings;
import eu.europeana.indexing.utils.TriConsumer;
import eu.europeana.metis.mongo.dao.RecordDao;
import eu.europeana.metis.solr.connection.SolrProperties;
import eu.europeana.postpublication.batch.model.ExecutionStep;
import eu.europeana.postpublication.translation.service.LanguageDetectionService;
import eu.europeana.postpublication.translation.service.pangeanic.PangeanicV2LangDetectService;
import eu.europeana.postpublication.translation.service.pangeanic.PangeanicV2TranslationService;
import eu.europeana.postpublication.translation.service.TranslationService;
import eu.europeana.postpublication.utils.AppConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Configuration
public class PostPublicationDataConfig {

    private static final Logger logger = LogManager.getLogger(PostPublicationDataConfig.class);

    private final PostPublicationSettings settings;

    private static final TriConsumer<FullBeanImpl, FullBeanImpl, Pair<Date, Date>> EMPTY_PREPROCESSOR = (created, updated, recordDateAndCreationDate) -> {
    };


    public PostPublicationDataConfig(PostPublicationSettings settings) {
        this.settings = settings;
    }


    /**
     * Configures Morphia data store for the batch job repository
     * Batch database is the DB from which the records will be read from.
     *
     * In PP pipeline we have to read and write in the different MongoDB
     *
     * @return data store for Spring batch JobRepository
     */
    @Primary
    @Bean(name = AppConstants.BEAN_BATCH_DATA_STORE)
    public Datastore batchDataStore() {
        logger.info("Configuring Batch (reader) database: {}", settings.getReadDatabase());
        RecordDao recordDao = new RecordDao(MongoClients.create(settings.getMongoReadConnectionUrl()), settings.getReadDatabase(), false);
        recordDao.getDatastore().getMapper().mapPackage(JobExecutionEntity.class.getPackageName());
        return recordDao.getDatastore();
    }

    /**
     * Configures Record Dao
     * This is configured to write the Records in the writer database
     * In PP pipeline we have to read and write in the different MongoDB
     *
     * @return RecordDao
     */
    @Bean(name = AppConstants.RECORD_DAO)
    public RecordDao recordDao() {
        logger.info("Configuring writer database: {}", settings.getWriteDatabase());
        return new RecordDao(MongoClients.create(settings.getMongoWriteConnectionUrl()), settings.getWriteDatabase(), true);
    }

    @Bean(name = AppConstants.BEAN_WRITER_DATA_STORE)
    public Datastore recordDaoDatastore() {
        return recordDao().getDatastore();
    }

    /**
     * Configures TriConsumer Bean
     *
     * @return default TriConsumer
     */
    // TODO decide later what consumer do we want when we plugin metis-indexing,
    //  for now we can work with the defaults
    @Bean(name = AppConstants.FULL_BEAN_PRE_PROCESSOR)
    public TriConsumer fullBeanPreprocessor() {
        return EMPTY_PREPROCESSOR;
    }


    // solr indexing beans

    /**
     * Solr properties solr properties.
     *
     * @return the solr properties
     * @throws URISyntaxException the uri syntax exception
     * @throws SetupRelatedIndexingException the setup related indexing exception
     */
    @Bean
    SolrProperties<SetupRelatedIndexingException> solrProperties() throws SetupRelatedIndexingException {
        try {
            logger.info("Configuring the solr properties for indexing - {}" , settings.getSolrUrl());
            SolrProperties<SetupRelatedIndexingException> solrProperties = new SolrProperties<>(SetupRelatedIndexingException::new);
            solrProperties.addSolrHost(new URI(settings.getSolrUrl()));
            return solrProperties;
        } catch (URISyntaxException e) {
            throw new SetupRelatedIndexingException("Invalid solr host !!!");
        }
    }

    @Bean(name = AppConstants.SOLR_INDEXING_SETTING_BEAN)
    public SolrIndexingSettings solrIndexingSettings() throws SetupRelatedIndexingException {
        return new SolrIndexingSettings(solrProperties());
    }


    /**
     * Translation Service bean
     * Currently for Post publication pipeline only Pangeanic
     * Translation Engine is required
     * @return
     */
    @Bean(name = AppConstants.TRANSLATION_SERVICE_BEAN)
    public TranslationService translationService() {
        return new PangeanicV2TranslationService();
    }

    /**
     * lang Detection Service bean
     * Currently for Post publication pipeline only Pangeanic
     * @return
     */
    @Bean(name = AppConstants.LANGUAGE_DETECTION_SERVICE_BEAN)
    public LanguageDetectionService detectionService() {
        return new PangeanicV2LangDetectService();
    }


    /**
     * Will create list of valid steps.
     * @return list of steps to be exceuted
     */
    @Bean(name = AppConstants.EXECUTION_STEPS_BEAN)
    public List<ExecutionStep> getExecutionSteps() {
        List<ExecutionStep> executionSteps = new ArrayList<>();
        for (String value: settings.getStepsToExecute()) {
            ExecutionStep step = ExecutionStep.getStep(value);
            if(step != null) {
                executionSteps.add(step);
            }
        }
        logger.info("Configured steps for execution: {}", executionSteps);
        return executionSteps;
    }
}
