package com.baglio.autocdninvalidator.core.listeners;

import com.baglio.autocdninvalidator.core.helpers.LoggingHelper;
import com.day.cq.replication.ReplicationAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(
    service = EventHandler.class,
    immediate = true,
    property = {EventConstants.EVENT_TOPIC + "=" + ReplicationAction.EVENT_TOPIC})
@Designate(ocd = ReplicationEventListener.Config.class, factory = true)
public class ReplicationEventListener extends AbstractListener implements EventHandler {
  private static final LoggingHelper LOGGER = new LoggingHelper(ReplicationEventListener.class);

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
  public void activate(final Config config) {
    pathsToListenFor = config.resource_paths();
    isEnabled = config.isEnabled();
    jobTopic = config.job_topic();
    filterRegex = config.filter_regex();
    LOGGER.info("Activated - enabled: {}, root paths: {}, filterRegex: {}", isEnabled, pathsToListenFor, filterRegex);
  }

  /**
   * Called by the EventAdmin service to notify the listener of an event.
   *
   * @param event The event that occurred
   */
  @Override
  public void handleEvent(final Event event) {
    if (isEnabled) {
      final ReplicationAction action = ReplicationAction.fromEvent(event);
      if (action != null) {
        final String path = action.getPath();
        processPath(path);
      }
    }
  }

  /**
   * Processes a given item path and triggers an event if it matches any of the paths to listen for.
   *
   * @param itemPath the item path to process
   * @return true if the event was processed successfully, false otherwise
   */
  public boolean processPath(final String itemPath) {
    boolean pathToProcess =
        Arrays.stream(pathsToListenFor).parallel().anyMatch(rootPath -> StringUtils.contains(itemPath, rootPath));

    boolean result;
    if (pathToProcess) {
      result = processEvent(new HashSet<>(Collections.singletonList(itemPath)), filterRegex, jobTopic);
      LOGGER.info("Result of processing: {}", result);
      return result;
    } else {
      LOGGER.info("Processing skipped for path: {}, : root folder: {}", itemPath, pathsToListenFor);
    }
    return false;
  }

  /** {@inheritDoc} */
  @Override
  LoggingHelper getLogger() {
    return LOGGER;
  }

  /** {@inheritDoc} */
  @Override
  public JobManager getJobManager() {
    return jobManager;
  }

  @ObjectClassDefinition(
      name = "Auto CDN Invalidator - Replication Event Listener",
      description = "DO NOT CHANGE UNLESS you knowing what you are doing")
  public @interface Config {

    @AttributeDefinition(name = "Enable", type = AttributeType.BOOLEAN, description = "Tick to enable it")
    boolean isEnabled();

    @AttributeDefinition(
        name = "Filter Paths",
        description = "Root paths for observed events. Regex are not supported here.")
    String[] resource_paths();

    @AttributeDefinition(
        name = "Target Job Topic",
        description = "Defines which job consumer will be used to process data")
    String job_topic();

    @AttributeDefinition(name = "Filter Regex", description = "Pattern to identify page/asset which must be processed")
    String filter_regex();
  }
}
