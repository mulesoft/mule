/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config;

import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCK_PROVIDER;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.lang.Boolean.getBoolean;

import org.mule.runtime.api.artifact.ArtifactType;
import org.mule.runtime.api.lock.LockProvider;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.core.internal.lock.TwoImplementationsLockProvider;
import org.mule.runtime.core.internal.store.TwoImplementationsObjectStore;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * {@inheritDoc}
 */
public class DefaultCustomizationService implements InternalCustomizationService {

  // Not retrieving the value once for testing purposes
  public static final String HA_MIGRATION_ENABLED_PROPERTY = SYSTEM_PROPERTY_PREFIX + "enable.ha.migration";
  private final Map<String, CustomService> muleContextDefaultServices = new HashMap<>();
  private final Map<String, CustomService> customServices = new HashMap<>();

  private ArtifactType artifactType;
  private Map<String, String> artifactProperties = new HashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> void overrideDefaultServiceImpl(String serviceId, T serviceImpl) {
    muleContextDefaultServices
        .put(serviceId,
             new CustomService<>(serviceId, serviceInterceptor -> serviceInterceptor.overrideServiceImpl(serviceImpl), false));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> void interceptDefaultServiceImpl(String serviceId, Consumer<ServiceInterceptor<T>> serviceInterceptor) {
    if (isMigrationSecondService(serviceId)) {
      CustomService first = muleContextDefaultServices.get(serviceId);
      CustomService second = new CustomService<>(serviceId, serviceInterceptor, false);
      muleContextDefaultServices.put(serviceId, combineServicesForMigration(serviceId, first, second));
    } else {
      muleContextDefaultServices.put(serviceId, new CustomService<>(serviceId, serviceInterceptor, false));
    }
  }

  private boolean isMigrationSecondService(String serviceId) {
    return muleContextDefaultServices.containsKey(serviceId)
        && (serviceId.equals(OBJECT_LOCK_PROVIDER) || serviceId.equals(BASE_IN_MEMORY_OBJECT_STORE_KEY))
        && getBoolean(HA_MIGRATION_ENABLED_PROPERTY);
  }

  private CustomService combineServicesForMigration(String serviceId, CustomService first, CustomService second) {
    if (serviceId.equals(OBJECT_LOCK_PROVIDER)) {
      return new CombinedService(serviceId, first, second, false,
                                 (impl1, impl2) -> new TwoImplementationsLockProvider((LockProvider) impl1,
                                                                                      (LockProvider) impl2));
    } else if (serviceId.equals(BASE_IN_MEMORY_OBJECT_STORE_KEY)) {
      return new CombinedService(serviceId, first, second, false,
                                 (impl1, impl2) -> new TwoImplementationsObjectStore((ObjectStore) impl1, (ObjectStore) impl2));
    } else {
      return second;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> void overrideDefaultServiceClass(String serviceId, Class<T> serviceClass) {
    muleContextDefaultServices.put(serviceId, new CustomService<>(serviceId, serviceClass, false));
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
    registerCustomServiceImpl(serviceId, serviceImpl, false);
  }

  @Override
  public <T> void registerCustomServiceImpl(String serviceId, T serviceImpl, boolean baseContext) {
    checkArgument(!isEmpty(serviceId), "serviceId cannot be empty");
    requireNonNull(serviceImpl, "serviceImpl cannot be null");
    customServices.put(serviceId,
                       new CustomService<>(serviceId,
                                           serviceInterceptor -> serviceInterceptor.overrideServiceImpl(serviceImpl),
                                           baseContext));
  }

  @Override
  public <T> void registerCustomServiceClass(String serviceId, Class<T> serviceClass) {
    registerCustomServiceClass(serviceId, serviceClass, false);
  }

  @Override
  public <T> void registerCustomServiceClass(String serviceId, Class<T> serviceClass, boolean baseContext) {
    checkArgument(!isEmpty(serviceId), "serviceId cannot be empty");
    requireNonNull(serviceClass, "serviceClass cannot be null");
    customServices.put(serviceId, new CustomService<>(serviceId, serviceClass, baseContext));
  }

  @Override
  public Map<String, CustomService> getCustomServices() {
    return unmodifiableMap(customServices);
  }

  @Override
  public Map<String, CustomService> getDefaultServices() {
    return unmodifiableMap(muleContextDefaultServices);
  }

  @Override
  public void setArtifactProperties(Map<String, String> artifactProperties) {
    this.artifactProperties = unmodifiableMap(new HashMap<>(artifactProperties));
  }

  @Override
  public Map<String, String> getArtifactProperties() {
    return artifactProperties;
  }

  @Override
  public void setArtifactType(ArtifactType artifactType) {
    this.artifactType = artifactType;
  }

  @Override
  public ArtifactType getArtifactType() {
    return artifactType;
  }

}
