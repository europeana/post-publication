package eu.europeana.postpublication.batch.pipelines;

import eu.europeana.postpublication.batch.AbstractPipeline;
import eu.europeana.postpublication.batch.model.ExecutionStep;
import eu.europeana.postpublication.batch.processor.AnnotationProcessor;
import eu.europeana.postpublication.batch.writer.AnnotationWriter;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class DebiasPipeline implements AbstractPipeline {

    private final AnnotationWriter annotationWriter;

    private final AnnotationProcessor annotationProcessor;

    public DebiasPipeline(AnnotationWriter annotationWriter, AnnotationProcessor annotationProcessor) {
        this.annotationWriter = annotationWriter;
        this.annotationProcessor = annotationProcessor;
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
        return null;
    }
}
