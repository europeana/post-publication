package eu.europeana.postpublication.batch.processor;

import eu.europeana.annotation.definitions.model.Annotation;
import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.postpublication.debias.service.RecordAnnotationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AnnotationProcessor implements ItemProcessor<List<FullBean>, List<Annotation>> {

    private static final Logger logger = LogManager.getLogger(AnnotationProcessor.class);

    @Autowired
    private final RecordAnnotationService recordAnnotationService;

    public AnnotationProcessor(RecordAnnotationService recordAnnotationService) {
        this.recordAnnotationService = recordAnnotationService;
    }

    @Override
    public List<Annotation> process(List<FullBean> fullBeans) throws Exception {
        logger.debug("processing {} items" , fullBeans.size());
        //"processing {} - {}", fullBeans.size(), fullBeans.stream().map(f -> f.getAbout()).collect(Collectors.toList());
        return recordAnnotationService.process(fullBeans);
    }
}
