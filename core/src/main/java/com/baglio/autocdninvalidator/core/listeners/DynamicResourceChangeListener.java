package com.baglio.autocdninvalidator.core.listeners;

import com.baglio.autocdninvalidator.core.helpers.LoggingHelper;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.event.jobs.JobManager;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * The Sling Resource Change Listener is the preferred method for listening for Resource Change events in AEM. This is
 * preferred over the Sling Resource Event Listener, or the JCR Event Handler approaches.
 *
 * <p>Source: <a href="https://cqdump.joerghoh.de/2022/02/03/the-deprecation-of-sling-resource-events">The deprecation
 * of Sling Resource Events</a>
 *
 * <p>This specific implementation is intended to be used on AEM Publish instances as it relies * on Publish-specific
 * behavior.
 */
@Component(service = ResourceChangeListener.class, immediate = true)
@Designate(ocd = DynamicResourceChangeListener.Config.class, factory = true)
public class DynamicResourceChangeListener extends AbstractListener implements ResourceChangeListener {
  private static final LoggingHelper LOGGER = new LoggingHelper(DynamicResourceChangeListener.class);

  @Reference private JobManager jobManager;

  private String[] pathsToListenFor;
  private boolean isEnabled;
  private String jobTopic;
  private String filterRegex;

  /**
   * Activate method to initialize configuration.
   *
   * @param config The OSGi configuration
   */
  @Activate
  @Modified
  protected void activate(final Config config) {
    pathsToListenFor = config.resource_paths();
    isEnabled = config.isEnabled();
    jobTopic = config.job_topic();
    filterRegex = config.filter_regex();
    LOGGER.info(
        "Activated - enabled: {}, root paths: {}, events: {}",
        isEnabled,
        pathsToListenFor,
        config.resource_change_types());
  }

  /** {@inheritDoc} */
  @Override
  public void onChange(final @NotNull List<ResourceChange> changes) {
    if (isEnabled) {
      LOGGER.info("Received {} changes under root paths: {}", changes.size(), pathsToListenFor);
      final Set<String> filteredPaths = getChangesPaths(changes);
      boolean result = processEvent(filteredPaths, filterRegex, jobTopic);
      LOGGER.info("Result of processing: {}", result);
    }
  }

  /**
   * Extracts the paths from a list of {@link ResourceChange} instances and returns a set of filtered paths.
   *
   * <p>This method processes each {@code ResourceChange} in parallel, extracting the path from each change and logging
   * relevant information using the configured logger. The resulting paths are collected into a {@code Set} to ensure
   * uniqueness.
   *
   * @param changes A list of {@code ResourceChange} instances to extract paths from.
   * @return A {@code Set} of filtered paths extracted from the input {@code ResourceChange} instances.
   * @throws NullPointerException if the input list of {@code ResourceChange} instances is {@code null}.
   */
  public Set<String> getChangesPaths(final @NotNull List<ResourceChange> changes) {
    return changes
        .parallelStream()
        .filter(Objects::nonNull)
        .map(
            change -> {
              final String path = change.getPath();
              LOGGER.debug("Change type: {}, path: {}, isExternal: {}", change.getType(), path, change.isExternal());
              return path;
            })
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.toSet());
  }

  /** {@inheritDoc} */
  @Override
  LoggingHelper getLogger() {
    return LOGGER;
  }

  /** {@inheritDoc} */
  @Override
  JobManager getJobManager() {
    return jobManager;
  }

  @ObjectClassDefinition(
      name = "Auto CDN Invalidator - Resource Change Listener",
      description = "DO NOT CHANGE UNLESS you knowing what you are doing")
  public @interface Config {

    @AttributeDefinition(name = "Enable", type = AttributeType.BOOLEAN, description = "Tick to enable it")
    boolean isEnabled() default false;

    @AttributeDefinition(
        name = "Filter Paths",
        description = "Root paths for observed events. Regex are not supported here.")
    String[] resource_paths() default {};

    @AttributeDefinition(name = "Events", description = "Array of change types.")
    String[] resource_change_types() default {"CHANGED"};

    @AttributeDefinition(
        name = "Target Job Topic",
        description = "Defines which job consumer will be used to process data")
    String job_topic();

    @AttributeDefinition(name = "Filter Regex", description = "Pattern to identify page/asset which must be processed")
    String filter_regex();
  }
}
