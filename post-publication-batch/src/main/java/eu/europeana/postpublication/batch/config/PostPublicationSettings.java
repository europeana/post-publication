package eu.europeana.postpublication.batch.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:post-publication.properties")
@PropertySource(value = "classpath:post-publication.user.properties", ignoreResourceNotFound = true)
public class PostPublicationSettings implements InitializingBean {

    private static final Logger logger = LogManager.getLogger(PostPublicationSettings.class);

    @Value("${mongo.connectionUrl}")
    private String mongoConnectionUrl;

    @Value("${mongo.connectionUrl.write}")
    private String mongoConnectionUrlWrite;

    @Value("${mongo.batch.database}")
    private String batchDatabase;


    @Value("${mongo.writer.database}")
    private String writerDatabase;

    @Value("${mongo.max.idle.time.millisec: 10000}")
    private long mongoMaxIdleTimeMillisec;

    @Value("${batch.step.chunkSize: 100}")
    private int batchChunkSize;

    @Value("${batch.executor.corePool: 5}")
    private int batchCorePoolSize;

    @Value("${batch.step.skipLimit: 10}")
    private int batchSkipLimit;

    @Value("${batch.executor.maxPool: 10}")
    private int batchMaxPoolSize;

    @Value("${batch.step.executor.queueSize: 5}")
    private int batchQueueSize;

    @Value("${batch.step.throttleLimit: 5}")
    private int annoSyncThrottleLimit;

    @Value("${batch.retry:3}")
    private int retryLimit;


    @Value("${pp.initialDelaySeconds}")
    private int ppSyncInitialDelay;

    @Value("${pp.intervalSeconds}")
    private int ppSyncInterval;

    public String getMongoConnectionUrl() {
        return mongoConnectionUrl;
    }

    public String getMongoConnectionUrlWrite() {
        return mongoConnectionUrlWrite;
    }

    public String getBatchDatabase() {
        return batchDatabase;
    }

    public String getWriterDatabase() {
        return writerDatabase;
    }

    public long getMongoMaxIdleTimeMillisec() {
        return mongoMaxIdleTimeMillisec;
    }

    public int getBatchChunkSize() {
        return batchChunkSize;
    }

    public int getBatchCorePoolSize() {
        return batchCorePoolSize;
    }

    public int getBatchSkipLimit() {
        return batchSkipLimit;
    }

    public int getBatchMaxPoolSize() {
        return batchMaxPoolSize;
    }

    public int getBatchQueueSize() {
        return batchQueueSize;
    }

    public int getAnnoSyncThrottleLimit() {
        return annoSyncThrottleLimit;
    }

    public int getRetryLimit() {
        return retryLimit;
    }

    public int getPpSyncInitialDelay() {
        return ppSyncInitialDelay;
    }

    public int getPpSyncInterval() {
        return ppSyncInterval;
    }

    private void validateRequiredSettings() {
        if (StringUtils.equals(batchDatabase, writerDatabase)) {
            throw new IllegalStateException("Reader and writer Database must be different. Reader dB ::" +batchDatabase + ". Writer DB :: " +writerDatabase);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        validateRequiredSettings();
    }
}
