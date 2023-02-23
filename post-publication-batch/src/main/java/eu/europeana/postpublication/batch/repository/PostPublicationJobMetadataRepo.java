package eu.europeana.postpublication.batch.repository;

import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Sort;
import eu.europeana.postpublication.batch.model.PostPublicationJobMetadata;
import eu.europeana.postpublication.utils.AppConstants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class PostPublicationJobMetadataRepo {

    private final Datastore datastore;

    public PostPublicationJobMetadataRepo(@Qualifier(AppConstants.BEAN_WRITER_DATA_STORE) Datastore datastore) {
        this.datastore = datastore;
    }

    public PostPublicationJobMetadata getMostRecentPostPublicationMetadata() {
        return datastore
                .find(PostPublicationJobMetadata.class)
                .iterator(new FindOptions().sort(Sort.descending("lastSuccessfulStartTime")).limit(1))
                .tryNext();
    }

    public void save(PostPublicationJobMetadata metadata) {
        datastore.save(metadata);
    }

}
