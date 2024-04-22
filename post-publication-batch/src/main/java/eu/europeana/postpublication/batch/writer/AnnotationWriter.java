package eu.europeana.postpublication.batch.writer;

import eu.europeana.annotation.definitions.model.Annotation;
import eu.europeana.postpublication.debias.service.AnnotationClientService;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnnotationWriter implements ItemWriter<Annotation> {

    private final AnnotationClientService annotationClientService;

    public AnnotationWriter(AnnotationClientService annotationClientService) {
        this.annotationClientService = annotationClientService;
    }

    @Override
    public void write(List<? extends Annotation> list) throws Exception {
        annotationClientService.createAnnotations(list);
    }
}
