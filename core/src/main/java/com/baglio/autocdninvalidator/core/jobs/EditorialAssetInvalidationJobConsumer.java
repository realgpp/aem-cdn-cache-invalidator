package com.baglio.autocdninvalidator.core.jobs;

import com.baglio.autocdninvalidator.core.helpers.LoggingHelper;
import com.baglio.autocdninvalidator.core.service.CdnInvalidationService;
import com.baglio.autocdninvalidator.core.service.ReadService;
import com.baglio.autocdninvalidator.core.service.UtilityService;
import com.day.cq.commons.Externalizer;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

/** Job consumer for invalidating CDN cached assets. */
@Component(service = JobConsumer.class, immediate = true)
@Designate(ocd = EditorialAssetInvalidationJobConsumer.Config.class)
public class EditorialAssetInvalidationJobConsumer extends AbstractInvalidationJob implements JobConsumer {
  private static final LoggingHelper LOGGER = new LoggingHelper(EditorialAssetInvalidationJobConsumer.class);
  /** Job property for paths to invalidate. */
  public static final String JOB_PROPERTY_PATHS = "paths";

  private static final String VALUE_SEPARATOR = "=";
  private static final int VALUE_LEFT_OPERAND_INDEX = 0;
  private static final int VALUE_RIGHT_OPERAND_INDEX = 1;

  private boolean isEnabled;
  private String cdnConfigurationID;
  private Map<String, String> invalidationRules;
  private String invalidationType;
  private String externalLinkScheme;
  private String externalLinkDomain;

  @Reference private Externalizer externalizer;
  @Reference private ReadService readService;
  @Reference private UtilityService utilityService;

  /**
   * Activate method to initialize configuration.
   *
   * @param config The OSGi configuration
   */
  @Activate
  protected void activate(final EditorialAssetInvalidationJobConsumer.Config config) {
    LOGGER.info("Configuration values={}", config);
    this.isEnabled = config.isEnabled();
    this.cdnConfigurationID = config.cdnConfigurationID();
    this.invalidationType = config.invalidation_type();
    this.externalLinkDomain =
        StringUtils.defaultIfBlank(config.externalLinkDomain(), Config.DEFAULT_EXTERNAL_LINK_DOMAIN);
    this.externalLinkScheme =
        StringUtils.defaultIfBlank(config.externalLinkScheme(), Config.EXTERNAL_LINK_SCHEME_OPTION_HTTPS);

    if (null != config.tagCodeMappings()) {
      this.invalidationRules =
          Arrays.stream(config.tagCodeMappings())
              .filter(mapping -> mapping.contains(VALUE_SEPARATOR)) // Ignore values without comma
              .map(
                  configValue -> {
                    final String[] split = configValue.split(VALUE_SEPARATOR, 2);
                    return new KeyValueOption(split[VALUE_LEFT_OPERAND_INDEX], split[VALUE_RIGHT_OPERAND_INDEX]);
                  }) // Split each string by separator
              .filter(
                  option ->
                      !option.getKey().isEmpty() && !option.getValue().isEmpty()) // Remove from map if one is empty
              .collect(
                  Collectors.toMap(
                      KeyValueOption::getKey,
                      KeyValueOption::getValue,
                      (v1, v2) -> v1,
                      TreeMap::new) // Collect the results into a TreeMap
                  );
    }
  }

  /**
   * Execute the job. If the job has been processed successfully, JobResult.OK should be returned. If the job has not
   * been processed completely, but might be rescheduled JobResult.FAILED should be returned. If the job processing
   * failed and should not be rescheduled, JobResult.CANCEL should be returned.
   *
   * @param job The job
   * @return The job result
   */
  @Override
  public JobResult process(final Job job) {

    if (!isEnabled()) {
      LOGGER.debug("Job is disabled");
      return JobResult.CANCEL;
    }

    try {
      final CdnInvalidationService cdnInvalidationService =
          getUtilityService().getService(CdnInvalidationService.class, cdnConfigurationID);
      if (cdnInvalidationService == null) {
        LOGGER.error("Impossible to call CDN Api because service retrieval failed");
        return JobResult.FAILED;
      }

      LOGGER.debug("Consuming job - topic: {}, properties: {}", job.getTopic(), job);

      Set<String> paths = (Set<String>) job.getProperty(JOB_PROPERTY_PATHS);
      if (paths.isEmpty()) {
        LOGGER.debug("No Paths have been provided: processing cancelled");
        return JobResult.CANCEL;
      }
      LOGGER.info("Paths to process: {}", paths);

      JobResult jobResult;
      switch (invalidationType) {
        case Config.INVALIDATION_TYPE_OPTION_CODE:
        case Config.INVALIDATION_TYPE_OPTION_TAG:
          jobResult = handleInvalidateByCodeOrTag(cdnInvalidationService, paths, job);
          break;
        case Config.INVALIDATION_TYPE_OPTION_URLS:
          jobResult = handleInvalidateByURLs(cdnInvalidationService, paths, job);
          break;
        default:
          LOGGER.error("Invalidation type is not allowed: {}", invalidationType);
          return JobResult.FAILED;
      }

      return jobResult;

    } catch (Exception e) {
      LOGGER.error("Unexpected error while invalidating in CDN", e);
      return JobResult.FAILED;
    }
  }

