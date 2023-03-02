package eu.europeana.postpublication.batch;

import eu.europeana.postpublication.batch.config.PostPublicationSettings;
import eu.europeana.postpublication.utils.AppConstants;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;

// TODO will figure out later once we how the scheduling will be done for post publication pipeline.
//  For now the code to run schedule updates is not being called
@Configuration
public class PostPublicationSchedulingConfig implements InitializingBean {

    private final PostPublicationJobConfig postPublicationJobConfig;

    private final TaskScheduler ppTaskScheduler;
    private static final Logger logger = LogManager.getLogger(PostPublicationSchedulingConfig.class);

    private final JobLauncher jobLauncher;
    private final int ppSyncInitialDelay;
    private final int ppSyncInterval;


    public PostPublicationSchedulingConfig(
            PostPublicationJobConfig postPublicationJobConfig,
            @Qualifier(AppConstants.PP_SYNC_TASK_SCHEDULAR) TaskScheduler ppTaskScheduler,
            JobLauncher launcher,
            PostPublicationSettings appSettings) {
        this.postPublicationJobConfig = postPublicationJobConfig;
        this.ppTaskScheduler = ppTaskScheduler;
        this.jobLauncher = launcher;
        this.ppSyncInitialDelay = appSettings.getPpSyncInitialDelay();
        this.ppSyncInterval = appSettings.getPpSyncInterval();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
//        if (logger.isInfoEnabled()) {
//            logger.info(
//                    "Post publication pipeline scheduling initialized – initialDelay: {}; interval: {}",
//                    toMinutesAndSeconds(ppSyncInitialDelay),
//                    toMinutesAndSeconds(ppSyncInterval));
//        }

       // schedulePeriodicAnnoSync();
    }

    private void schedulePeriodicAnnoSync() {
        ppTaskScheduler.scheduleWithFixedDelay(
                this::runScheduledAnnoSyncJob,
                Instant.now().plusSeconds(ppSyncInitialDelay),
                Duration.ofSeconds(ppSyncInterval));
    }

    /**
     * Periodically run full entity updates.
     */
    void runScheduledAnnoSyncJob() {
        logger.info("Triggering scheduled Post publication pipeline job");
        try {
            String startTimeJobParam = "startTime";
            jobLauncher.run(
                    postPublicationJobConfig.syncRecords(),
                    new JobParametersBuilder()
                            .addDate(startTimeJobParam, Date.from(Instant.now()))
                            .toJobParameters());
        } catch (Exception e) {
            logger.warn("Error running Post publication pipeline job", e);
        }
    }

    /**
     * Converts Seconds to "x min, y sec"
     */
    private String toMinutesAndSeconds(long seconds) {
        return String.format(
                "%d min, %d sec",
                TimeUnit.SECONDS.toMinutes(seconds),
                seconds - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(seconds)));
    }
}

