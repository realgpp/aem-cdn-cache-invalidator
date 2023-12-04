package com.baglio.autocdninvalidator.core.service;

import org.apache.http.impl.client.CloseableHttpClient;

/** Service interface for obtaining a configured HTTP client. */
public interface HttpClientService {

  /**
   * Gets a closeable HTTP client that is configured with the desired properties.
   *
   * <p>The client is configured with:
   *
   * <ul>
   *   <li>Custom accepting trust strategy to allow all certificates
   *   <li>HTTP and HTTPS socket factories
   *   <li>Connection pool with max total and default max per route connections
   *   <li>Request configuration with connect, socket and request timeouts
   * </ul>
   *
   * <p>The client is lazily initialized on first call to this method.
   *
   * @return a closeable HttpClient instance
   */
  CloseableHttpClient getConfiguredHttpClient();
}
