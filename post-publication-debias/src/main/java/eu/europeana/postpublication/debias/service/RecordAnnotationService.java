package eu.europeana.postpublication.debias.service;

import eu.europeana.annotation.definitions.model.Annotation;
import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.corelib.definitions.edm.entity.Proxy;
import eu.europeana.postpublication.debias.exception.DebiasException;
import eu.europeana.postpublication.debias.model.*;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

// TODO check the region codes
@Service
public class RecordAnnotationService {

    private static final Logger LOG = LogManager.getLogger(RecordAnnotationService.class);

    private static final Set<String> INCLUDE_PROXY_MAP_FIELDS = Set.of("dcTitle", "dctermsAlternative", "dcDescription");
    protected static final ReflectionUtils.FieldFilter proxyFieldFilter = field -> field.getType().isAssignableFrom(Map.class) &&
            INCLUDE_PROXY_MAP_FIELDS.contains(field.getName());

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
            LOG.debug("Gathered data for languages {} - {} ", itemsMap.keySet(), itemsMap);
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
            for (Map.Entry<String, List<String>> entry : fieldData.entrySet()) {
                if (DebiasLanguage.isSupported(entry.getKey())) {
                    // get the two-letter ISO code language. there are cases where we will have region codes
                    // we need to fetch the first two ISO letter for the request
                    String language = DebiasLanguage.getLanguage(entry.getKey()).name().toLowerCase();
                    Item item = null;
                    if (itemsMap.containsKey(language)) {
                        // check if the item already is present for that language, if not default to new item
                        item = getExistingItemOrDefaultNew(itemsMap, language, bean.getAbout());
                    } else {
                        item = new Item(bean.getAbout());
                        itemsMap.put(language, new ArrayList<>(Arrays.asList(item))); // create modifiable list
                    }
                    // update the field value in the Item
                    List<String> existingValue = getValueOfTheListFields(item, true).apply(field.getName());
                    existingValue.addAll(entry.getValue());

                }
            }
        }
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
