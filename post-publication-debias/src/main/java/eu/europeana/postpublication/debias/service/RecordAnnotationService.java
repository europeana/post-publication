package eu.europeana.postpublication.debias.service;

import eu.europeana.annotation.definitions.model.Annotation;
import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.corelib.definitions.edm.entity.Concept;
import eu.europeana.corelib.definitions.edm.entity.Proxy;
import eu.europeana.postpublication.debias.exception.DebiasException;
import eu.europeana.postpublication.debias.model.*;

import java.util.Map.Entry;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

@Service
public class RecordAnnotationService {

    private static final Logger LOG = LogManager.getLogger(RecordAnnotationService.class);

    /**
     * Look for the exact field name in class -
     * @see eu.europeana.corelib.solr.entity.PhysicalThingImpl
     * so if in future we have to add the fields -  "dcSubject", "dcType" in this set INCLUDE_PROXY_MAP_FIELDS
     * Also in the
     *     @see eu.europeana.postpublication.debias.model.Item class add the fields with the same name
     *
     * The same name startegy is beneficial while adding and updaing the field value while processing bean
     * in both Item and FullBean class
     *
     */
    private static final Set<String> INCLUDE_PROXY_MAP_FIELDS = Set.of("dcTitle", "dctermsAlternative", "dcDescription","dcSubject","dcType");

    protected static final ReflectionUtils.FieldFilter proxyFieldFilter = field -> field.getType().isAssignableFrom(Map.class) &&
        INCLUDE_PROXY_MAP_FIELDS.contains(field.getName());

    private static final List<String> FIELD_WITH_POTENTIAL_REFERENCE_VAL = List.of("dcSubject","dcType");

    private final DebiasService debiasService;

    public RecordAnnotationService(DebiasService debiasService) {
        this.debiasService = debiasService;
    }

    /**
     * Method to process single bean for the debias case
     * @param fullBeans beans to be processed
     * @return List of Annotations
     *
     * @throws DebiasException
     */
    public List<Annotation> process(List<FullBean> fullBeans) throws DebiasException {
        long start = System.currentTimeMillis();
        List<Annotation> annotations = new ArrayList<>();
        Map<String, List<Item>> itemsMap = new LinkedHashMap<>();

        for (FullBean bean : fullBeans) {
            List<Proxy> proxies = new ArrayList<>(bean.getProxies()); // make sure we clone first so we can edit the list to our needs.
            // gather language segmented data for fields
            for (Proxy proxy : proxies) {
                ReflectionUtils.doWithFields(proxy.getClass(), field -> getProxyFieldValues(proxy, field, bean, itemsMap), proxyFieldFilter);
            }
        }

        if (LOG.isDebugEnabled()) {
            // LOG.debug("Gathered data for languages {} - {} ", itemsMap.keySet(), itemsMap);
            LOG.debug("Gathered data for languages {} ", itemsMap.keySet());
        }

        // create Debias Requests for each language gathered
        for (Map.Entry<String, List<Item>> entry : itemsMap.entrySet()) {
            DebiasRequest request = new DebiasRequest();
            request.setParams(new Params(1, entry.getKey(), false));
            request.setItems(entry.getValue());
            request.setTotalItems(entry.getValue().size());

            // send request for each language
            List<Annotation> response = debiasService.getAnnotationsForBiasTerms(request);
            if (LOG.isDebugEnabled()) {
                LOG.debug("For language {} debias request, annotations received {}" , entry.getKey(), response.size());
            }
            annotations.addAll(response);
        }
        LOG.debug("Time taken to process {} bean {} ms ", fullBeans.size(), (System.currentTimeMillis() - start));
        return annotations;
    }


    private void getProxyFieldValues(Proxy proxy, Field field, FullBean bean,  Map<String, List<Item>> itemsMap) {
        HashMap<String, List<String>> fieldData = (HashMap<String, List<String>>) getValueOfTheMapFields(proxy, false).apply(field.getName());
        if (fieldData != null && !fieldData.isEmpty()) {
            //for the special fields ,if no values present for supported language , then use corresponding concept->preflabel values as fields values.
            if(FIELD_WITH_POTENTIAL_REFERENCE_VAL.contains(field.getName())) {
                fieldData.putAll(getValueForReferenceFields(fieldData, bean));
            }

            for (Map.Entry<String, List<String>> entry : fieldData.entrySet()) {
                String languageKey = entry.getKey();
                if (DebiasLanguage.isSupported(languageKey)    ) {
                    // get the two-letter ISO code language. there are cases where we will have region codes
                    // we need to fetch the first two ISO letter for the request
                    String language = DebiasLanguage.getLanguage(languageKey).name().toLowerCase();
                    Item item = null;
                    if (itemsMap.containsKey(language)) {
                        // check if the item already is present for that language, if not default to new item
                        item = getExistingItemOrDefaultNew(itemsMap, language, bean.getAbout());
                    } else {
                        item = new Item(bean.getAbout());
                        itemsMap.put(language, new ArrayList<>(Arrays.asList(item))); // create modifiable list
                    }
                    // update the field value in the Item
                    updateItemWithFieldValues(field, entry, item);
                }
            }
        }
    }

