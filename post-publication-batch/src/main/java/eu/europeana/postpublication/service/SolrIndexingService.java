package eu.europeana.postpublication.service;

import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.corelib.edm.utils.EdmUtils;
import eu.europeana.corelib.solr.bean.impl.FullBeanImpl;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.indexing.exception.SetupRelatedIndexingException;
import eu.europeana.indexing.solr.SolrIndexer;
import eu.europeana.indexing.solr.SolrIndexingSettings;
import eu.europeana.metis.schema.jibx.RDF;
import eu.europeana.postpublication.utils.AppConstants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SolrIndexingService extends SolrIndexer {

    @Qualifier(AppConstants.SOLR_INDEXING_SETTING_BEAN)
    private final SolrIndexingSettings solrIndexingSettings;

    // TODO while testing see if Solrindexer stringToRdfConverterSupplier is instantiated correctly
    public SolrIndexingService(SolrIndexingSettings solrIndexingSettings) throws SetupRelatedIndexingException {
        super(solrIndexingSettings);
        this.solrIndexingSettings = solrIndexingSettings;
    }


    // TODO - don't like that we have to convert Fullbean to RDF and then later metis SolrIndexer calls the
    //  FullBeanPublisher.publishSolr() and converts the rdf into FullBean again.
    //  Although i see both rdf and full bean are being used to pulish to solr. Need to ask the logic behind this from Metis
    public void publshToSolr(List<? extends FullBean> recordList) throws IndexingException {
        for (FullBean fullBean : recordList) {
            // For indexing, we preserve the identifiers
            RDF rdf = EdmUtils.toRDF((FullBeanImpl) fullBean, true);
            indexRecord(rdf);
        }
    }

}
