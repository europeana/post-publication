package eu.europeana.postpublication.batch.model;

import org.apache.commons.lang3.StringUtils;

public enum ExecutionStep {

    TRANSLATIONS ("translation"),
    INDEXING ("indexing"),
    DEBIAS ("debias");

    private String step;

    ExecutionStep(String step) {
        this.step = step;
    }

    public String getStep() {
        return step;
    }

    public static ExecutionStep getStep(String step) {
        for (ExecutionStep executionStep : ExecutionStep.values()) {
            if (StringUtils.equalsIgnoreCase(executionStep.name(), step)) { return executionStep; }
        }
        return null;
    }
}
