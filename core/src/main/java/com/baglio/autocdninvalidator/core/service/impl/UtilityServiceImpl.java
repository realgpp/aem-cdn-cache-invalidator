package com.baglio.autocdninvalidator.core.service.impl;

import static com.baglio.autocdninvalidator.core.utils.Constants.CONFIGURATION_ID;

import com.baglio.autocdninvalidator.core.helpers.LoggingHelper;
import com.baglio.autocdninvalidator.core.service.UtilityService;
import java.util.Collection;
import java.util.Optional;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;

/** A utility service that provides methods to get other services by their class and service ID. */
@Component(service = UtilityService.class, immediate = true)
public class UtilityServiceImpl implements UtilityService {
  private static final LoggingHelper LOGGER = new LoggingHelper(UtilityServiceImpl.class);

  /**
   * Gets a service of a given class and service ID from the bundle context.
   *
   * @param tClass the class of the service to get
   * @param serviceId the service ID of the service to get
   * @return the service object if found, null otherwise
   */
  @Override
  public <T> T getService(final Class<T> tClass, final String serviceId) {
    try {
      // Get bundle context
      BundleContext context = FrameworkUtil.getBundle(tClass).getBundleContext();
      Collection<ServiceReference<T>> refs = context.getServiceReferences(tClass, null);

      Optional<T> optionalHttpClientService =
          refs.parallelStream()
              .filter(
                  cdnInvalidationServiceServiceReference ->
                      serviceId.equals(cdnInvalidationServiceServiceReference.getProperty(CONFIGURATION_ID)))
              .findFirst()
              .map(context::getService);

      if (optionalHttpClientService.isPresent()) {
        return optionalHttpClientService.get();
      }
      LOGGER.warn("No service was found - class: {}, serviceId: {}", tClass, serviceId);
    } catch (Exception e) {
      LOGGER.error("Impossible to get {}", tClass, e);
    }
    return null;
  }
}
