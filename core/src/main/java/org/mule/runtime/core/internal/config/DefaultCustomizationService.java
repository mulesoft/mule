/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.config.custom.CustomizationService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@inheritDoc}
 */
public class DefaultCustomizationService implements CustomizationService, CustomServiceRegistry {

  private final Map<String, CustomService> muleContextDefaultServices = new HashMap<>();
  private final Map<String, Consumer<ServiceOverrider>> muleContextDefaultServicesOverrider = new HashMap<>();
  private final Map<String, CustomService> customServices = new HashMap<>();

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
  public void overrideDefaultServiceImpl(String serviceId, Consumer<ServiceOverrider> serviceOverrider) {
    muleContextDefaultServicesOverrider.put(serviceId, serviceOverrider);
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

  @Override
  public Optional<Object> overrideDefaultService(String serviceId, Object serviceImpl) {
    if (!muleContextDefaultServicesOverrider.containsKey(serviceId)) {
      return of(serviceImpl);
    }

    DefaultServiceOverrider serviceOverrider = new DefaultServiceOverrider(serviceImpl);
    muleContextDefaultServicesOverrider.get(serviceId).accept(serviceOverrider);

    return serviceOverrider.isRemove() ? empty() : of(serviceOverrider.getOverrider());
  }

  private static class DefaultServiceOverrider implements ServiceOverrider {

    private final Object serviceImpl;
    private Object overrider;
    private boolean remove;

    public DefaultServiceOverrider(Object serviceImpl) {
      this.serviceImpl = serviceImpl;
    }

    @Override
    public Object getOverridee() {
      return serviceImpl;
    }

    @Override
    public void override(Object overrider) {
      this.overrider = overrider;
    }

    @Override
    public void remove() {
      if (overrider != null) {
        throw new IllegalStateException("An 'overrider' service is already present");
      }

      remove = true;
    }

    public Object getOverrider() {
      if (remove) {
        throw new IllegalStateException("Service set to be removed, can't be overridden");
      }

      return overrider;
    }

    public boolean isRemove() {
      return remove;
    }

  }

  public Map<String, CustomService> getDefaultServices() {
    return unmodifiableMap(muleContextDefaultServices);
  }
}
