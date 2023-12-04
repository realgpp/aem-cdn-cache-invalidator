package com.baglio.autocdninvalidator.core.jobs;

import static com.baglio.autocdninvalidator.core.jobs.EditorialAssetInvalidationJobConsumer.JOB_PROPERTY_PATHS;
import static junit.framework.Assert.assertNotNull;
import static junitx.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baglio.autocdninvalidator.core.service.CdnInvalidationService;
import com.baglio.autocdninvalidator.core.service.ReadService;
import com.baglio.autocdninvalidator.core.service.UtilityService;
import com.day.cq.commons.Externalizer;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import junit.framework.Assert;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
class EditorialAssetInvalidationJobConsumerTest {

  private static final String INVALIDATION_RULE_1 = "/content/we-retail/(..)(/.*)*=tag-dev-$1";
  private static final String INVALIDATION_RULE_2 = "/content/we-retail/(..)/.*/experience/.*=tag-dev-$1-experience";
  private static final String VALUE_SEPARATOR = "=";
  @Mock private Job job;
  @Mock private Externalizer externalizer;

  @Mock private CdnInvalidationService cdnInvalidationService;
  @Mock private ReadService readService;
  @Mock private ResourceResolver resourceResolver;

  private UtilityService utilityService;
  private EditorialAssetInvalidationJobConsumer.Config config;
  private final EditorialAssetInvalidationJobConsumerHelper consumer =
      new EditorialAssetInvalidationJobConsumerHelper();

  @BeforeEach
  public void setUp() {
    config = mock(EditorialAssetInvalidationJobConsumer.Config.class);
    utilityService = mock(UtilityService.class);
  }

  @Test
  void testProcessJobFailing() throws NoSuchFieldException, IllegalAccessException {
    // test service is disabled
    JobConsumer.JobResult result = consumer.process(job);
    assertEquals(JobConsumer.JobResult.CANCEL, result);

    // test when CDN service is not retrieved
    when(config.isEnabled()).thenReturn(true);
    when(config.cdnConfigurationID()).thenReturn("");
    when(utilityService.getService(CdnInvalidationService.class, "")).thenReturn(null);
    consumer.activate(config);
    result = consumer.process(job);
    assertEquals(JobConsumer.JobResult.FAILED, result);

    // test job topic does not have a path
    when(job.getProperty(JOB_PROPERTY_PATHS)).thenReturn(new HashSet<>(0));
    when(config.cdnConfigurationID()).thenReturn("cdnConfigurationID");
    when(utilityService.getService(CdnInvalidationService.class, "cdnConfigurationID"))
        .thenReturn(cdnInvalidationService);
    consumer.activate(config);
    result = consumer.process(job);
    assertEquals(JobConsumer.JobResult.CANCEL, result);

    // test wrong invalidation type
    when(config.invalidation_type()).thenReturn("not-valid");
    when(job.getProperty(JOB_PROPERTY_PATHS)).thenReturn(new HashSet<>(Collections.singletonList("test")));
    consumer.activate(config);
    result = consumer.process(job);
    assertEquals(JobConsumer.JobResult.FAILED, result);

    // test catch of exception
    EditorialAssetInvalidationJobConsumerErrorHelper errorHelper =
        new EditorialAssetInvalidationJobConsumerErrorHelper();
    errorHelper.activate(config);
    result = errorHelper.process(job);
    assertEquals(JobConsumer.JobResult.FAILED, result);
  }

  @Test
  void testSimpleUseCases() {
    assertNotNull(consumer.getExternalizer());
    assertNotNull(consumer.getReadService());
    assertNotNull(consumer.getUtilityService());
  }

  @Test
  void testInitInvalidationRules() throws NoSuchFieldException, IllegalAccessException {
    when(config.tagCodeMappings()).thenReturn(getInvalidationRules());
    consumer.activate(config);

    Field privateField = EditorialAssetInvalidationJobConsumer.class.getDeclaredField("invalidationRules");
    privateField.setAccessible(true);

    // Get or set value
    Map<String, String> actualMap = (Map<String, String>) privateField.get(consumer);
    Map<String, String> expectedMap = new HashMap<>();
    expectedMap.put(INVALIDATION_RULE_1.split(VALUE_SEPARATOR, 2)[0], INVALIDATION_RULE_1.split(VALUE_SEPARATOR, 2)[1]);
    expectedMap.put(INVALIDATION_RULE_2.split(VALUE_SEPARATOR, 2)[0], INVALIDATION_RULE_2.split(VALUE_SEPARATOR, 2)[1]);

    Assert.assertEquals(expectedMap, actualMap);
  }

