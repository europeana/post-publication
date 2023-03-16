package eu.europeana.postpublication.translation.service;

import eu.europeana.postpublication.translation.exception.TranslationException;
import eu.europeana.postpublication.translation.model.Language;
import eu.europeana.postpublication.translation.utils.PangeanicTranslationUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

/**
 * Service to send data to translate to Pangeanic Translate API V2
 * @author Srishti Singh
 */
// TODO get api key, for now passed empty
@Service
@PropertySource("classpath:post-publication.properties")
@PropertySource(value = "classpath:post-publication.user.properties", ignoreResourceNotFound = true)
public class PangeanicV2TranslationService implements TranslationService  {

    private static final Logger LOG = LogManager.getLogger(PangeanicV2TranslationService.class);

    @Value("${translation.pangeanic.endpoint.translate:}")
    private String translateEndpoint;

    @Value("${translation.pangeanic.endpoint.detect:}")
    private String detectEndpoint;

    private CloseableHttpClient translateClient;

    // ONLY for testing purpose
    public String getTranslateEndpoint() {
        return translateEndpoint;
    }

    public void setTranslateEndpoint(String translateEndpoint) {
        this.translateEndpoint = translateEndpoint;
    }

    public String getDetectEndpoint() {
        return detectEndpoint;
    }

    public void setDetectEndpoint(String detectEndpoint) {
        this.detectEndpoint = detectEndpoint;
    }

    public CloseableHttpClient getTranslateClient() {
        return translateClient;
    }

    public void setTranslateClient(CloseableHttpClient translateClient) {
        this.translateClient = translateClient;
    }

