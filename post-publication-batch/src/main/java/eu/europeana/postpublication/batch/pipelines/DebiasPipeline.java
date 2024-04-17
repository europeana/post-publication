package eu.europeana.postpublication.batch.pipelines;

import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.postpublication.batch.AbstractPipeline;
import eu.europeana.postpublication.batch.listener.RecordUpdateListener;
import eu.europeana.postpublication.batch.model.ExecutionStep;
import eu.europeana.postpublication.batch.processor.AnnotationProcessor;
import eu.europeana.postpublication.batch.reader.ItemReaderConfig;
import eu.europeana.postpublication.batch.writer.AnnotationWriter;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class DebiasPipeline implements AbstractPipeline {

    private final AnnotationWriter annotationWriter;

    private final AnnotationProcessor annotationProcessor;

    private final RecordUpdateListener listener;

    public DebiasPipeline(AnnotationWriter annotationWriter, AnnotationProcessor annotationProcessor, RecordUpdateListener listener) {
        this.annotationWriter = annotationWriter;
        this.annotationProcessor = annotationProcessor;
        this.listener = listener;
    }

    @Override
    public ExecutionStep getExecutionStep() {
        return ExecutionStep.DEBIAS;
    }

    @Override
    public List<String> getFieldsToFetchFromReader() {
      return new ArrayList<>(Arrays.asList("about", "proxies"));
    }

    @Override
    public ItemWriter getItemWriter() {
        return this.annotationWriter;
    }

    @Override
    public ItemProcessor getItemProcessor() {
        return this.annotationProcessor;
    }

    @Override
    public ItemProcessListener getItemProcessListener() {
        return listener;
    }
}
