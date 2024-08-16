/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.internal.memory.management;

import static org.mule.runtime.internal.memory.bytebuffer.ByteBufferProviderBuilder.BYTE_BUFFER_PROVIDER_NAME_CANNOT_BE_NULL_MESSAGE;
import static org.mule.runtime.internal.memory.bytebuffer.ByteBufferProviderBuilder.buildByteBufferProviderFrom;

import static org.mule.runtime.internal.memory.profiling.NoOpMemoryProfilingService.getNoOpMemoryProfilingService;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.memory.provider.ByteBufferPoolConfiguration;
import org.mule.runtime.api.memory.provider.ByteBufferProvider;
import org.mule.runtime.api.memory.provider.type.ByteBufferType;
import org.mule.runtime.api.profiling.ProfilingService;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

/**
 * A Default Implementation of {@link MemoryManagementService}
 */
public class DefaultMemoryManagementService implements ProfiledMemoryManagementService {

  private static final DefaultMemoryManagementService INSTANCE = new DefaultMemoryManagementService();
  static Logger LOGGER = getLogger(DefaultMemoryManagementService.class);

  private final Map<String, ByteBufferProvider<ByteBuffer>> byteBufferProviders;

  private final Map<String, Long> byteBufferProvidersUsageCount;
  private ProfilingService profilingService = getNoOpMemoryProfilingService();

  public static DefaultMemoryManagementService getInstance() {
    return INSTANCE;
  }

  /**
   * This is added for testing purposes when we want to create a different service for fake test components.
   *
   * @return a new independent {@link DefaultMemoryManagementService}
   */
  public static DefaultMemoryManagementService newDefaultMemoryManagementService() {
    return new DefaultMemoryManagementService();
  }

  private DefaultMemoryManagementService() {
    this(new HashMap<>(), new HashMap<>());
  }

  DefaultMemoryManagementService(Map<String, ByteBufferProvider<ByteBuffer>> byteBufferProviders,
                                 Map<String, Long> byteBufferProvidersCount) {
    this.byteBufferProviders = byteBufferProviders;
    this.byteBufferProvidersUsageCount = byteBufferProvidersCount;
  }

  @Override
  public void dispose() {
    synchronized (byteBufferProvidersUsageCount) {
      byteBufferProviders.values().forEach(ByteBufferProvider::dispose);
      byteBufferProviders.clear();
      byteBufferProvidersUsageCount.clear();
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    // Nothing to do.
  }

  @Override
  public synchronized ByteBufferProvider<ByteBuffer> getByteBufferProvider(String name, ByteBufferType byteBufferType,
                                                                           ByteBufferPoolConfiguration poolConfiguration) {
    return doGetByteBufferProvider(name, byteBufferType, poolConfiguration);
  }

  @Override
  public ByteBufferProvider<ByteBuffer> getByteBufferProvider(String name, ByteBufferType byteBufferType) {
    return doGetByteBufferProvider(name, byteBufferType, null);
  }

  private ByteBufferProvider<ByteBuffer> doGetByteBufferProvider(String name, ByteBufferType byteBufferType,
                                                                 ByteBufferPoolConfiguration poolConfiguration) {
    if (name == null) {
      throw new IllegalArgumentException(BYTE_BUFFER_PROVIDER_NAME_CANNOT_BE_NULL_MESSAGE);
    }

    synchronized (byteBufferProvidersUsageCount) {
      ByteBufferProvider<ByteBuffer> byteBufferProvider =
          byteBufferProviders.computeIfAbsent(name, thisName -> buildByteBufferProviderFrom(byteBufferType)
              .withName(name)
              .withPoolConfiguration(poolConfiguration)
              .withProfilingService(profilingService)
              .build());

      byteBufferProvidersUsageCount.putIfAbsent(name, 0L);
      byteBufferProvidersUsageCount.computeIfPresent(name, (key, val) -> val + 1);


      return byteBufferProvider;
    }
  }

  @Override
  public void disposeByteBufferProvider(String name) {
    synchronized (byteBufferProvidersUsageCount) {
      Long count = byteBufferProvidersUsageCount.get(name);
      if (count != null) {
        disposeOrReduceUsageCount(name, count);
      } else {
        LOGGER.warn("Unable to dispose not present ByteBufferProvider '{}'", name);
      }
    }
  }

  private void disposeOrReduceUsageCount(String name, Long count) {
    if (count == 1) {
      ByteBufferProvider<ByteBuffer> byteBufferProvider = byteBufferProviders.get(name);
      byteBufferProvider.dispose();
      byteBufferProviders.remove(name);
      byteBufferProvidersUsageCount.remove(name);
    } else {
      byteBufferProvidersUsageCount.put(name, byteBufferProvidersUsageCount.get(name) - 1);
    }
  }

  @Override
  public void setProfilingService(ProfilingService profilingService) {
    this.profilingService = profilingService;
  }
}
