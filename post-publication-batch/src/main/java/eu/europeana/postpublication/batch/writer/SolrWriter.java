package eu.europeana.postpublication.batch.writer;

import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.postpublication.service.SolrIndexingService;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SolrWriter implements ItemWriter<FullBean> {

    private final SolrIndexingService solrIndexingService;

    public SolrWriter(SolrIndexingService solrIndexingService) {
        this.solrIndexingService = solrIndexingService;
    }

    @Override
    public void write(List<? extends FullBean> list) throws Exception {
        solrIndexingService.publshToSolr(list);
    }
}
