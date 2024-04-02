package eu.europeana.postpublication.batch.pipelines;

import eu.europeana.postpublication.batch.AbstractPipeline;
import eu.europeana.postpublication.batch.model.ExecutionStep;
import eu.europeana.postpublication.batch.processor.RecordProcessor;
import eu.europeana.postpublication.batch.writer.RecordWriter;
import eu.europeana.postpublication.utils.AppConstants;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("translation")
public class TranslationPipeline implements AbstractPipeline {

    private final RecordProcessor recordProcessor;
    private final RecordWriter recordWriter;
    private final ExecutionStep step;

    public TranslationPipeline(RecordWriter recordWriter, RecordProcessor recordProcessor, @Qualifier(AppConstants.EXECUTION_STEPS_BEAN) ExecutionStep step) {
        this.recordWriter = recordWriter;
        this.recordProcessor = recordProcessor;
        this.step = step;
    }

    @Override
    public ExecutionStep getExecutionStep() {
        return ExecutionStep.TRANSLATIONS;
    }

    @Override
    public ItemWriter getItemWriter() {
        return recordWriter;
    }

    /**
     * if there is TRANSLATIONS step then translate the record
     * or else just simply migrate without any processing
     * @return
     */
    @Override
    public ItemProcessor getItemProcessor() {
        if (step.equals(ExecutionStep.TRANSLATIONS)) {
            return recordProcessor;
        }
        return  null;
    }
}
