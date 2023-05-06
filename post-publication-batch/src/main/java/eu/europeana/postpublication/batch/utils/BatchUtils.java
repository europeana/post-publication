package eu.europeana.postpublication.batch.utils;

import eu.europeana.corelib.definitions.edm.beans.FullBean;

import java.util.List;
import java.util.Map;

public class BatchUtils {

    private BatchUtils() {
    }

    public static String[] getRecordIds(List<? extends FullBean> batchRecords) {
        return batchRecords.stream()
                .map(FullBean::getAbout)
                .toArray(String[]::new);
    }

    public static String getSetsToProcess(Map<String, List<String>> failedSetsOrRecords) {
        StringBuilder sets = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : failedSetsOrRecords.entrySet()) {
            if(entry.getValue().isEmpty()) {
                sets.append(entry.getKey() + ",");
            }
        }
        return  sets.toString();
    }

}
