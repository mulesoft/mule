/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.internal.memory.management;

import static org.mule.runtime.internal.memory.bytebuffer.ByteBufferProviderBuilder.buildByteBufferProviderFrom;

import static java.lang.String.format;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.memory.provider.ByteBufferPoolConfiguration;
import org.mule.runtime.api.memory.provider.ByteBufferProvider;
import org.mule.runtime.api.memory.provider.type.ByteBufferType;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.internal.memory.profiling.NoOpMemoryProfilingService;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * A Default Implementation of {@link MemoryManagementService}
 */
public class DefaultMemoryManagementService implements ProfiledMemoryManagementService {

  public static final String DUPLICATE_BYTE_BUFFER_PROVIDER_NAME = "A ByteBuffer Provider is already registered with name '%s'.";

  private static final DefaultMemoryManagementService INSTANCE = new DefaultMemoryManagementService();

  private final Map<String, ByteBufferProvider<ByteBuffer>> byteBufferProviders = new HashMap<>();
  private ProfilingService profilingService = new NoOpMemoryProfilingService();

  public static DefaultMemoryManagementService getInstance() {
    return INSTANCE;
  }

  private DefaultMemoryManagementService() {};

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
    if (byteBufferProviders.containsKey(name)) {
      throw new IllegalArgumentException(format(DUPLICATE_BYTE_BUFFER_PROVIDER_NAME, name));
    }

    return byteBufferProviders.computeIfAbsent(name, n -> buildByteBufferProviderFrom(byteBufferType)
        .withPoolConfiguration(poolConfiguration)
        .withProfilingService(profilingService)
        .build());

  }

  @Override
  public synchronized ByteBufferProvider<ByteBuffer> getByteBufferProvider(String name, ByteBufferType byteBufferType) {
    if (byteBufferProviders.containsKey(name)) {
      throw new IllegalArgumentException(format(DUPLICATE_BYTE_BUFFER_PROVIDER_NAME, name));
    }

    return byteBufferProviders.computeIfAbsent(name, n -> buildByteBufferProviderFrom(byteBufferType)
        .withName(name)
        .withProfilingService(profilingService)
        .build());
  }

  @Override
  public synchronized void disposeByteBufferProvider(String name) {
    byteBufferProviders.remove(name).dispose();
  }

  @Override
  public void setProfilingService(ProfilingService profilingService) {
    this.profilingService = profilingService;
  }
}
