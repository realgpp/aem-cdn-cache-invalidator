package com.baglio.autocdninvalidator.core.service.impl;

import static com.baglio.autocdninvalidator.core.utils.Constants.CONFIGURATION_ID;

import com.akamai.edgegrid.signer.ClientCredential;
import com.akamai.edgegrid.signer.EdgeGridV1Signer;
import com.akamai.edgegrid.signer.Request;
import com.akamai.edgegrid.signer.exceptions.RequestSigningException;
import com.baglio.autocdninvalidator.core.helpers.LoggingHelper;
import com.baglio.autocdninvalidator.core.service.CdnInvalidationService;
import com.baglio.autocdninvalidator.core.service.HttpClientService;
import com.baglio.autocdninvalidator.core.service.UtilityService;
import com.google.gson.Gson;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@Designate(ocd = AkamaiInvalidationServiceImpl.Config.class)
@Component(service = CdnInvalidationService.class, immediate = true)
public class AkamaiInvalidationServiceImpl implements CdnInvalidationService {
  private static final LoggingHelper LOGGER = new LoggingHelper(AkamaiInvalidationServiceImpl.class);
  private static final String SERVICE_NAME_PLACEHOLDER = "<serviceName>";

  private final Gson gson = new Gson();
  private ClientCredential credential;
  private String unprocessedAkamaiUrl;
  private CloseableHttpClient client;
  private boolean isEnabled;
  private String httpClientConfigurationID;

  @Reference private UtilityService utilityService;

  enum ServiceName {
    CPCODE("cpcode"),
    URL("url"),
    TAG("tag");

    private final String value;

    ServiceName(final String name) {
      this.value = name;
    }

    /**
     * Returns the string value of this enum constant.
     *
     * @return the string value of this enum constant
     */
    public String getValue() {
      return value;
    }
  }

  /**
   * Activate method to initialize configuration.
   *
   * @param config The OSGi configuration
   */
  @Activate
  @Modified
  public void activate(final Config config) {
    LOGGER.info("Activated/Modified");

    this.isEnabled = config.isEnabled();

    if (this.isEnabled) {

      if (!mandatoryFieldsAvailable(config)) {
        LOGGER.error("Not all mandatory fields are available: {}", config);
        return;
      }
      this.httpClientConfigurationID = config.httpClientConfigurationID();
      this.credential =
          getClientCredential(
              config.getAkamaiAccessToken(),
              config.getAkamaiClientToken(),
              config.getAkamaiClientSecret(),
              config.hostname());

      this.unprocessedAkamaiUrl =
          "https://"
              + config.hostname()
              + "/ccu/v3/"
              + config.purgeType()
              + "/"
              + SERVICE_NAME_PLACEHOLDER
              + "/"
              + config.network();
    }
  }

  /**
   * Checks if the mandatory fields of a given config object are available and not blank.
   *
   * @param config the config object to check
   * @return true if all the mandatory fields are available and not blank, false otherwise
   */
  boolean mandatoryFieldsAvailable(final Config config) {
    List<String> result = new ArrayList<>();
    result.add(config.configurationID());
    result.add(config.httpClientConfigurationID());
    result.add(config.hostname());
    result.add(config.getAkamaiAccessToken());
    result.add(config.getAkamaiClientToken());
    result.add(config.getAkamaiClientSecret());
    result.add(config.network());

    return result.parallelStream().noneMatch(StringUtils::isBlank);
  }

