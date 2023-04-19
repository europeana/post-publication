package eu.europeana.postpublication.batch.processor;

import eu.europeana.corelib.definitions.edm.beans.FullBean;

import eu.europeana.postpublication.translation.model.Language;
import eu.europeana.postpublication.translation.service.impl.RecordLangDetectionService;
import eu.europeana.postpublication.translation.service.impl.RecordTranslateService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Component
public class RecordProcessor implements ItemProcessor<FullBean, FullBean> {
    private static final Logger LOG = LogManager.getLogger(RecordProcessor.class);


    @Autowired
    private final RecordTranslateService translateFilterService;

    @Autowired
    private final RecordLangDetectionService langDetectionService;

    public RecordProcessor(RecordTranslateService translateFilterService, RecordLangDetectionService langDetectionService) {
        this.translateFilterService = translateFilterService;
        this.langDetectionService = langDetectionService;
    }

    @Override
    public FullBean process(@NotNull FullBean bean) throws Exception {
        bean = langDetectionService.detectLanguageForProxy(bean);

        LOG.info("rid:{} started",bean.getAbout());
        bean = translateFilterService.translateProxyFields(bean, Language.ENGLISH);

        //update the timestamp for the bean after processing
        bean.setTimestampUpdated(new Date());

        return bean;
    }
}
