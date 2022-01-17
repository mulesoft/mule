/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.memory.provider.ByteBufferPoolConfiguration;
import org.mule.runtime.api.memory.provider.ByteBufferProvider;
import org.mule.runtime.api.memory.provider.type.ByteBufferType;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class AbstractStreamingTestCase extends AbstractMuleContextTestCase {

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> startupRegistryObjects = new HashMap<>(1);
    startupRegistryObjects.put(MuleProperties.MULE_MEMORY_MANAGEMENT_SERVICE, new MemoryManagementService() {

      @Override
      public ByteBufferProvider<ByteBuffer> getByteBufferProvider(String name, ByteBufferType type,
                                                                  ByteBufferPoolConfiguration poolConfiguration) {
        throw new UnsupportedOperationException("Not implemented yet");
      }

      @Override
      public ByteBufferProvider<ByteBuffer> getByteBufferProvider(String name, ByteBufferType type) {
        return new ByteBufferProviderStub();
      }

      @Override
      public void disposeByteBufferProvider(String name) {
        // Nothing to do.
      }

      @Override
      public void dispose() {
        // Nothing to do.
      }

      @Override
      public void initialise() throws InitialisationException {
        // Nothing to do.
      }
    });
    return startupRegistryObjects;
  }

  private static class ByteBufferProviderStub implements ByteBufferProvider<ByteBuffer> {

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
      throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void release(ByteBuffer buffer) {
      buffer.clear();
    }

    @Override
    public byte[] getByteArray(int size) {
      return new byte[size];
    }

    @Override
    public void dispose() {
      // Nothing to do.
    }
  }
}
