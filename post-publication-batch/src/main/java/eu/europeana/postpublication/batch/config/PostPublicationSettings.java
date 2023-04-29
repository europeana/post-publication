package eu.europeana.postpublication.batch.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@PropertySource("classpath:post-publication.properties")
@PropertySource(value = "classpath:post-publication.user.properties", ignoreResourceNotFound = true)
public class PostPublicationSettings implements InitializingBean {

    @Value("${run.post.publication:#{true}}")
    private Boolean isFrameworkEnabled;

    @Value("${mongo.read.connectionUrl}")
    private String mongoReadConnectionUrl;

    @Value("${mongo.write.connectionUrl}")
    private String mongoWriteConnectionUrl;

    @Value("${mongo.read.database}")
    private String readDatabase;

    @Value("${mongo.writer.database}")
    private String writeDatabase;

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
    private int ppSyncThrottleLimit;

    @Value("${batch.retry:3}")
    private int retryLimit;

    @Value("${pp.initialDelaySeconds}")
    private int ppSyncInitialDelay;

    @Value("${pp.intervalSeconds}")
    private int ppSyncInterval;


    @Value("${process.datasets}")
    private String datasetsToProcess;

    public boolean IsFrameworkEnabled() {
        return isFrameworkEnabled;
    }

    public String getMongoReadConnectionUrl() {
        return mongoReadConnectionUrl;
    }

    public String getMongoWriteConnectionUrl() {
        return mongoWriteConnectionUrl;
    }

    public String getReadDatabase() {
        return readDatabase;
    }

    public String getWriteDatabase() {
        return writeDatabase;
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

    public int gePpSyncThrottleLimit() {
        return ppSyncThrottleLimit;
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

    public List<String> getDatasetsToProcess() {
        if(StringUtils.isNotEmpty(datasetsToProcess)) {
            return new ArrayList<>(Arrays.asList(datasetsToProcess.split("\\s*,\\s*")));
        }
        return new ArrayList<>();
    }

    private void validateRequiredSettings() {
        if (StringUtils.equals(writeDatabase, readDatabase)) {
            throw new IllegalStateException("Reader and writer Database must be different. Reader dB ::" +readDatabase + ". Writer DB :: " +writeDatabase);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        validateRequiredSettings();
    }
}
