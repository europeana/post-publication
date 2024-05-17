package eu.europeana.postpublication.batch.writer;

import eu.europeana.annotation.definitions.model.Annotation;
import eu.europeana.postpublication.debias.service.AnnotationClientService;
import java.util.ArrayList;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnnotationWriter implements ItemWriter<List<Annotation>> {

    private final AnnotationClientService annotationClientService;

    public AnnotationWriter(AnnotationClientService annotationClientService) {
        this.annotationClientService = annotationClientService;
    }



    @Override
    public void write(List<? extends List<Annotation>> list) throws Exception {
        //Annotation processor returns the List of Annotations which are then considered as single list item in
        // org.springframework.batch.item.ItemWriter.write  method hence while calling annotation the items are put in sing list of Annotations

        List<Annotation> consolidatedList = new ArrayList<>();
        for(List<Annotation> subList :  list) {
            consolidatedList.addAll(subList);
        }
        annotationClientService.createAnnotations(consolidatedList);
    }
}
