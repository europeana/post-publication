package eu.europeana.postpublication.batch.processor;

import eu.europeana.corelib.definitions.edm.beans.FullBean;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

@Component
public class RecordProcessor implements ItemProcessor<FullBean, FullBean> {

    @Override
    public FullBean process(@NotNull FullBean bean) throws Exception {
        return bean;
    }
}
