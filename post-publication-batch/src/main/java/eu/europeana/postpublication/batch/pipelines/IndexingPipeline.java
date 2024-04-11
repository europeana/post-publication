package eu.europeana.postpublication.batch.pipelines;

import eu.europeana.postpublication.batch.AbstractPipeline;
import eu.europeana.postpublication.batch.model.ExecutionStep;
import eu.europeana.postpublication.batch.writer.SolrWriter;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IndexingPipeline implements AbstractPipeline {

    private final SolrWriter solrWriter;

    public IndexingPipeline(SolrWriter solrWriter) {
        this.solrWriter = solrWriter;
    }

    @Override
    public ExecutionStep getExecutionStep() {
        return ExecutionStep.INDEXING;
    }

    @Override
    public List<String> getFieldsToFetchFromReader() {
        return null;
    }

    @Override
    public ItemWriter getItemWriter() {
        return solrWriter;
    }

    // TODO probabaly will be tier calculation or something else
    // we don't have yet processor for that
    @Override
    public ItemProcessor getItemProcessor() {
        return null;
    }

    @Override
    public ItemProcessListener getItemProcessListener() {
        return null;
    }
}
