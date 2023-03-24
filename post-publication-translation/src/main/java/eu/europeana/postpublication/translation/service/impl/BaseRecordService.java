package eu.europeana.postpublication.translation.service.impl;

import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.corelib.definitions.edm.entity.ContextualClass;
import eu.europeana.corelib.definitions.edm.entity.Proxy;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

public abstract class BaseRecordService {

    private static final Logger LOG = LogManager.getLogger(BaseRecordService.class);

    // TODO check this field value in DB dctermsTableOfContents there is a chnace it is named dcTermsTOC
    private static final Set<String> INCLUDE_PROXY_MAP_FIELDS = Set.of("dcContributor", "dcCoverage", "dcCreator", "dcDate", "dcDescription", "dcFormat", "dcIdentifier",
            "dcLanguage", "dcPublisher", "dcRelation", "dcRights", "dcSource", "dcSubject", "dcTitle", "dcType", "dctermsAlternative", "dctermsConformsTo", "dctermsCreated",
            "dctermsExtent", "dctermsHasFormat", "dctermsHasPart", "dctermsHasVersion", "dctermsIsFormatOf", "dctermsIsPartOf", "dctermsIsReferencedBy", "dctermsIsReplacedBy",
            "dctermsIsRequiredBy", "dctermsIssued", "dctermsIsVersionOf", "dctermsMedium", "dctermsProvenance", "dctermsReferences", "dctermsReplaces", "dctermsRequires",
            "dctermsSpatial", "dctermsTableOfContents", "dctermsTemporal", "edmCurrentLocation", "edmHasMet", "edmHasType", "edmIsRelatedTo", "edmType");

    private static final List<String> ENTITIES = List.of("agents", "concepts", "places", "timespans");

    // TODO still to be determined for now has dummy values
    protected static final List<String> PRECENDANCE_LIST = List.of("de", "nl", "fr", "pt");

    protected static final ReflectionUtils.FieldFilter proxyFieldFilter = field -> field.getType().isAssignableFrom(Map.class) &&
            INCLUDE_PROXY_MAP_FIELDS.contains(field.getName());

    /**
     * Function to get the lang-value map of the field from the proxy Object
     * @param proxy
     * @return
     */
    public static Function<String, Map<String, List<String>>> getValueOfTheField(Proxy proxy) {
        return e -> {
            Field field = ReflectionUtils.findField(proxy.getClass(), e);
            ReflectionUtils.makeAccessible(field);
            Object value = ReflectionUtils.getField(field, proxy);
            // if the field doesn't exist, set an empty map in the proxy object
            if (value == null) {
                ReflectionUtils.setField(field, proxy, new LinkedHashMap<>());
                value = ReflectionUtils.getField(field, proxy);
            }
            if (value instanceof Map) {
                return (Map<String, List<String>>) value;
            } else if (value != null) { // should not happen as the whitelisted values are all lang-map
                LOG.warn("Unexpected data - field {} did not return a map", e);
            }
            return new LinkedHashMap<>(); // default return an empty map
        };
    }

    /**
     * Finds the Contextual entity from the bean matching the uri
     * @param bean record
     * @param uri url to check
     * @return
     */
    public static ContextualClass entityExistsWithUrl(FullBean bean, String uri) {
        List<ContextualClass> matchingEntity= new ArrayList<>();

        // check only entity objects
        ReflectionUtils.FieldFilter entityFilter = field -> ENTITIES.contains(field.getName());

        ReflectionUtils.doWithFields(bean.getClass(), field -> {
            // if we found the Contextual class already, no need to iterate more
            if (matchingEntity.size() == 1) {
                return;
            }
            ReflectionUtils.makeAccessible(field);
            Object o = ReflectionUtils.getField(field, bean);
            LOG.trace("Searching for entities with type {}...", field.getName());
            // check only if it's a list and is not empty
            if (o instanceof List && !((List<?>) o).isEmpty()) {
                List<ContextualClass> entities = (List<ContextualClass>) o;
                for (ContextualClass entity : entities) {
                    if (StringUtils.equalsIgnoreCase(uri, entity.getAbout())) {
                        LOG.debug(" Found matching entity {}", entity.getAbout());
                        matchingEntity.add(entity);
                        break;
                    }
                }
            }
        }, entityFilter);

        // return Contextual Class if found or else null
        return matchingEntity.isEmpty() ? null : matchingEntity.get(0);
    }
}


