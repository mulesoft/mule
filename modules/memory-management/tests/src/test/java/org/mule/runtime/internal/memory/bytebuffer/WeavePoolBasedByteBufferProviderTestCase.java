/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.internal.memory.bytebuffer;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.nio.ByteBuffer;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.test.allure.AllureConstants.MemoryManagement.MEMORY_MANAGEMENT;
import static org.mule.test.allure.AllureConstants.MemoryManagement.MemoryManagementServiceStory.BYTE_BUFFER_PROVIDER;

@Feature(MEMORY_MANAGEMENT)
@Story(BYTE_BUFFER_PROVIDER)
public class WeavePoolBasedByteBufferProviderTestCase extends AbstractMuleTestCase {

  private static final int TEST_MAX_BUFFER_SIZE = 1000;
  private static final int TEST_NUMBER_OF_POOLS = 3;
  public static final String TEST_WEAVE_BUFFER_PROVIDER = "test-heap-buffer-provider";

  private final WeavePoolBasedByteBufferProvider weavePoolBasedByteBufferProvider =
      new WeavePoolBasedByteBufferProvider(TEST_WEAVE_BUFFER_PROVIDER, TEST_MAX_BUFFER_SIZE,
                                           TEST_NUMBER_OF_POOLS, mock(ProfilingService.class));

  @Test
  public void testInitialStatus() {
    WeavePoolBasedByteBufferProvider.WeaveByteBufferPool pool = weavePoolBasedByteBufferProvider.getPool();
    assertThat(pool.size(), is(0));
  }

  @Test
  public void testAllocationTest() {
    WeavePoolBasedByteBufferProvider.WeaveByteBufferPool pool = weavePoolBasedByteBufferProvider.getPool();

    // Assertion concerning the retrieved byte buffer. The capacity is always the MAX_BUFFER_SIZE.
    ByteBuffer smallByteBuffer = weavePoolBasedByteBufferProvider.allocate(1);
    assertByteBuffer(smallByteBuffer, TRUE);

    // The pool is still empty because no ByteBuffer were released yet.
    assertThat(pool.size(), is(0));
    ByteBuffer mediumByteBuffer = weavePoolBasedByteBufferProvider.allocate(12);
    assertThat(pool.size(), is(0));

    // Assertion concerning the retrieved byte buffer. The capacity is always the MAX_BUFFER_SIZE
    assertByteBuffer(mediumByteBuffer, TRUE);

    // Releasing DirectByteBuffers should add them to the pool to be reused.
    weavePoolBasedByteBufferProvider.release(mediumByteBuffer);
    assertThat(pool.size(), is(1));
    weavePoolBasedByteBufferProvider.release(smallByteBuffer);
    assertThat(pool.size(), is(2));

    // New allocations should take byteBuffers from the pool.
    weavePoolBasedByteBufferProvider.allocate(100);
    assertThat(pool.size(), is(1));
    weavePoolBasedByteBufferProvider.allocate(100);
    assertThat(pool.size(), is(0));

    // When the number of DirectByteBuffers offered is equal to the NUMBER_OF_POOLS, heap buffers are offered.
    ByteBuffer thirdDirectByteBuffer = weavePoolBasedByteBufferProvider.allocate(100);
    assertByteBuffer(thirdDirectByteBuffer, TRUE);
    ByteBuffer fourthHeapByteBuffer = weavePoolBasedByteBufferProvider.allocate(100);
    assertByteBuffer(fourthHeapByteBuffer, FALSE);

    // Releasing a heap byteBuffer shouldn't be added to the pool.
    weavePoolBasedByteBufferProvider.release(fourthHeapByteBuffer);
    assertThat(pool.size(), is(0));
  }

  @Test
  public void testDisposeByteBufferProvider() {
    WeavePoolBasedByteBufferProvider.WeaveByteBufferPool pool = weavePoolBasedByteBufferProvider.getPool();
    ByteBuffer directByteBuffer = weavePoolBasedByteBufferProvider.allocate(100);
    ByteBuffer otherDirectByteBuffer = weavePoolBasedByteBufferProvider.allocate(100);
    weavePoolBasedByteBufferProvider.release(directByteBuffer);
    weavePoolBasedByteBufferProvider.release(otherDirectByteBuffer);
    assertThat(pool.size(), is(2));

    weavePoolBasedByteBufferProvider.dispose();
    assertThat(pool.size(), is(0));
  }

  private void assertByteBuffer(ByteBuffer smallByteBuffer, Boolean isDirect) {
    assertThat(smallByteBuffer.isDirect(), is(isDirect));
    assertThat(smallByteBuffer.capacity(), is(TEST_MAX_BUFFER_SIZE));
    assertThat(smallByteBuffer.limit(), is(TEST_MAX_BUFFER_SIZE));
    assertThat(smallByteBuffer.position(), is(0));
  }

}
