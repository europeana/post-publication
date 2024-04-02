package eu.europeana.postpublication.batch.pipelines;

import eu.europeana.postpublication.batch.AbstractPipeline;
import eu.europeana.postpublication.batch.model.ExecutionStep;
import eu.europeana.postpublication.batch.writer.SolrWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component("indexing")
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
    public ItemWriter getItemWriter() {
        return solrWriter;
    }

    // TODO probabaly will be tier calculation or something else
    // we don't have yet processor for that
    @Override
    public ItemProcessor getItemProcessor() {
        return null;
    }
}
