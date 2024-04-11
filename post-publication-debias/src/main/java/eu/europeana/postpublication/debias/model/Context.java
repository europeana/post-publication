package eu.europeana.postpublication.debias.model;

public class Context {
    private String base;

    public Context(String uri) { base = uri; }

    public String getURI()  { return "https://www.europeana.eu/schemas/context/edm.jsonld"; }

    public String getBase() { return base; }
}