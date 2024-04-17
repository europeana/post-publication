package eu.europeana.postpublication.debias.model;

public class Params {

    private long limitPerPredicate;
    private String language;
    private boolean provenance;

    public Params(long limitPerPredicate, String language, boolean provenance) {
        this.limitPerPredicate = limitPerPredicate;
        this.language = language;
        this.provenance = provenance;
    }

    public long getLimitPerPredicate() {
        return limitPerPredicate;
    }

    public void setLimitPerPredicate(long limitPerPredicate) {
        this.limitPerPredicate = limitPerPredicate;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isProvenance() {
        return provenance;
    }

    public void setProvenance(boolean provenance) {
        this.provenance = provenance;
    }
}