  /**
   * Handles invalidating by code or tag based on configuration.
   *
   * @param cdnInvalidationService the CDN invalidation service to use
   * @param paths the content paths that changed
   * @param job the current job being processed
   * @return the job result based on success or failure
   */
  private JobResult handleInvalidateByCodeOrTag(
      final CdnInvalidationService cdnInvalidationService, final Set<String> paths, final Job job) {
    LOGGER.debug("About to get invalidation values for paths: {}", paths);

    Set<String> values = processValues(paths);

    if (values.isEmpty()) {
      LOGGER.debug("No values to invalidate: processing cancelled");
      return JobResult.CANCEL;
    }

    LOGGER.debug("Values to invalidate: {}", values);

    values = beforeInvalidation(values);
    boolean result =
        Config.INVALIDATION_TYPE_OPTION_CODE.equals(invalidationType)
            ? cdnInvalidationService.purgeByCode(values)
            : cdnInvalidationService.purgeByTag(values);
    result = afterInvalidation(result, job);
    return getFinalResult(result, job);
  }

  /**
   * Processes the content paths to generate invalidation values.
   *
   * <p>Applies before, main, and after processing pipeline.
   *
   * @param paths the content paths that changed
   * @return the set of processed invalidation values
   */
  private Set<String> processValues(final Set<String> paths) {

    Set<String> values = preprocessInvalidationValues(paths);
    LOGGER.debug("Invalidation values after initial processing: {}", values);

    values = getInvalidationValues(values, invalidationRules);
    LOGGER.debug("Invalidation values after main processing: {}", values);

    values = postprocessInvalidationValues(values);
    LOGGER.debug("Final invalidation values to send: {}", values);
    return values;
  }

  /**
   * Handles invalidating by public URLs.
   *
   * @param cdnInvalidationService the CDN invalidation service
   * @param paths the content paths that changed
   * @param job the current job
   * @return the job result based on success or failure
   */
  private JobResult handleInvalidateByURLs(
      final CdnInvalidationService cdnInvalidationService, final Set<String> paths, final Job job) {
    Set<String> urls = processURLs(paths);

    LOGGER.debug("Values after third processing: {}", urls);

    boolean result = cdnInvalidationService.purgeByURLs(urls);
    return getFinalResult(result, job);
  }

