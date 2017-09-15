/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.config.custom.CustomizationService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * {@inheritDoc}
 */
public class DefaultCustomizationService implements CustomizationService, CustomServiceRegistry {

  private Map<String, CustomService> muleContextDefaultServices = new HashMap<>();
  private Map<String, CustomService> customServices = new HashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> void overrideDefaultServiceImpl(String serviceId, T serviceImpl) {
    muleContextDefaultServices.put(serviceId, new CustomService(serviceImpl));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> void overrideDefaultServiceClass(String serviceId, Class<T> serviceClass) {
    muleContextDefaultServices.put(serviceId, new CustomService(serviceClass));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<CustomService> getOverriddenService(String serviceId) {
    return ofNullable(muleContextDefaultServices.get(serviceId));
  }

  @Override
  public <T> void registerCustomServiceImpl(String serviceId, T serviceImpl) {
    checkArgument(!isEmpty(serviceId), "serviceId cannot be empty");
    checkArgument(serviceImpl != null, "serviceImpl cannot be null");
    customServices.put(serviceId, new CustomService(serviceImpl));
  }

  @Override
  public <T> void registerCustomServiceClass(String serviceId, Class<T> serviceClass) {
    checkArgument(!isEmpty(serviceId), "serviceId cannot be empty");
    checkArgument(serviceClass != null, "serviceClass cannot be null");
    customServices.put(serviceId, new CustomService(serviceClass));
  }

  @Override
  public Map<String, CustomService> getCustomServices() {
    return unmodifiableMap(customServices);
  }

  public Map<String, CustomService> getDefaultServices() {
    return unmodifiableMap(muleContextDefaultServices);
  }
}
