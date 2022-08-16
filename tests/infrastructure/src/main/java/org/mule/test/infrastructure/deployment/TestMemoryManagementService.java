/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.infrastructure.deployment;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.memory.provider.ByteBufferPoolConfiguration;
import org.mule.runtime.api.memory.provider.ByteBufferProvider;
import org.mule.runtime.api.memory.provider.type.ByteBufferType;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.internal.memory.management.ProfiledMemoryManagementService;

import java.nio.ByteBuffer;

/**
 * A {@link ProfiledMemoryManagementService} that does not do anything. Used for testing in cases where several fake mule servers
 * are created using the same classloader.
 *
 * @since 4.5.0
 */
public class TestMemoryManagementService implements ProfiledMemoryManagementService {

  private static final ByteBufferProvider<ByteBuffer> DUMMY_BYTE_BUFFER_PROVIDER = new ByteBufferProvider() {

    @Override
    public ByteBuffer allocate(int size) {
      return ByteBuffer.allocate(size);
    }

    @Override
    public ByteBuffer allocateAtLeast(int size) {
      return ByteBuffer.allocate(size);
    }

    @Override
    public ByteBuffer reallocate(ByteBuffer oldBuffer, int newSize) {
      return ByteBuffer.allocate(newSize);
    }

    @Override
    public void release(ByteBuffer buffer) {
      // Nothing to do.
    }

    @Override
    public byte[] getByteArray(int size) {
      return new byte[size];
    }

    @Override
    public void dispose() {
      // Nothing to do.
    }
  };

  @Override
  public void dispose() {
    // Nothing to do.
  }

  @Override
  public void initialise() throws InitialisationException {
    // Nothing to do.
  }

  @Override
  public ByteBufferProvider<ByteBuffer> getByteBufferProvider(String name, ByteBufferType type,
                                                              ByteBufferPoolConfiguration poolConfiguration) {
    return DUMMY_BYTE_BUFFER_PROVIDER;
  }

  @Override
  public ByteBufferProvider<ByteBuffer> getByteBufferProvider(String name, ByteBufferType type) {
    return DUMMY_BYTE_BUFFER_PROVIDER;
  }

  @Override
  public void disposeByteBufferProvider(String name) {
    // Nothing to do.
  }

  @Override
  public void setProfilingService(ProfilingService profilingService) {
    // Nothing to do.
  }
}
