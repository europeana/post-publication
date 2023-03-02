package eu.europeana.postpublication.batch.writer;

import com.mongodb.MongoException;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.postpublication.batch.BatchSyncStats;
import eu.europeana.postpublication.exception.MongoConnnectionException;
import eu.europeana.postpublication.service.FullBeanPublisher;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.List;

@Component
public class RecordWriter implements ItemWriter<FullBean>{

    private final FullBeanPublisher publisher;
    private final BatchSyncStats stats;

    public RecordWriter(FullBeanPublisher publisher, BatchSyncStats stats) {
        this.publisher = publisher;
        this.stats = stats;
    }

    @Override
    public void write(@NotNull List<? extends FullBean> list) throws Exception {
        try {
            List<String> recordsUpdated = publisher.publish(list);
            for (int i = 0; i < recordsUpdated.size(); i++) {
                stats.addUpdated();
            }
            // failed
            addFailedRecords((list.size() - recordsUpdated.size()));

        } catch (MongoException e) {
            if (e instanceof MongoSocketException || e instanceof MongoTimeoutException) {
                throw new MongoConnnectionException("Error while connecting to Mongo -"  +e.getMessage());
            }
        }
    }

    private void addFailedRecords(int failedRecordsSize) {
        if(failedRecordsSize != 0) {
            for (int i =0 ; i < failedRecordsSize; i ++) {
                stats.addFailed();
            }
        }

    }
}
