package com.baglio.autocdninvalidator.core.service.impl;

import static com.baglio.autocdninvalidator.core.utils.Constants.CONFIGURATION_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.baglio.autocdninvalidator.core.service.CdnInvalidationService;
import com.baglio.autocdninvalidator.core.service.UtilityService;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

class UtilityServiceImplTest {

  @Mock private BundleContext bundleContext;

  @Mock private ServiceReference<CdnInvalidationService> cdnInvalidationServiceReference;

  @Mock private CdnInvalidationService cdnInvalidationService;

  @Mock Bundle bundle;
  MockedStatic<FrameworkUtil> frameworkUtilMockedStatic = mockStatic(FrameworkUtil.class);

  private UtilityService utilityService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(bundleContext.getService(cdnInvalidationServiceReference)).thenReturn(cdnInvalidationService);
    utilityService = new UtilityServiceImpl();
  }

  @AfterEach
  void tearDown() {
    frameworkUtilMockedStatic.close();
  }

  @Test
  void getService_shouldReturnService_whenServiceIdMatches() throws InvalidSyntaxException {
    String serviceId = "service-id";
    Class<CdnInvalidationService> tClass = CdnInvalidationService.class;
    when(cdnInvalidationServiceReference.getProperty(CONFIGURATION_ID)).thenReturn(serviceId);
    Collection<ServiceReference<CdnInvalidationService>> refs =
        Collections.singletonList(cdnInvalidationServiceReference);
    when(bundleContext.getServiceReferences(tClass, null)).thenReturn(refs);
    frameworkUtilMockedStatic.when(() -> FrameworkUtil.getBundle(tClass)).thenReturn(bundle);
    when(bundle.getBundleContext()).thenReturn(bundleContext);

    CdnInvalidationService service = utilityService.getService(tClass, serviceId);

    assertNotNull(service);
    assertEquals(cdnInvalidationService, service);
  }

  @Test
  void getService_shouldReturnNull_whenServiceIdDoesNotMatch() throws InvalidSyntaxException {
    Class<CdnInvalidationService> tClass = CdnInvalidationService.class;
    String serviceId = "service-id";
    when(cdnInvalidationServiceReference.getProperty(CONFIGURATION_ID)).thenReturn("other-service");
    Collection<ServiceReference<CdnInvalidationService>> refs = Arrays.asList(cdnInvalidationServiceReference);
    when(bundleContext.getServiceReferences(tClass, null)).thenReturn(refs);
    frameworkUtilMockedStatic.when(() -> FrameworkUtil.getBundle(tClass)).thenReturn(bundle);
    when(bundle.getBundleContext()).thenReturn(bundleContext);

    CdnInvalidationService service = utilityService.getService(tClass, serviceId);

    assertNull(service);
  }

  @Test
  void getService_shouldReturnNull_whenNoServiceReferenceFound() throws InvalidSyntaxException {
    Class<CdnInvalidationService> tClass = CdnInvalidationService.class;
    String serviceId = "service-id";
    when(bundleContext.getServiceReferences(tClass, null)).thenReturn(null);
    Bundle bundle = mock(Bundle.class);
    frameworkUtilMockedStatic.when(() -> FrameworkUtil.getBundle(tClass)).thenReturn(bundle);
    when(bundle.getBundleContext()).thenReturn(bundleContext);

    CdnInvalidationService service = utilityService.getService(tClass, serviceId);

    assertNull(service);
  }

  @Test
  void getService_shouldReturnNull_whenExceptionOccurs() throws InvalidSyntaxException {
    Class<CdnInvalidationService> tClass = CdnInvalidationService.class;
    String serviceId = "service-id";
    when(bundleContext.getServiceReferences(tClass, null)).thenThrow(new RuntimeException("test"));
    Bundle bundle = mock(Bundle.class);
    frameworkUtilMockedStatic.when(() -> FrameworkUtil.getBundle(tClass)).thenReturn(bundle);
    when(bundle.getBundleContext()).thenReturn(bundleContext);

    CdnInvalidationService service = utilityService.getService(tClass, serviceId);

    assertNull(service);
  }
}
