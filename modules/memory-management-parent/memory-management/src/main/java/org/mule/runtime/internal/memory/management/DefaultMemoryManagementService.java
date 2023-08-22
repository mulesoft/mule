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

import static java.lang.String.format;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.memory.provider.ByteBufferPoolConfiguration;
import org.mule.runtime.api.memory.provider.ByteBufferProvider;
import org.mule.runtime.api.memory.provider.type.ByteBufferType;
import org.mule.runtime.api.profiling.ProfilingService;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

/**
 * A Default Implementation of {@link MemoryManagementService}
 */
public class DefaultMemoryManagementService implements ProfiledMemoryManagementService {

  public static final String DUPLICATE_BYTE_BUFFER_PROVIDER_NAME = "A ByteBuffer Provider is already registered with name '%s'.";

  private static final DefaultMemoryManagementService INSTANCE = new DefaultMemoryManagementService();
  private static final Logger LOGGER = getLogger(DefaultMemoryManagementService.class);

  private final Map<String, ByteBufferProvider<ByteBuffer>> byteBufferProviders = new ConcurrentHashMap<>();
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

  private DefaultMemoryManagementService() {}

  @Override
  public void dispose() {
    byteBufferProviders.values().forEach(ByteBufferProvider::dispose);
    byteBufferProviders.clear();
  }

  @Override
  public void initialise() throws InitialisationException {
    // Nothing to do.
  }

  @Override
  public synchronized ByteBufferProvider<ByteBuffer> getByteBufferProvider(String name, ByteBufferType byteBufferType,
                                                                           ByteBufferPoolConfiguration poolConfiguration) {
    if (name == null) {
      throw new IllegalArgumentException(BYTE_BUFFER_PROVIDER_NAME_CANNOT_BE_NULL_MESSAGE);
    }

    if (byteBufferProviders.containsKey(name)) {
      throw new IllegalArgumentException(format(DUPLICATE_BYTE_BUFFER_PROVIDER_NAME, name));
    }

    byteBufferProviders.put(name, buildByteBufferProviderFrom(byteBufferType)
        .withPoolConfiguration(poolConfiguration)
        .withProfilingService(profilingService)
        .withName(name)
        .build());

    return byteBufferProviders.get(name);
  }

  @Override
  public synchronized ByteBufferProvider<ByteBuffer> getByteBufferProvider(String name, ByteBufferType byteBufferType) {
    if (name == null) {
      throw new IllegalArgumentException(BYTE_BUFFER_PROVIDER_NAME_CANNOT_BE_NULL_MESSAGE);
    }

    if (byteBufferProviders.containsKey(name)) {
      throw new IllegalArgumentException(format(DUPLICATE_BYTE_BUFFER_PROVIDER_NAME, name));
    }

    byteBufferProviders.put(name, buildByteBufferProviderFrom(byteBufferType)
        .withName(name)
        .withProfilingService(profilingService)
        .build());

    return byteBufferProviders.get(name);
  }

  @Override
  public synchronized void disposeByteBufferProvider(String name) {
    ByteBufferProvider<ByteBuffer> bufferProvider = byteBufferProviders.remove(name);
    if (bufferProvider != null) {
      bufferProvider.dispose();
    } else {
      LOGGER.warn("Unable to dispose not present ByteBufferProvider '{}'", name);
    }
  }

  @Override
  public void setProfilingService(ProfilingService profilingService) {
    this.profilingService = profilingService;
  }
}
