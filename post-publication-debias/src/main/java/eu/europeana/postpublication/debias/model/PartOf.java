package eu.europeana.postpublication.debias.model;

import java.time.Instant;

public class PartOf {

    private String type ="AnnotationCollection";
    private long total;
    private String modified;

    public String getType() {
        return type;
    }

    public long getTotal() {
        return total;
    }

    public String getModified() {
        return modified;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }
}
