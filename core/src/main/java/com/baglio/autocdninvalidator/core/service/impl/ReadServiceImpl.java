package com.baglio.autocdninvalidator.core.service.impl;

import com.baglio.autocdninvalidator.core.helpers.LoggingHelper;
import com.baglio.autocdninvalidator.core.service.ReadService;
import java.util.HashMap;
import java.util.Map;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
    immediate = true,
    service = {ReadService.class})
public class ReadServiceImpl implements ReadService {
  private static final LoggingHelper LOGGER = new LoggingHelper(ReadServiceImpl.class);
  @Reference private ResourceResolverFactory resolverFactory;

  /**
   * Gets a resource resolver using the autocdninvalidatorReadService subservice.
   *
   * <p>The resolver is retrieved from the {@code resolverFactory} using a map of authentication info specifying the
   * subservice name.
   *
   * @return The {@code ResourceResolver} instance or {@code null} if there was an error getting it.
   */
  @Override
  public ResourceResolver getResourceResolver() {
    Map<String, Object> authInfo = new HashMap<>();
    authInfo.put("sling.service.subservice", "autocdninvalidatorReadService");

    try {
      return this.resolverFactory.getServiceResourceResolver(authInfo);
    } catch (Exception var3) {
      LOGGER.error("Error retrieving resource resolver", var3);
      return null;
    }
  }
}
