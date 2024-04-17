package eu.europeana.postpublication.debias.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Item {

    private String id;

    @JsonProperty("dc:title")
    private List<String> dcTitle;

    @JsonProperty("dc:description")
    private List<String> dcDescription;

    @JsonProperty("dc:termsAlternative")
    private List<String> dctermsAlternative;

    public Item(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getDcTitle() {
        return dcTitle;
    }

    public void setDcTitle(List<String> dcTitle) {
        this.dcTitle = dcTitle;
    }

    public List<String> getDcDescription() {
        return dcDescription;
    }

    public void setDcDescription(List<String> dcDescription) {
        this.dcDescription = dcDescription;
    }

    public List<String> getDctermsAlternative() {
        return dctermsAlternative;
    }

    public void setDctermsAlternative(List<String> dctermsAlternative) {
        this.dctermsAlternative = dctermsAlternative;
    }

    @Override
    public String toString() {
        return "Item {" +
                "id='" + id + '\'' +
                ", dcTitle=" + dcTitle +
                ", dcDescription=" + dcDescription +
                ", dctermsAlternative=" + dctermsAlternative +
                '}';
    }
}
