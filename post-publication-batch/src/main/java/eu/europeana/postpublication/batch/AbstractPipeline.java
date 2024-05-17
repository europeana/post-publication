package eu.europeana.postpublication.batch;

import eu.europeana.postpublication.batch.model.ExecutionStep;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.listener.ItemListenerSupport;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public interface AbstractPipeline {

    ExecutionStep getExecutionStep();

    List<String> getFieldsToFetchFromReader();

    ItemWriter getItemWriter() ;

    ItemProcessor getItemProcessor() ;

    ItemProcessListener getItemProcessListener();

}
