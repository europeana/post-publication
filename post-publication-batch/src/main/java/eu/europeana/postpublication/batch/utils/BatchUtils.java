package eu.europeana.postpublication.batch.utils;

import eu.europeana.corelib.definitions.edm.beans.FullBean;

import java.util.List;

public class BatchUtils {

    public static String[] getRecordIds(List<? extends FullBean> batchRecords) {
        return batchRecords.stream()
                .map(record -> record.getAbout())
                .toArray(String[]::new);
    }

}
