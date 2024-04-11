package eu.europeana.postpublication.batch.writer;

import eu.europeana.corelib.definitions.edm.beans.FullBean;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;


// TODO writer needs to be modified based on where the annotations have to be saved
@Component
public class AnnotationWriter implements ItemWriter<FullBean> {

    @Override
    public void write(List<? extends FullBean> list) throws Exception {

    }
}