    private static void updateItemWithFieldValues(Field field, Entry<String, List<String>> entry, Item item) {
        List<String> existingValue = getValueOfTheListFields(item, true).apply(field.getName());
        //Avoid duplicates
        for(String value : entry.getValue()){
            if(!existingValue.contains(value)) {
                existingValue.add(value);
            }
        }
    }

    /** Method checks the proxy field and its values in the FullBean object.
     * If there is no value present for the supported languages ,
     * it checks the value associated to  default 'def' language.
     * If any concept found for this value  , the map of associated preflable values is returned
     * which later used as proxy field's actual value.
     * @param fieldData
     * @param bean
     * @return Map of language and values from associated concept->preflabel for the given field from proxy.
     */
    public Map<String, List<String>> getValueForReferenceFields( HashMap<String, List<String>> fieldData ,FullBean bean){
        for (Map.Entry<String, List<String>> entry : fieldData.entrySet()) {
            //In case the value for supported  languge is present , we do not need to look for it in concept->preflabel
            if(!DebiasLanguage.isSupported(entry.getKey()) && "def".equals(entry.getKey()) ) {
                //get the values corresponding to def key
                List<String> value = entry.getValue();
                //check the conceptReference in the concept list associated to the fullbean
                for (String conceptReferece : value) {
                    List<Concept> conceptList = (List<Concept>) bean.getConcepts();
                    Optional<Concept> matchedConcept = conceptList.stream()
                        .filter(i -> StringUtils.equals(i.getAbout(), conceptReferece)).findFirst();
                    // if the reference object of concept is found
                    if (matchedConcept.isPresent()) {
                        //return the map of preflabel which acts as the field value
                        return matchedConcept.get().getPrefLabel();
                    }

                }
            }
        }
        return new HashMap<>();
    }

    /**
     * Checks if in the itemsMap for the language requested there is already an item present
     *  true : return the existing item
     *  false : create a new item and add it in the requested langauge item list
     * @param itemsMap
     * @param language
     * @param about
     * @return
     */
    private Item getExistingItemOrDefaultNew(Map<String, List<Item>> itemsMap, String language, String about) {
        List<Item> items = itemsMap.get(language);
        Optional<Item> beanItem = items.stream().filter(i -> StringUtils.equals(i.getId(), about)).findFirst();
        if (beanItem.isPresent()) {
            return beanItem.get();
        } else {
            Item item = new Item(about);
            itemsMap.get(language).add(item);
            return item;
        }
    }

    /**
     * Function to get the lang-value map of the field from the proxy Object
     * @param proxy
     * @param update if true, and the value is null for the field - It sets the empty map in the proxy object
     *               for that field.
     * @return
     */
    private static Function<String, Map<String, List<String>>> getValueOfTheMapFields(Object proxy, boolean update) {

        return e -> {
            Field field = ReflectionUtils.findField(proxy.getClass(), e);
            ReflectionUtils.makeAccessible(field);
            Object value = ReflectionUtils.getField(field, proxy);
            // If we are updating the proxy value, then for the field we must set an empty map
            // if it doesn't exist already. When we are just fetching the values, we need not alter anything in the proxy object
            if (value == null && update) {
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
     * Function to get the list of value from the Item object
     * @param object
     * @param update
     * @return
     */
    private static Function<String, List<String>> getValueOfTheListFields(Object object, boolean update) {
        return e -> {
            Field field = ReflectionUtils.findField(object.getClass(), e);
            ReflectionUtils.makeAccessible(field);
            Object value = ReflectionUtils.getField(field, object);
            // If we are updating the proxy value, then for the field we must set an empty map
            // if it doesn't exist already. When we are just fetching the values, we need not alter anything in the proxy object
            if (value == null && update) {
                ReflectionUtils.setField(field, object, new ArrayList<>());
                value = ReflectionUtils.getField(field, object);
            }
            if (value instanceof List) {
                return (List<String>) value;
            } else if (value != null) { // should not happen as the whitelisted values are all lang-map
                LOG.warn("Unexpected data - field {} did not return a list", e);
            }
            return  new ArrayList<>(); // default return an empty list
        };
    }
}
