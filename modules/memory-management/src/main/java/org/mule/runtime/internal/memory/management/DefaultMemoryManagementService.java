/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.internal.memory.management;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.memory.provider.ByteBufferProvider;
import org.mule.runtime.api.memory.provider.ByteBufferProviderConfiguration;

/**
 * A Default Implementation of {@link MemoryManagementService}
 */
public class DefaultMemoryManagementService implements MemoryManagementService {

  private static DefaultMemoryManagementService INSTANCE = new DefaultMemoryManagementService();

  public static DefaultMemoryManagementService getInstance() {
    return INSTANCE;
  }

  private DefaultMemoryManagementService() {};

  @Override
  public void dispose() {
    // TODO EE-8096 Default ByteBufferProvider implementation in memory management
  }

  @Override
  public void initialise() throws InitialisationException {
    // TODO EE-8096 Default ByteBufferProvider implementation in memory management
  }

  @Override
  public ByteBufferProvider getByteBufferProvider(String name, ByteBufferProviderConfiguration configuration) {
    // TODO EE-8096 Default ByteBufferProvider implementation in memory management
    return null;
  }
}
