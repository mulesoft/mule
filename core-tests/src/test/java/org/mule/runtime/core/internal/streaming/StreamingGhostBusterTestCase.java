/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static java.lang.System.gc;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.internal.streaming.IdentifiableCursorProviderDecorator.of;
import static org.mule.tck.probe.PollingProber.DEFAULT_POLLING_INTERVAL;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.STREAM_MANAGEMENT;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.memory.provider.ByteBufferPoolConfiguration;
import org.mule.runtime.api.memory.provider.ByteBufferProvider;
import org.mule.runtime.api.memory.provider.type.ByteBufferType;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.internal.streaming.bytes.ManagedCursorStreamProvider;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(STREAMING)
@Story(STREAM_MANAGEMENT)
public class StreamingGhostBusterTestCase extends AbstractMuleContextTestCase {

  private static final int GC_POLLING_TIMEOUT = 10000;

  private StreamingGhostBuster ghostBuster;

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
        return new MockedByteBufferProvider();
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

  private static class MockedByteBufferProvider implements ByteBufferProvider<ByteBuffer> {

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

  @Override
  protected void doSetUp() throws Exception {
    ghostBuster = new StreamingGhostBuster();
    initialiseIfNeeded(ghostBuster, true, muleContext);
    startIfNeeded(ghostBuster);
  }

  @Override
  protected void doTearDown() throws MuleException {
    ghostBuster.stop();
    ghostBuster.dispose();
  }

  @Test
  @Issue("MULE-18573")
  public void releaseResourcesWhenReferenceIsCollected() {
    MutableStreamingStatistics statistics = mock(MutableStreamingStatistics.class);
    CursorStreamProvider provider = mock(CursorStreamProvider.class);
    ManagedCursorStreamProvider managedCursorProvider = new ManagedCursorStreamProvider(of(provider), statistics);

    WeakReference<ManagedCursorProvider> reference = ghostBuster.track(managedCursorProvider);

    // Force GC collection
    managedCursorProvider = null;

    check(GC_POLLING_TIMEOUT, DEFAULT_POLLING_INTERVAL, () -> {
      gc();
      assertThat(reference.get(), is(nullValue()));
      verify(provider).releaseResources();
      return true;
    });
  }
}