  @Test
  void testInvalidationByTag() {
    testInvalidationBy("tag");
  }

  @Test
  void testInvalidationByCode() {
    testInvalidationBy("code");
  }

  @Test
  void testInvalidationByURLs() {
    testInvalidationBy("urls");
  }

  void testInvalidationBy(final String invalidationType) {
    consumer.activate(getDefaultConfig(invalidationType));

    // test job topic does not have a path
    when(job.getProperty(JOB_PROPERTY_PATHS)).thenReturn(new HashSet<String>());
    JobConsumer.JobResult result = consumer.process(job);
    Assert.assertEquals(JobConsumer.JobResult.CANCEL, result);

    // test job topic does not have a path (with cdn invalidation ok)
    Set<String> paths =
        new HashSet<>(
            Arrays.asList(
                "/content/we-retail/ca/en/home.html",
                "/content/we-retail/ca/en/adventures/riverside-camping-australia.html"));
    when(job.getProperty(JOB_PROPERTY_PATHS)).thenReturn(paths);

    switch (invalidationType) {
      case EditorialAssetInvalidationJobConsumer.Config.INVALIDATION_TYPE_OPTION_TAG:
        when(cdnInvalidationService.purgeByTag(anySet())).thenReturn(true);
        break;
      case EditorialAssetInvalidationJobConsumer.Config.INVALIDATION_TYPE_OPTION_URLS:
        when(cdnInvalidationService.purgeByURLs(anySet())).thenReturn(true);
        break;
      case EditorialAssetInvalidationJobConsumer.Config.INVALIDATION_TYPE_OPTION_CODE:
        when(cdnInvalidationService.purgeByCode(anySet())).thenReturn(true);
        break;
      default:
        break;
    }
    result = consumer.process(job);
    Assert.assertEquals(JobConsumer.JobResult.OK, result);

    // test job topic does not have a path (with cdn invalidation failed)

    switch (invalidationType) {
      case EditorialAssetInvalidationJobConsumer.Config.INVALIDATION_TYPE_OPTION_TAG:
        when(cdnInvalidationService.purgeByTag(anySet())).thenReturn(false);
        break;
      case EditorialAssetInvalidationJobConsumer.Config.INVALIDATION_TYPE_OPTION_URLS:
        when(cdnInvalidationService.purgeByURLs(anySet())).thenReturn(false);
        break;
      case EditorialAssetInvalidationJobConsumer.Config.INVALIDATION_TYPE_OPTION_CODE:
        when(cdnInvalidationService.purgeByCode(anySet())).thenReturn(false);
        break;
      default:
        break;
    }
    result = consumer.process(job);
    Assert.assertEquals(JobConsumer.JobResult.FAILED, result);
  }

  private EditorialAssetInvalidationJobConsumer.Config getDefaultConfig(@NotNull final String invalidationType) {
    when(utilityService.getService(CdnInvalidationService.class, "cdnConfigurationID"))
        .thenReturn(cdnInvalidationService);
    EditorialAssetInvalidationJobConsumer.Config config = mock(EditorialAssetInvalidationJobConsumer.Config.class);
    when(config.isEnabled()).thenReturn(true);
    when(config.cdnConfigurationID()).thenReturn("cdnConfigurationID");
    when(config.invalidation_type()).thenReturn(invalidationType);
    lenient().when(readService.getResourceResolver()).thenReturn(resourceResolver);
    lenient().when(externalizer.publishLink(any(ResourceResolver.class), anyString())).thenReturn("publishLink");

    if (!EditorialAssetInvalidationJobConsumer.Config.INVALIDATION_TYPE_OPTION_URLS.equals(invalidationType)) {
      when(config.tagCodeMappings()).thenReturn(getInvalidationRules());
    }
    return config;
  }

  @NotNull
  private static String[] getInvalidationRules() {
    return new String[] {INVALIDATION_RULE_1, INVALIDATION_RULE_2, "missing-separator", "empty-value=", "=empty-key"};
  }

  class EditorialAssetInvalidationJobConsumerHelper extends EditorialAssetInvalidationJobConsumer {
    @Override
    Externalizer getExternalizer() {
      return externalizer;
    }

    @Override
    ReadService getReadService() {
      return readService;
    }

    @Override
    UtilityService getUtilityService() {
      return utilityService;
    }
  }

  class EditorialAssetInvalidationJobConsumerErrorHelper extends EditorialAssetInvalidationJobConsumerHelper {
    @Override
    UtilityService getUtilityService() {
      return null;
    }
  }
}
