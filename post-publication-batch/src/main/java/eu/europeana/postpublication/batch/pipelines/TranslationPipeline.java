package eu.europeana.postpublication.batch.pipelines;

import eu.europeana.postpublication.batch.AbstractPipeline;
import eu.europeana.postpublication.batch.listener.RecordUpdateListener;
import eu.europeana.postpublication.batch.model.ExecutionStep;
import eu.europeana.postpublication.batch.processor.RecordProcessor;
import eu.europeana.postpublication.batch.writer.RecordWriter;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TranslationPipeline implements AbstractPipeline {

    private final RecordProcessor recordProcessor;
    private final RecordWriter recordWriter;
    private final RecordUpdateListener recordUpdateListener;

    public TranslationPipeline(RecordWriter recordWriter, RecordProcessor recordProcessor, RecordUpdateListener recordUpdateListener) {
        this.recordWriter = recordWriter;
        this.recordProcessor = recordProcessor;
        this.recordUpdateListener = recordUpdateListener;
    }

    @Override
    public ExecutionStep getExecutionStep() {
        return ExecutionStep.TRANSLATIONS;
    }

    @Override
    public List<String> getFieldsToFetchFromReader() {
        return null;
    }

    @Override
    public ItemWriter getItemWriter() {
        return recordWriter;
    }


    @Override
    public ItemProcessor getItemProcessor() {
        return recordProcessor;
    }

    @Override
    public ItemProcessListener getItemProcessListener() {
        return this.recordUpdateListener;
    }
}
