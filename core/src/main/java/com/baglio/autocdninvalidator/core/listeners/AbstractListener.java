package com.baglio.autocdninvalidator.core.listeners;

import com.baglio.autocdninvalidator.core.helpers.LoggingHelper;
import com.baglio.autocdninvalidator.core.jobs.EditorialAssetInvalidationJobConsumer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;

/**
 * Abstract base class for listeners that process repository change events. Provides common logic to filter paths,
 * create jobs, and offload work.
 */
public abstract class AbstractListener {
  /**
   * Gives the Logger.
   *
   * @return the Logger
   */
  abstract LoggingHelper getLogger();

  /**
   * Gets the OSGi JobManager service to create jobs.
   *
   * @return the JobManager
   */
  abstract JobManager getJobManager();

  /**
   * Processes a set of resource paths when changed in the repository. Applies configured regex filter and offloads work
   * via job.
   *
   * @param paths the resource paths changed
   * @param filterRegex regex to filter relevant paths
   * @param jobTopic job topic to use for offloading
   * @return true if a job was scheduled, false otherwise
   */
  public boolean processEvent(final Set<String> paths, final String filterRegex, final String jobTopic) {

    Set<String> resourcePaths = filterPaths(paths, filterRegex);

    if (resourcePaths.isEmpty()) {
      getLogger().warn("No resources to process for paths={} with filter regex={}", paths, filterRegex);
      return false;
    }
    Map<String, Object> jobprops = new HashMap<>();
    jobprops.put(EditorialAssetInvalidationJobConsumer.JOB_PROPERTY_PATHS, resourcePaths);

    Job offloadingJob = getJobManager().addJob(jobTopic, jobprops);
    if (offloadingJob == null) {
      getLogger().error("Job could not be created");
    }
    return offloadingJob != null;
  }

  /**
   * Filters the set of paths using the supplied regex.
   *
   * @param paths the paths to filter
   * @param filterRegex the regex to apply
   * @return filtered set of paths
   */
  Set<String> filterPaths(final Set<String> paths, final String filterRegex) {
    return paths.stream()
        .map(
            path -> {
              getLogger().trace("Path to check: {}", path);
              if (StringUtils.contains(path, JcrConstants.JCR_CONTENT)) {
                return path.split("/" + JcrConstants.JCR_CONTENT, 2)[0];
              }
              return path;
            })
        .filter(path -> Pattern.matches(StringUtils.isBlank(filterRegex) ? ".*" : filterRegex, path))
        .collect(Collectors.toSet());
  }
}
