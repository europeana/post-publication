package eu.europeana.postpublication.translation.service.pangeanic;

import eu.europeana.postpublication.translation.exception.TranslationException;
import eu.europeana.postpublication.translation.service.LanguageDetectionService;
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
import java.util.ArrayList;
import java.util.List;

@Service
@PropertySource("classpath:post-publication.properties")
@PropertySource(value = "classpath:post-publication.user.properties", ignoreResourceNotFound = true)
public class PangeanicV2LangDetectService implements LanguageDetectionService {

    protected static final Logger LOG = LogManager.getLogger(PangeanicV2LangDetectService.class);

    @Value("${translation.pangeanic.endpoint.detect:}")
    protected String detectEndpoint;

    protected CloseableHttpClient translateClient;

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
        LOG.info("Pangeanic Language Detection service is initialized with detect language Endpoint - {}", detectEndpoint);
    }

    // TODO logic yet to be identified, for now return true for every pair
    @Override
    public boolean isSupported(String srcLang) {
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

    // TODO score logic still pending
    private List<String> sendDetectRequestAndParse(HttpPost post) throws IOException, JSONException, TranslationException {
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
