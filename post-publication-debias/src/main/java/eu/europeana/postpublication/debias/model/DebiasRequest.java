package eu.europeana.postpublication.debias.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import eu.europeana.postpublication.debias.utils.SerialisationUtils;

import java.util.List;

@JsonAppend(prepend = true, attrs = { @JsonAppend.Attr(value = SerialisationUtils.context) })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DebiasRequest {

    private String type ="Request";
    private Params params;
    private long totalItems;
    private List<Item> items;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Params getParams() {
        return params;
    }

    public void setParams(Params params) {
        this.params = params;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
