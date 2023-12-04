package com.baglio.autocdninvalidator.core.service.impl;

import com.baglio.autocdninvalidator.core.helpers.LoggingHelper;
import com.baglio.autocdninvalidator.core.service.HttpClientService;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Designate(ocd = HttpClientServiceImpl.Config.class, factory = true)
@Component(service = HttpClientService.class, immediate = true)
public class HttpClientServiceImpl implements HttpClientService {

  private static final LoggingHelper LOGGER = new LoggingHelper(HttpClientServiceImpl.class);

  private CloseableHttpClient httpClient;
  private PoolingHttpClientConnectionManager poolingConnectionManager;

  private int connectionTimeout;
  private int connectionRequestTimeout;
  private int socketTimeout;
  private int maxTotalConnections;
  private int maxConnectionsPerRoute;

  /**
   * Activate method to initialize configuration.
   *
   * @param config The OSGi configuration
   */
  @Activate
  @Modified
  protected void activate(final Config config) {
    this.maxConnectionsPerRoute = config.maxConnectionsPerRoute();
    this.maxTotalConnections = config.maxTotalConnections();

    this.connectionRequestTimeout = config.connectionRequestTimeout();
    this.connectionTimeout = config.connectionTimeout();
    this.socketTimeout = config.socketTimeout();

    if (!mandatoryFieldsAvailable(config)) {
      throw new IllegalArgumentException("Any of mandatory fields not available");
    }

    httpClient = null; // reset of client, so it will be recalculated on next first get
  }

  /** Deactivate method before service is stopped. */
  @Deactivate
  protected void deactivate() {
    if (this.poolingConnectionManager != null) {
      this.poolingConnectionManager.shutdown();
    }

    if (this.httpClient != null) {
      try {
        this.httpClient.close();
        this.httpClient = null;
      } catch (IOException e) {
        LOGGER.error("Error closing HTTP client", e);
      }
    }
  }

  /**
   * Checks if the mandatory fields of a given config object are available and not blank.
   *
   * @param config the config object to check
   * @return true if all the mandatory fields are available and not blank, false otherwise
   */
  boolean mandatoryFieldsAvailable(final Config config) {
    List<Integer> result = new ArrayList<>();
    result.add(config.maxConnectionsPerRoute());
    result.add(config.maxTotalConnections());
    result.add(config.connectionRequestTimeout());
    result.add(config.connectionTimeout());
    result.add(config.socketTimeout());

    return result.parallelStream().noneMatch(item -> item <= 0);
  }

  /** {@inheritDoc} */
  @Override
  public CloseableHttpClient getConfiguredHttpClient() {
    if (this.httpClient == null) {
      TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
      RegistryBuilder<ConnectionSocketFactory> connectionSocketFactoryRegistryBuilder = RegistryBuilder.create();
      try {
        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        SSLConnectionSocketFactory sslConnectionSocketFactory =
            new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        connectionSocketFactoryRegistryBuilder.register("https", sslConnectionSocketFactory);
      } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
        LOGGER.error("Error setting SSL context", e);
      }

      Registry<ConnectionSocketFactory> socketFactoryRegistry =
          connectionSocketFactoryRegistryBuilder.register("http", new PlainConnectionSocketFactory()).build();

      this.poolingConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
      poolingConnectionManager.setMaxTotal(this.maxTotalConnections);
      poolingConnectionManager.setDefaultMaxPerRoute(this.maxConnectionsPerRoute);

      RequestConfig requestConfig =
          RequestConfig.custom()
              .setConnectionRequestTimeout(this.connectionRequestTimeout)
              .setConnectTimeout(this.connectionTimeout)
              .setSocketTimeout(this.socketTimeout)
              .build();
      HttpClientBuilder httpClientBuilder =
          HttpClients.custom().setConnectionManager(poolingConnectionManager).setDefaultRequestConfig(requestConfig);

      this.httpClient = httpClientBuilder.build();
    }

    return this.httpClient;
  }

  @ObjectClassDefinition(name = "Auto CDN Invalidator - HTTP Client")
  public @interface Config {

    @AttributeDefinition(name = "Configuration ID", description = "A unique identifier for the configuration")
    String configurationID();

    @AttributeDefinition(name = "HTTP Connection Timeout", description = "Value in milliseconds")
    int connectionTimeout() default 5000;

    @AttributeDefinition(name = "HTTP Connection Request Timeout", description = "Value in milliseconds")
    int connectionRequestTimeout() default 5000;

    @AttributeDefinition(name = "HTTP Socket Timeout", description = "Value in milliseconds")
    int socketTimeout() default 5000;

    @AttributeDefinition(name = "HTTP Max Total Connections")
    int maxTotalConnections() default 20;

    @AttributeDefinition(name = "HTTP Max Connections per Route")
    int maxConnectionsPerRoute() default 20;
  }
}
