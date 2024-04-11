package eu.europeana.postpublication.batch.processor;

import eu.europeana.corelib.definitions.edm.beans.FullBean;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

// TODO processor would be Fullbean to debiasRequest OR fullbean to Annotation (depends on the flow)
@Component
public class AnnotationProcessor implements ItemProcessor<FullBean, FullBean> {

    @Override
    public FullBean process(FullBean bean) throws Exception {
        return bean;
    }
}
