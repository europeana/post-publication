package eu.europeana.postpublication.batch;

import eu.europeana.postpublication.batch.model.ExecutionStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;

public interface AbstractPipeline {

    ExecutionStep getExecutionStep();

    ItemWriter getItemWriter() ;

    ItemProcessor getItemProcessor() ;

}
