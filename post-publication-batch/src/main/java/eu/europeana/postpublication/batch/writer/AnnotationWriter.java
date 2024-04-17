package eu.europeana.postpublication.batch.writer;

import eu.europeana.annotation.definitions.model.Annotation;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnnotationWriter implements ItemWriter<Annotation> {

    @Override
    public void write(List<? extends Annotation> list) throws Exception {
    }
}
