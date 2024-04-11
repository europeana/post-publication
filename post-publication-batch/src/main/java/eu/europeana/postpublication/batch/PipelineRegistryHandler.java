package eu.europeana.postpublication.batch;

import eu.europeana.postpublication.batch.model.ExecutionStep;
import eu.europeana.postpublication.batch.pipelines.DebiasPipeline;
import eu.europeana.postpublication.batch.pipelines.IndexingPipeline;
import eu.europeana.postpublication.batch.pipelines.TranslationPipeline;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class PipelineRegistryHandler extends HashMap<ExecutionStep, AbstractPipeline> {

    private final IndexingPipeline indexingPipeline;
    private final DebiasPipeline debiasPipeline;
    private final TranslationPipeline translationPipeline;


    public PipelineRegistryHandler(IndexingPipeline indexingPipeline, DebiasPipeline debiasPipeline, TranslationPipeline translationPipeline) {
        this.indexingPipeline = indexingPipeline;
        this.debiasPipeline = debiasPipeline;
        this.translationPipeline = translationPipeline;

        put(ExecutionStep.INDEXING, this.indexingPipeline);
        put(ExecutionStep.DEBIAS, this.debiasPipeline);
        put(ExecutionStep.TRANSLATIONS, this.translationPipeline);

    }
}
