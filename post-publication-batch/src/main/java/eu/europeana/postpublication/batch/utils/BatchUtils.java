package eu.europeana.postpublication.batch.utils;

import eu.europeana.corelib.definitions.edm.beans.FullBean;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BatchUtils {

    private BatchUtils() {
    }

    public static String[] getRecordIds(List<? extends FullBean> batchRecords) {
        return batchRecords.stream()
                .map(FullBean::getAbout)
                .toArray(String[]::new);
    }

    public static List<String> getRecordsToProcess(Map <String, List<String>> map) {
        List<String> recordToProcess = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                recordToProcess.addAll(entry.getValue());
            }
        }
        return recordToProcess;
    }

    public static List<String> getSetsToProcess(Map <String, List<String>> map) {
        List<String> sets = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (entry.getValue().isEmpty()) {
                sets.add(entry.getKey());
            }
        }
        return sets;
    }


    public static void processFailedData(Map<String, List<String>> failedRecords, List<String> datasetsToProcess, List<String> recordsToProcess) {
        for (Map.Entry<String, List<String>> entry : failedRecords.entrySet()) {
            if (entry.getValue().isEmpty()) {
                datasetsToProcess.add(entry.getKey());
            } else {
                recordsToProcess.addAll(entry.getValue());
            }
        }
    }

    public static Map<String, List<String>> convertListIntoMap(List<String> list) {
            return list.stream().collect(
                    Collectors.groupingBy(s -> StringUtils.substringBetween(s, "/" , "/"), HashMap::new, Collectors.toCollection(ArrayList::new))
            );
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

    public static int getTotalFailed(Map<String, List<String>> failedSetsOrRecords) {
        int failed =0;
        for (Map.Entry<String, List<String>> entry : failedSetsOrRecords.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                failed += entry.getValue().size();
            }
        }
        return failed;
    }

}
