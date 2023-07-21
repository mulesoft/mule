/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.internal.memory.management;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.memory.provider.ByteBufferPoolConfiguration;
import org.mule.runtime.api.memory.provider.ByteBufferProvider;
import org.mule.runtime.api.memory.provider.type.ByteBufferType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link MemoryManagementService} that handles memory resources for a mule artifact.
 */
public class ArtifactMemoryManagementService implements MemoryManagementService {

  private final MemoryManagementService containerMemoryManagementService;

  private final Map<String, ByteBufferProvider<?>> byteBufferProviders = new ConcurrentHashMap<>();

  public ArtifactMemoryManagementService(MemoryManagementService containerMemoryManagementService) {
    this.containerMemoryManagementService = containerMemoryManagementService;
  }

  @Override
  public ByteBufferProvider<?> getByteBufferProvider(String name, ByteBufferType byteBufferType,
                                                     ByteBufferPoolConfiguration byteBufferPoolConfiguration) {
    ByteBufferProvider<?> byteBufferProvider =
        containerMemoryManagementService.getByteBufferProvider(name, byteBufferType, byteBufferPoolConfiguration);
    byteBufferProviders.put(name, byteBufferProvider);
    return byteBufferProvider;
  }

  @Override
  public ByteBufferProvider<?> getByteBufferProvider(String name, ByteBufferType byteBufferType) {
    ByteBufferProvider<?> byteBufferProvider = containerMemoryManagementService.getByteBufferProvider(name, byteBufferType);
    byteBufferProviders.put(name, byteBufferProvider);
    return byteBufferProvider;
  }

  @Override
  public void disposeByteBufferProvider(String name) {
    containerMemoryManagementService.disposeByteBufferProvider(name);
    byteBufferProviders.remove(name);

  }

  @Override
  public void dispose() {
    byteBufferProviders.keySet().forEach(this::disposeByteBufferProvider);
  }

  @Override
  public void initialise() throws InitialisationException {
    // Nothing to do.
  }
}
