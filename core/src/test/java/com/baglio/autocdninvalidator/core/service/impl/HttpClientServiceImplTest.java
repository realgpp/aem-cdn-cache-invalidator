package com.baglio.autocdninvalidator.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpClientServiceImplTest {

  private HttpClientServiceImpl httpClientService;

  @BeforeEach
  void setUp() {
    httpClientService = new HttpClientServiceImpl();
  }

  @AfterEach
  void tearDown() {
    httpClientService.deactivate();
  }

  @Test
  void testMandatoryFieldsAvailable() {
    HttpClientServiceImpl.Config config = mock(HttpClientServiceImpl.Config.class);

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> httpClientService.activate(config));
    assertNotNull(exception);
    assertTrue(StringUtils.isNotBlank(exception.getMessage()));
  }

  @Test
  void testDeactivate() throws NoSuchFieldException, IOException, IllegalAccessException {
    CloseableHttpClient closeableHttpClient = mock(CloseableHttpClient.class);
    doThrow(new IOException("test exception")).when(closeableHttpClient).close();
    Field privateField2 = HttpClientServiceImpl.class.getDeclaredField("httpClient");
    privateField2.setAccessible(true);
    privateField2.set(httpClientService, closeableHttpClient);

    httpClientService.deactivate();
    assertNotNull(privateField2.get(httpClientService));
  }

  @Test
  void testGetConfiguredHttpClient() {
    httpClientService.activate(getDefaultConfig());
    CloseableHttpClient client = httpClientService.getConfiguredHttpClient();
    assertNotNull(client);
  }

  private HttpClientServiceImpl.Config getDefaultConfig() {
    HttpClientServiceImpl.Config config = mock(HttpClientServiceImpl.Config.class);
    when(config.configurationID()).thenReturn("testId");
    when(config.connectionTimeout()).thenReturn(1000);
    when(config.connectionRequestTimeout()).thenReturn(1000);
    when(config.socketTimeout()).thenReturn(5000);
    when(config.maxTotalConnections()).thenReturn(20);
    when(config.maxConnectionsPerRoute()).thenReturn(20);
    return config;
  }
}
