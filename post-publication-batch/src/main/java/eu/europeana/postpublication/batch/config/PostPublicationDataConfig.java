package eu.europeana.postpublication.batch.config;

import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import eu.europeana.batch.entity.JobExecutionEntity;
import eu.europeana.corelib.solr.bean.impl.FullBeanImpl;
import eu.europeana.indexing.utils.TriConsumer;
import eu.europeana.metis.mongo.dao.RecordDao;
import eu.europeana.postpublication.translation.service.PangeanicV2TranslationService;
import eu.europeana.postpublication.translation.service.RecordTranslateService;
import eu.europeana.postpublication.translation.service.TranslationService;
import eu.europeana.postpublication.utils.AppConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Date;

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
        RecordDao recordDao = new RecordDao(MongoClients.create(settings.getMongoReadConnectionUrl()), settings.getReadDatabase(), true);
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
        RecordDao recordDao = new RecordDao(MongoClients.create(settings.getMongoWriteConnectionUrl()), settings.getWriteDatabase(), true);
        return recordDao;
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
     * RecordTranslateService bean
     * @return
     */
    @Bean(name = AppConstants.RECORD_TRANSLATION_SERVICE_BEAN)
    public RecordTranslateService recordTranslateService() {
        return new RecordTranslateService(translationService());
    }
}
