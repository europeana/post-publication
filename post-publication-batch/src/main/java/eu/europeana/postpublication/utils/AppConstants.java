package eu.europeana.postpublication.utils;

public class AppConstants {


    private AppConstants() {
    }

    public static final String BEAN_BATCH_RECORD_SERVICE = "batchRecordSevice";

    public static final String BEAN_BATCH_DATA_STORE = "batchDataStore";
    public static final String RECORD_DAO = "recordDao";
    public static final String BEAN_WRITER_DATA_STORE = "recordDaoDatastore";

    public static final String FULL_BEAN_PRE_PROCESSOR = "fullBeanPreprocessor";
    public static final String TRANSLATION_SERVICE_BEAN = "translationService";
    public static final String LANGUAGE_DETECTION_SERVICE_BEAN = "languageDetectionService";


    public static final String PP_SYNC_TASK_EXECUTOR = "postPublicationTaskExecutor";
    public static final String PP_SYNC_TASK_SCHEDULAR = "postPublicationSyncTaskScheduler";

}