  private boolean internalRequest(final ServiceName serviceName, final Set<String> items) {
    LOGGER.trace("Starting akamai invalidation for '{}'", items);
    if (!this.isEnabled) {
      LOGGER.info("Service is disabled by configuration");
      return false;
    }
    if (items == null || items.isEmpty()) {
      LOGGER.warn("Provided input set of values is empty");
      return false;
    }

    if (getClient() == null || this.credential == null) {
      LOGGER.error("Impossible to invalidate '{}' because mandatory data is not available", items);
      return false;
    }

    String finalAkamaiUrl = this.unprocessedAkamaiUrl.replace(SERVICE_NAME_PLACEHOLDER, serviceName.getValue());
    HttpPost request = new HttpPost(finalAkamaiUrl);
    try {
      request.setEntity(new StringEntity("{\"objects\":" + getJsonString(items) + "}"));
    } catch (UnsupportedEncodingException e) {
      LOGGER.error("Impossible to convert items to json array string", e);
      return false;
    }

    boolean result = false;
    try {
      String authHeader = getAuthenticationHeader(finalAkamaiUrl, items);

      request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
      request.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

      HttpResponse response = client.execute(request);
      String bodyResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
      LOGGER.trace("Akamai response for invalidating '{}': {}", items, bodyResponse);

      int statusCode = response.getStatusLine().getStatusCode();
      result = statusCode >= HttpServletResponse.SC_OK && statusCode <= HttpServletResponse.SC_MULTIPLE_CHOICES;
    } catch (Exception e) {
      LOGGER.error("Invalidation by tag - Unexpected error", e);
    }
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * @see <a href="https://techdocs.akamai.com/purge-cache/reference/invalidate-tag">Official Akamai documentation:
   *     invalidation by tag</a>
   * @see <a href="https://techdocs.akamai.com/purge-cache/reference/delete-tag">Official Akamai documentation: delete
   *     by tag</a>
   */
  @Override
  public boolean purgeByTag(final Set<String> tags) {
    return internalRequest(ServiceName.TAG, tags);
  }

  /**
   * {@inheritDoc}
   *
   * @see <a href="https://techdocs.akamai.com/purge-cache/reference/invalidate-cpcode">Official Akamai documentation:
   *     invalidation by cpcode</a>
   * @see <a href="https://techdocs.akamai.com/purge-cache/reference/delete-cpcode">Official Akamai documentation:
   *     delete by cpcode</a>
   */
  @Override
  public boolean purgeByCode(final Set<String> codes) {
    return internalRequest(ServiceName.CPCODE, codes);
  }

  /**
   * {@inheritDoc}
   *
   * @see <a href="https://techdocs.akamai.com/purge-cache/reference/invalidate-url">Official Akamai documentation:
   *     invalidation by URL</a>
   * @see <a href="https://techdocs.akamai.com/purge-cache/reference/delete-url">Official Akamai documentation: delete
   *     by URL</a>
   */
  @Override
  public boolean purgeByURLs(final Set<String> urls) {
    return internalRequest(ServiceName.URL, urls);
  }

  /**
   * Generates an authentication header for a POST request to Akamai CDN using the EdgeGridV1Signer library.
   *
   * @param akamaiUrl the URL of the Akamai CDN endpoint
   * @param objects a set of objects to be invalidated
   * @return a string containing the authentication header value
   * @throws RequestSigningException if the request signing fails
   */
  private String getAuthenticationHeader(final String akamaiUrl, final Set<String> objects)
      throws RequestSigningException {
    final String valueAsString = getJsonString(objects);
    Request request2 =
        Request.builder()
            .method("POST")
            .header(HttpHeaders.CONNECTION, ContentType.APPLICATION_JSON.getMimeType())
            .uri(akamaiUrl)
            .body(("{\"objects\":" + valueAsString + "}").getBytes(Charset.defaultCharset()))
            .build();
    return new EdgeGridV1Signer().getSignature(request2, this.credential);
  }

  /**
   * Converts a set of objects into a JSON string using the Gson library.
   *
   * @param objects a set of objects to be serialized
   * @return a JSON string representation of the objects
   */
  private String getJsonString(final Set<String> objects) {
    return gson.toJson(objects);
  }

  /**
   * creates ClientCredential object with Akamai configs which is required for POST and returns it.
   *
   * @param clientAccessToken akamai access token
   * @param clientSecret akamai secret
   * @param baseUrl base url
   * @param clientToken akamai token
   * @return ClientCredential
   */
  private ClientCredential getClientCredential(
      final String clientAccessToken, final String clientToken, final String clientSecret, final String baseUrl) {
    return ClientCredential.builder()
        .accessToken(clientAccessToken)
        .clientToken(clientToken)
        .clientSecret(clientSecret)
        .host(baseUrl)
        .build();
  }

  /**
   * Returns a configured HTTP client to communicate with the CDN provider. If the client is null, it obtains the HTTP
   * client service by matching the configuration ID and then gets the configured HTTP client from the service.
   *
   * @return a CloseableHttpClient object or null if the service is not available
   */
  CloseableHttpClient getClient() {
    if (client == null) {
      final HttpClientService httpClientService =
          utilityService.getService(HttpClientService.class, this.httpClientConfigurationID);
      if (httpClientService != null) {
        client = httpClientService.getConfiguredHttpClient();
      } else {
        LOGGER.error(
            "Impossible to find HttpClientService with {}={}", CONFIGURATION_ID, this.httpClientConfigurationID);
      }
    }
    return client;
  }

  @ObjectClassDefinition(name = "Auto CDN Invalidator - Akamai Purge API Settings")
  public @interface Config {

    String PURGE_TYPE_OPTION_INVALIDATE = "invalidate";
    String PURGE_TYPE_OPTION_DELETE = "delete";

    @AttributeDefinition(name = "Enable", type = AttributeType.BOOLEAN, description = "Tick to enable it")
    boolean isEnabled() default false;

    @AttributeDefinition(name = "Configuration ID", description = "A unique identifier for the configuration")
    String configurationID();

    @AttributeDefinition(name = "Akamai Client secret")
    String getAkamaiClientSecret();

    @AttributeDefinition(name = "Akamai Access token")
    String getAkamaiAccessToken();

    @AttributeDefinition(name = "Akamai Client token")
    String getAkamaiClientToken();

    @AttributeDefinition(name = "Akamai Hostname")
    String hostname();

    @AttributeDefinition(
        name = "Akamai network",
        description = "The network on which you want to invalidate or delete content",
        options = {@Option(label = "staging", value = "staging"), @Option(label = "production", value = "production")})
    String network() default "staging";

    @AttributeDefinition(
        name = "Purge type",
        description = "Akamai Fast Purge supports two different methods: Invalidate or Delete",
        options = {
          @Option(label = "Invalidate", value = PURGE_TYPE_OPTION_INVALIDATE),
          @Option(label = "Delete", value = PURGE_TYPE_OPTION_DELETE),
        })
    String purgeType() default PURGE_TYPE_OPTION_INVALIDATE;

    @AttributeDefinition(
        name = "HTTP Client Configuration ID",
        description = "Configuration ID to use for HTTP connections")
    String httpClientConfigurationID();
  }
}