    /**
     * Creates a new client that can send translation requests to Google Cloud Translate. Note that the client needs
     * to be closed when it's not used anymore
     * @throws IOException when there is a problem retrieving the first token
     * @throws JSONException when there is a problem decoding the received token
     */
    @PostConstruct
    private void init() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(PangeanicTranslationUtils.MAX_CONNECTIONS);
        cm.setDefaultMaxPerRoute(PangeanicTranslationUtils.MAX_CONNECTIONS_PER_ROUTE);
        translateClient = HttpClients.custom().setConnectionManager(cm).build();
        LOG.info("Pangeanic translation service is initialized. Translate Endpoint is {}. Detect language Endpoint is {}", translateEndpoint, detectEndpoint);
    }

    // TODO logic yet to be identified, for now return true for every pair
    @Override
    public boolean isSupported(String srcLang, String trgLang) {
        return true;
    }

    @Override
    public List<String> detectLang(List<String> texts, String langHint) throws TranslationException {
        try {
            HttpPost post = PangeanicTranslationUtils.createDetectlanguageRequest(detectEndpoint, texts, langHint, "");
            return sendDetectRequestAndParse(post);
        } catch (JSONException | IOException e) {
            throw new TranslationException(e.getMessage());
        }
    }

    @Override
    public List<String> translate(List<String> texts, String targetLanguage, String sourceLanguage) throws TranslationException {
        try {
            HttpPost post = PangeanicTranslationUtils.createTranslateRequest(translateEndpoint, texts, targetLanguage, sourceLanguage, "" );
            return PangeanicTranslationUtils.getResults(texts, sendTranslateRequestAndParse(post), false);
        } catch (JSONException|IOException e) {
            throw new TranslationException(e.getMessage());
        }
    }

    @Override
    public List<String> translateAndDetect(List<String> texts, String targetLanguage, String langHint) throws TranslationException {
        return translateWithDetctedLanguages(texts, targetLanguage, langHint);
    }

    /**
     * Translates the texts with no source language.
     * First a lang detect request is sent to identify the source language
     * Later translations are performed
     *
     * @param texts
     * @param targetLanguage
     * @return
     * @throws TranslationException
     */
    private List<String> translateWithDetctedLanguages(List<String> texts, String targetLanguage, String langHint) throws TranslationException {
        try {
            List<String> detectedLanguages = detectLang(texts, langHint);
            // create lang-value map for translation
            Map<String, List<String>> detectedLangValueMap = PangeanicTranslationUtils.getDetectedLangValueMap(texts, detectedLanguages);
            LOG.debug("Pangeanic detect lang request with hint {} is executed. Detected languages are {} ", langHint, detectedLangValueMap.keySet());

            Map<String, String> translations = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> entry : detectedLangValueMap.entrySet()) {
                if (PangeanicTranslationUtils.noTranslationRequired(entry.getKey())) {
                    if (entry.getKey().equals(Language.DEF)) {
                        LOG.debug("NOT translating data for empty lang detected values {} ", entry.getValue());
                    } else {
                        LOG.debug("NOT translating data for lang '{}' and values {} ", entry.getKey(), entry.getValue());
                    }
                } else {
                    HttpPost translateRequest = PangeanicTranslationUtils.createTranslateRequest(translateEndpoint, entry.getValue(), targetLanguage, entry.getKey(), "");
                    translations.putAll(sendTranslateRequestAndParse(translateRequest));
                }
            }
            return PangeanicTranslationUtils.getResults(texts, translations, PangeanicTranslationUtils.nonTranslatedDataExists(detectedLanguages));
        } catch (JSONException | IOException e) {
            throw  new TranslationException(e.getMessage());
        }
    }


    // TODO score logic still pending
    private Map<String, String> sendTranslateRequestAndParse(HttpPost post) throws IOException, JSONException, TranslationException {
        try (CloseableHttpResponse response = translateClient.execute(post)) {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("Error from Pangeanic Translation API: " +
                        response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase());
            } else {
                String json = EntityUtils.toString(response.getEntity());
                JSONObject obj = new JSONObject(json);
                Map<String, String> results = new LinkedHashMap<>();
                // there are cases where we get an empty response
                if (!obj.has(PangeanicTranslationUtils.TRANSLATIONS)) {
                    throw new TranslationException("Pangeanic Translation API returned empty response");
                }
                JSONArray translations = obj.getJSONArray(PangeanicTranslationUtils.TRANSLATIONS);
                for (int i = 0; i < translations.length(); i++) {
                    // TODO Pangeanic changed the object model. Still need to verify this change
                    JSONObject object = (JSONObject) ((JSONArray)translations.get(i)).get(0);
                    results.put(object.getString(PangeanicTranslationUtils.TRANSLATE_SOURCE), object.getString(PangeanicTranslationUtils.TRANSLATE_TARGET));

                }
                // response should not be empty
                if (results.isEmpty()) {
                    throw new TranslationException("Translation failed for source language - " +obj.get(PangeanicTranslationUtils.SOURCE_LANG));
                }
                return  results;
            }
        }
    }

    // TODO score logic still pending
    public List<String> sendDetectRequestAndParse(HttpPost post) throws IOException, JSONException, TranslationException {
        try (CloseableHttpResponse response = translateClient.execute(post)) {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("Error from Pangeanic Translation API: " +
                        response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase());
            } else {
                String json = EntityUtils.toString(response.getEntity());
                // sometimes language detect sends 200 ok status with empty response data
                if (json.isEmpty()) {
                    throw new TranslationException("Language detect returned an empty response");
                }
                JSONObject obj = new JSONObject(json);
                List<String> result = new ArrayList<>();
                JSONArray detectedLangs = obj.getJSONArray(PangeanicTranslationUtils.DETECTED_LANGUAGE);
                for (int i = 0; i < detectedLangs.length(); i++) {
                    JSONObject object = (JSONObject) detectedLangs.get(i);
                    if (object.has(PangeanicTranslationUtils.SOURCE_DETECTED)) {
                        result.add(object.getString(PangeanicTranslationUtils.SOURCE_DETECTED));
                    } else {
                        // when no detected lang is returned. Ideally, this should not happen
                        // But there are time Pangeanic returns no src_detected value
                        // These values as well will remain non-translated
                        result.add(null);
                    }
                }
                return result;
            }
        }
    }

    @Override
    public void close() {
        if (translateClient != null) {
            try {
                this.translateClient.close();
            } catch (IOException e) {
                LOG.error("Error closing connection to Pangeanic Translation API", e);
            }
        }
    }
}