  /**
   * Processes the paths to get public URLs.
   *
   * <p>Applies before, main, and after processing pipeline.
   *
   * @param paths the content paths
   * @return the set of public URLs
   */
  private Set<String> processURLs(final Set<String> paths) {

    LOGGER.debug("About to process paths: {}", paths);

    Set<String> urls = preprocessPublicUrls(paths);
    LOGGER.debug("Values after first processing: {}", urls);

    urls = getPublicUrls(urls, externalLinkDomain, externalLinkScheme);
    LOGGER.debug("Values after second processing: {}", urls);

    return postprocessPublicUrls(urls);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isEnabled() {
    return this.isEnabled;
  }

  /** {@inheritDoc} */
  @Override
  public Set<String> preprocessInvalidationValues(final Set<String> paths) {
    return paths;
  }

  /** {@inheritDoc} */
  @Override
  public Set<String> postprocessInvalidationValues(final Set<String> values) {
    return values;
  }

  /**
   * Determines final job result based on processing result.
   *
   * @param result outcome of processing invalidation
   * @param job the job being processed
   * @return the final JobResult status
   */
  private JobResult getFinalResult(final boolean result, final Job job) {
    // check result and return OK or FAILED

    if (result) {
      LOGGER.info("Job processed successfully: {}", job);
      return JobResult.OK;
    }

    LOGGER.error("Job did not finished as expected. See logs for further info: {}", job);
    return JobResult.FAILED;
  }

  /** {@inheritDoc} */
  @Override
  public Set<String> preprocessPublicUrls(final Set<String> paths) {
    LOGGER.debug("preprocessPublicUrls - objects: {}", paths);
    return paths;
  }

  /** {@inheritDoc} */
  @Override
  public Set<String> postprocessPublicUrls(final Set<String> paths) {
    LOGGER.debug("postprocessPublicUrls - objects: {}", paths);
    return paths;
  }

  /** {@inheritDoc} */
  @Override
  public Set<String> beforeInvalidation(final Set<String> values) {
    LOGGER.debug("beforeInvalidation - objects: {}", values);
    return values;
  }

  /** {@inheritDoc} */
  @Override
  public boolean afterInvalidation(final boolean result, final Job job) {
    LOGGER.debug("afterInvalidation - result: {}, job: {}", result, job);
    return result;
  }

  /** {@inheritDoc} */
  @Override
  Externalizer getExternalizer() {
    return externalizer;
  }

  /** {@inheritDoc} */
  @Override
  ReadService getReadService() {
    return readService;
  }

  /** {@inheritDoc} */
  @Override
  UtilityService getUtilityService() {
    return utilityService;
  }

  /** OSGi configuration definition. */
  @ObjectClassDefinition(name = "Auto CDN Invalidator - Job Consumer - Website Generic")
  public @interface Config {

    String INVALIDATION_TYPE_OPTION_URLS = "urls";
    String INVALIDATION_TYPE_OPTION_TAG = "tag";
    String INVALIDATION_TYPE_OPTION_CODE = "code";
    String EXTERNAL_LINK_SCHEME_OPTION_HTTPS = "https";
    String DEFAULT_EXTERNAL_LINK_DOMAIN = "publish";

    @AttributeDefinition(name = "Enable", type = AttributeType.BOOLEAN, description = "Tick to enable it")
    boolean isEnabled() default false;

    @AttributeDefinition(
        name = "Job Topic",
        description = "Defines which topic this consumer is able to process",
        cardinality = 1)
    String[] job_topics();

    @AttributeDefinition(name = "CDN Configuration ID", description = "Defines which CDN Configuration to leverage")
    String cdnConfigurationID();

    @AttributeDefinition(
        name = "Type of Invalidation",
        description = "Defines type of invalidate to be leveraged",
        options = {
          @Option(label = "URLs", value = INVALIDATION_TYPE_OPTION_URLS),
          @Option(label = "Tag", value = INVALIDATION_TYPE_OPTION_TAG),
          @Option(label = "Code", value = INVALIDATION_TYPE_OPTION_CODE),
        })
    String invalidation_type() default INVALIDATION_TYPE_OPTION_TAG;

    @AttributeDefinition(
        name = "Tag/Code Mappings",
        description =
            "Pattern to Tage/Code associations for invalidation rules. Format: "
                + "<pattern-of-trigger-content>=<pattern-of-tag-or-code>")
    String[] tagCodeMappings();

    @AttributeDefinition(
        name = "External Link Domain",
        description =
            "Used to create an absolute URL for a named domain. Value must be present in filed 'externalizer.domains' "
                + "of service 'Day CQ Link Externalizer'")
    String externalLinkDomain() default DEFAULT_EXTERNAL_LINK_DOMAIN;

    @AttributeDefinition(
        name = "External Link Protocol Scheme",
        description = "Protocol scheme that will be part of the URL",
        options = {
          @Option(label = "HTTPS", value = EXTERNAL_LINK_SCHEME_OPTION_HTTPS),
          @Option(label = "HTTP", value = "http")
        })
    String externalLinkScheme() default EXTERNAL_LINK_SCHEME_OPTION_HTTPS;
  }

  static final class KeyValueOption {
    private final String key;
    private final String value;

    KeyValueOption(final String k, final String v) {
      this.key = k;
      this.value = v;
    }

    public String getKey() {
      return StringUtils.isBlank(key) ? key : key.trim();
    }

    public String getValue() {
      return StringUtils.isBlank(value) ? value : value.trim();
    }
  }
}
