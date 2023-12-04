package com.baglio.autocdninvalidator.core.service;

public interface UtilityService {
  <T> T getService(Class<T> tClass, String serviceId);
}
