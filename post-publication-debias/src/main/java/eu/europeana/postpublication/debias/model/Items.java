package eu.europeana.postpublication.debias.model;

public class Items {

    private String id;
    private String[] dcTitle;
    private String[] dcDescription;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String[] getDcTitle() {
        return dcTitle;
    }

    public void setDcTitle(String[] dcTitle) {
        this.dcTitle = dcTitle;
    }

    public String[] getDcDescription() {
        return dcDescription;
    }

    public void setDcDescription(String[] dcDescription) {
        this.dcDescription = dcDescription;
    }
}
