package eu.europeana.postpublication.batch.pipelines;

import eu.europeana.postpublication.batch.AbstractPipeline;
import eu.europeana.postpublication.batch.model.ExecutionStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component("debias")
public class DebiasPipeline implements AbstractPipeline {

    @Override
    public ExecutionStep getExecutionStep() {
        return ExecutionStep.DEBIAS;
    }

    @Override
    public ItemWriter getItemWriter() {
        return null;
    }

    @Override
    public ItemProcessor getItemProcessor() {
        return null;
    }
}
