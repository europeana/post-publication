package eu.europeana.postpublication.batch.utils;

import eu.europeana.corelib.definitions.edm.beans.FullBean;

import java.util.List;

public class BatchUtils {

    private BatchUtils() {
    }

    public static String[] getRecordIds(List<? extends FullBean> batchRecords) {
        return batchRecords.stream()
                .map(FullBean::getAbout)
                .toArray(String[]::new);
    }

}
