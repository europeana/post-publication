package eu.europeana.postpublication.batch.processor;

import eu.europeana.corelib.definitions.edm.beans.FullBean;

import eu.europeana.postpublication.translation.model.Language;
import eu.europeana.postpublication.translation.service.impl.RecordTranslateService;
import eu.europeana.postpublication.utils.AppConstants;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

@Component
public class RecordProcessor implements ItemProcessor<FullBean, FullBean> {

    @Qualifier(AppConstants.RECORD_TRANSLATION_SERVICE_BEAN)
    private final RecordTranslateService translateFilterService;

    public RecordProcessor(RecordTranslateService translateFilterService) {
        this.translateFilterService = translateFilterService;
    }

    @Override
    public FullBean process(@NotNull FullBean bean) throws Exception {
        bean = translateFilterService.translateProxyFields(bean, Language.ENGLISH);
        return bean;
    }
}
