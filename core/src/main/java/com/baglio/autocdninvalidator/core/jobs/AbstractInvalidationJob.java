package com.baglio.autocdninvalidator.core.jobs;

import com.baglio.autocdninvalidator.core.service.ReadService;
import com.baglio.autocdninvalidator.core.service.UtilityService;
import com.day.cq.commons.Externalizer;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Abstract base class for invalidation jobs. */
public abstract class AbstractInvalidationJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractInvalidationJob.class);

  /**
   * Returns the enabled state of this job.
   *
   * @return true if this job is enabled, false otherwise
   */
  public abstract boolean isEnabled();
  /**
   * Hook method called before processing the values to be used with invalidation by code/tag service.
   *
   * @param paths the content paths
   * @return initial values
   */
  public abstract Set<String> preprocessInvalidationValues(Set<String> paths);

  /**
   * Hook method called after processing the invalidation values and before calling CDN service.
   *
   * @param values Invalidation values
   * @return finalized values
   */
  public abstract Set<String> postprocessInvalidationValues(Set<String> values);

  /**
   * Hook method called before processing the public URLs.
   *
   * @param paths The paths to get public URLs for
   * @return The updated set of paths
   */
  public abstract Set<String> preprocessPublicUrls(Set<String> paths);

  /**
   * Hook method called after processing the public URLs and before calling CDN service.
   *
   * @param paths The public URLs
   * @return The updated set of public URLs
   */
  public abstract Set<String> postprocessPublicUrls(Set<String> paths);

  /**
   * Hook method called before issuing CDN invalidation request.
   *
   * @param values the set of processed invalidation values
   * @return The updated set of values
   */
  public abstract Set<String> beforeInvalidation(Set<String> values);

  /**
   * Hook method called after issuing CDN invalidation request.
   *
   * @param result the result of CDN invalidation request
   * @param job the current job
   * @return true if invalidation succeed, false otherwise.
   */
  public abstract boolean afterInvalidation(boolean result, Job job);
  /**
   * Gets the externalizer.
   *
   * @return The externalizer
   */
  abstract Externalizer getExternalizer();

  /**
   * Gets the read service.
   *
   * @return The read service
   */
  abstract ReadService getReadService();

  /**
   * Gets the utility service.
   *
   * @return The utility service
   */
  abstract UtilityService getUtilityService();

  /**
   * Converts a set of content paths to a set of public URLs that can be accessed by external users. The method uses a
   * resource resolver to map the paths to their corresponding HTML pages, and then uses an externalizer to generate the
   * publish links for those pages. The method returns a set of unique public URLs that match the input paths. If any
   * exception occurs during the conversion, the method logs an error message and returns an empty set.
   *
   * @param paths the set of content paths to convert
   * @return the set of public URLs for the content paths
   */
  Set<String> getPublicUrls(final Set<String> paths) {
    Set<String> result = new HashSet<>();
    try (ResourceResolver resourceResolver = getReadService().getResourceResolver()) {
      result =
          paths
              .parallelStream()
              .map(
                  path -> {
                    String newPath = resourceResolver.map(path) + ".html";
                    return getExternalizer().publishLink(resourceResolver, newPath);
                  })
              .collect(Collectors.toSet());
    } catch (Exception e) {
      LOGGER.error("Impossible to compute public urls for paths={}", paths, e);
    }
    return result;
  }

  /**
   * As part of invalidation by code or tag process, it generates invalidation values for the Akamai CDN by applying
   * rules to content paths that have changed. The rules are defined as key-value pairs, where the key is a regular
   * expression pattern that matches a path, and the value is a replacement string that is used to create the
   * invalidation value. For example, if the rule is "products/(.*)=tag-$1", and the path is "products/shoes", then the
   * invalidation value will be "tag-shoe". This method iterates over the paths and the rules, and returns a set of
   * unique invalidation values that match the paths.
   *
   * @param paths the set of content paths that have changed
   * @param invalidationRules the map of rules mapping path patterns to invalidation values
   * @return the set of invalidation values for the Akamai CDN
   */
  Set<String> getInvalidationValues(final Set<String> paths, final Map<String, String> invalidationRules) {
    // Check if the input parameters are null and log an error message if they are
    if (paths == null || invalidationRules == null) {
      LOGGER.error("No valid mandatory inputs: paths: {}, invalidationRules: {}", paths, invalidationRules);
      return new HashSet<>();
    }

    return paths.stream()
        .flatMap(
            path ->
                invalidationRules.entrySet().parallelStream() // Create a stream of map entries
                    .map(
                        item ->
                            new AbstractMap.SimpleEntry<>(
                                Pattern.compile(item.getKey()).matcher(path),
                                item.getValue())) // Map the entries to a pair of matcher and value
                    .filter(e -> e.getKey().matches()) // Filter the pairs that match the path
                    .map(e -> e.getKey().replaceAll(e.getValue())) // Map the pairs to the replaced values
                    .filter(StringUtils::isNotBlank) // Filter the values that are not blank
                    .collect(Collectors.toSet()).stream())
        .collect(Collectors.toSet());
  }
}
