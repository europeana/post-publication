package eu.europeana.postpublication.batch.config;

import eu.europeana.postpublication.utils.AppConstants;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;


@Configuration
public class PostPublicationBean {

    private final PostPublicationSettings settings;

    public PostPublicationBean(PostPublicationSettings settings) {
        this.settings = settings;
    }

    /** Task executor used by the Spring Batch step for multi-threading */
    @Bean(AppConstants.PP_SYNC_TASK_EXECUTOR)
    public TaskExecutor postPublicationTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(settings.getBatchCorePoolSize());
        taskExecutor.setMaxPoolSize(settings.getBatchMaxPoolSize());
        taskExecutor.setQueueCapacity(settings.getBatchQueueSize());

        return taskExecutor;
    }

    @Bean(AppConstants.PP_SYNC_TASK_SCHEDULAR)
    public TaskScheduler asyncTaskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    // TODO see if we need to add mongoBatchConfigurer

    //    /**
//     * Configures Spring Batch to use Mongo
//     *
//     * @param datastore Morphia datastore for Spring Batch
//     * @return BatchConfigurer instance
//     */
//    @Bean
//    public MongoBatchConfigurer mongoBatchConfigurer(
//            @Qualifier(AppConstants.BEAN_BATCH_DATA_STORE) Datastore datastore) {
//        return new MongoBatchConfigurer(datastore, defaultTaskExecutor);
//    }
}

