package com.baglio.autocdninvalidator.core.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.baglio.autocdninvalidator.core.service.HttpClientService;
import com.baglio.autocdninvalidator.core.service.UtilityService;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
class AkamaiInvalidationServiceImplTest {
  private static final String HTTP_CLIENT_CONFIGURATION_ID = "httpClientConfigurationID";

  @InjectMocks private AkamaiInvalidationServiceImpl invalidationService;
  @Mock private UtilityService utilityService;
  private AkamaiInvalidationServiceImpl.Config config;

  final Set<String> tags = new HashSet<>(Arrays.asList("tag1", "tag2"));

  @BeforeEach
  void setUp() {
    this.config = mock(AkamaiInvalidationServiceImpl.Config.class);
    when(config.getAkamaiAccessToken()).thenReturn("token");
    when(config.getAkamaiClientToken()).thenReturn("clientToken");
    when(config.getAkamaiClientSecret()).thenReturn("clientSecret");
    when(config.configurationID()).thenReturn("id");
    when(config.hostname()).thenReturn("hostname");
    when(config.network()).thenReturn("network");
    when(config.httpClientConfigurationID()).thenReturn(HTTP_CLIENT_CONFIGURATION_ID);
  }

  @Test
  void testMandatoryFieldsAvailable() throws NoSuchFieldException, IllegalAccessException {
    assertTrue(invalidationService.mandatoryFieldsAvailable(config));

    when(config.configurationID()).thenReturn(null);
    assertFalse(invalidationService.mandatoryFieldsAvailable(config));

    when(config.isEnabled()).thenReturn(true);
    when(config.httpClientConfigurationID()).thenReturn("httpClientConfigurationID");
    invalidationService.activate(config);

    Field privateField = AkamaiInvalidationServiceImpl.class.getDeclaredField("httpClientConfigurationID");
    privateField.setAccessible(true);
    // Get value
    String actualValue = (String) privateField.get(invalidationService);
    assertNull(actualValue);
  }

  @Test
  void testInvalidationFailure() throws IOException {

    // test service not enabled
    when(config.isEnabled()).thenReturn(false);
    invalidationService.activate(config);
    boolean result = invalidationService.invalidateByTag(null);
    assertFalse(result);

    // test null input
    when(config.isEnabled()).thenReturn(true);
    invalidationService.activate(config);
    result = invalidationService.invalidateByTag(null);
    assertFalse(result);

    // test empty input
    when(config.isEnabled()).thenReturn(true);
    invalidationService.activate(config);
    result = invalidationService.invalidateByTag(new HashSet<>());
    assertFalse(result);

    // test client is null
    when(config.isEnabled()).thenReturn(true);
    invalidationService.activate(config);
    result = invalidationService.invalidateByTag(tags);
    assertFalse(result);

    // test generic invalidation execution with negative result
    HttpClientService httpClientService = mock(HttpClientService.class);

    when(utilityService.getService(HttpClientService.class, HTTP_CLIENT_CONFIGURATION_ID))
        .thenReturn(httpClientService);
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    when(httpClientService.getConfiguredHttpClient()).thenReturn(httpClient);
    when(httpClient.execute(any())).thenThrow(new IOException("test exception"));
    invalidationService.activate(config);
    result = invalidationService.invalidateByURLs(tags);
    assertFalse(result);
  }

  @Test
  void testInvalidation() throws IOException {

    when(config.isEnabled()).thenReturn(true);
    invalidationService.activate(config);

    HttpClientService httpClientService = mock(HttpClientService.class);

    when(utilityService.getService(HttpClientService.class, HTTP_CLIENT_CONFIGURATION_ID))
        .thenReturn(httpClientService);
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    when(httpClientService.getConfiguredHttpClient()).thenReturn(httpClient);

    CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
    when(httpClient.execute(any(HttpPost.class))).thenReturn(closeableHttpResponse);
    HttpEntity httpEntity = mock(HttpEntity.class);
    when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);
    StatusLine statusLine = mock(StatusLine.class);
    when(closeableHttpResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(HttpServletResponse.SC_OK);

    // test invalidateByTag execution with positive result
    boolean result = invalidationService.invalidateByTag(tags);
    assertTrue(result);

    // test invalidateByCode full execution with positive result
    result = invalidationService.invalidateByCode(tags);
    assertTrue(result);

    // test invalidateByURLs full execution with positive result
    result = invalidationService.invalidateByURLs(tags);
    assertTrue(result);

    // test invalidateByTag execution with negative result
    when(statusLine.getStatusCode()).thenReturn(HttpServletResponse.SC_CONFLICT);
    result = invalidationService.invalidateByTag(tags);
    assertFalse(result);

    // test invalidateByCode execution with negative result
    result = invalidationService.invalidateByCode(tags);
    assertFalse(result);

    // test invalidateByURLs execution with negative result
    result = invalidationService.invalidateByURLs(tags);
    assertFalse(result);
  }
}
