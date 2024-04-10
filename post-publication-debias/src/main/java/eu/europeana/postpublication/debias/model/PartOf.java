package eu.europeana.postpublication.debias.model;

import java.time.Instant;

public class PartOf {

    private String type ="AnnotationCollection";
    private long total;
    private Instant modified;

    public PartOf(long total, String modified) {
        this.total = total;
        this.modified = Instant.parse(modified);
    }

    public String getType() {
        return type;
    }

    public long getTotal() {
        return total;
    }

    public Instant getModified() {
        return modified;
    }
}
