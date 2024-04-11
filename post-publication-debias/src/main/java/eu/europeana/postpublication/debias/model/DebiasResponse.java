package eu.europeana.postpublication.debias.model;

import eu.europeana.annotation.definitions.model.Annotation;

import java.util.ArrayList;
import java.util.List;

public class DebiasResponse {

    private Context context;
    private String type = "AnnotationPage";
    private PartOf partOf;

    private List<Annotation> items;

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public PartOf getPartOf() {
        return partOf;
    }

    public void setPartOf(PartOf partOf) {
        this.partOf = partOf;
    }

    public List<Annotation> getItems() {
        return items;
    }

    public void addItem(Annotation annotation) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(annotation);
    }

    public void setItems(List<Annotation> items) {
        this.items = items;
    }
}
