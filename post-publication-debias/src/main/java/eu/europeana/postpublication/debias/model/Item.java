package eu.europeana.postpublication.debias.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Fields to be sent in item to Debias tool
 * Make sure the field names are same as the ones present in the Set INCLUDE_PROXY_MAP_FIELDS
 * @see eu.europeana.postpublication.debias.service.RecordAnnotationService.INCLUDE_PROXY_MAP_FIELDS
 *
 * But the json property value will be differnt hence add the @JsonProperty tag for the json value
 * @author srishti singh
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Item {

    private String id;

    @JsonProperty("dc:title")
    private List<String> dcTitle;

    @JsonProperty("dc:description")
    private List<String> dcDescription;

    @JsonProperty("dcterms:alternative")
    private List<String> dctermsAlternative;

    @JsonProperty("dc:subject")
    private List<String> dcSubject;

    @JsonProperty("dc:type")
    private List<String> dcType;

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

    public List<String> getDcSubject() {
        return dcSubject;
    }

    public void setDcSubject(List<String> dcSubject) {
        this.dcSubject = dcSubject;
    }

    public List<String> getDcType() {
        return dcType;
    }

    public void setDcType(List<String> dcType) {
        this.dcType = dcType;
    }

    @Override
    public String toString() {
        return "Item {" +
                "id='" + id + '\'' +
                ", dcTitle=" + dcTitle +
                ", dcDescription=" + dcDescription +
                ", dctermsAlternative=" + dctermsAlternative +
                ", dcSubject=" + dcSubject +
                ", dcType=" + dcType +  
                '}';
    }
}
