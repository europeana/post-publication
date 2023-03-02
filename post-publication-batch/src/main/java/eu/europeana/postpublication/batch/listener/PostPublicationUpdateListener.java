package eu.europeana.postpublication.batch.listener;

import com.mongodb.lang.NonNull;
import eu.europeana.corelib.definitions.edm.beans.FullBean;
import org.springframework.batch.core.listener.ItemListenerSupport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import static eu.europeana.postpublication.batch.utils.BatchUtils.getRecordIds;

import java.util.List;

@Component
public class PostPublicationUpdateListener extends ItemListenerSupport<FullBean, FullBean> {

    private static final Logger logger = LogManager.getLogger(PostPublicationUpdateListener.class);

    @Override
    public void onReadError(@NonNull Exception e) {
        // No item linked to error, so we just log a warning
        logger.error("onReadError", e);
    }

    @Override
    public void onProcessError(@NonNull FullBean bean, @NonNull Exception e) {
        // just log warning for now
        logger.error(
                "Error processing Record id={}; recordId={}",
                bean.getId(),
                bean.getAbout(),
                e);
    }

    @Override
    public void onWriteError(@NonNull Exception ex, @NonNull List<? extends FullBean> annoPages) {
        logger.error("Error saving Records {}, ", getRecordIds(annoPages), ex);
    }
}
