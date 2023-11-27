package com.baglio.autocdninvalidator.core.service;

import java.util.Set;

/** A service interface for invalidating CDN (Content Delivery Network) cache by different criteria. */
public interface CdnInvalidationService {

  /**
   * Deletes CDN content on the selected set of cache tags.
   *
   * @param tags An array of cache tag strings you want to purge.
   * @return true if invalidation succeed, false otherwise.
   */
  boolean invalidateByTag(Set<String> tags);

  /**
   * Deletes CDN content on the selected CP code.
   *
   * @param codes An array of the CP codes you want to purge.
   * @return true if invalidation succeed, false otherwise.
   */
  boolean invalidateByCode(Set<String> codes);

  /**
   * Deletes CDN content on the selected URL.
   *
   * @param urls Lists URLs or ARLs to purge.
   * @return true if invalidation succeed, false otherwise.
   */
  boolean invalidateByURLs(Set<String> urls);
}
