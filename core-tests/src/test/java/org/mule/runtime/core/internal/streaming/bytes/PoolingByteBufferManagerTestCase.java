/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;

@SmallTest
@Features(STREAMING)
public class PoolingByteBufferManagerTestCase extends AbstractMuleTestCase {

  private PoolingByteBufferManager bufferManager = new PoolingByteBufferManager();
  private static final int CAPACITY = 100;
  private static final int OTHER_CAPACITY = CAPACITY + 1;

  @After
  public void after() {
    bufferManager.dispose();
  }

  @Test
  public void pooling() throws Exception {
    ByteBuffer buffer = bufferManager.allocate(CAPACITY);
    bufferManager.deallocate(buffer);

    ByteBuffer newBuffer = bufferManager.allocate(CAPACITY);
    assertThat(buffer, is(sameInstance(newBuffer)));
  }

  @Test
  public void grow() throws Exception {
    ByteBuffer buffer = bufferManager.allocate(CAPACITY);
    ByteBuffer newBuffer = bufferManager.allocate(CAPACITY);

    assertThat(buffer, not(sameInstance(newBuffer)));
  }

  @Test
  public void differentPoolsPerCapacity() throws Exception {
    ByteBuffer buffer = bufferManager.allocate(CAPACITY);
    bufferManager.deallocate(buffer);

    ByteBuffer buffer2 = bufferManager.allocate(OTHER_CAPACITY);
    assertThat(buffer, not(sameInstance(buffer2)));

    ByteBuffer buffer3 = bufferManager.allocate(OTHER_CAPACITY);
    assertThat(buffer, not(sameInstance(buffer3)));
    assertThat(buffer2, not(sameInstance(buffer3)));

    bufferManager.deallocate(buffer2);
    ByteBuffer buffer2Reborn = bufferManager.allocate(OTHER_CAPACITY);
    assertThat(buffer2, is(sameInstance(buffer2Reborn)));
  }

  @Test
  public void capacity() throws Exception {
    assertCapacity(CAPACITY);
    assertCapacity(OTHER_CAPACITY);
  }

  private void assertCapacity(int capacity) {
    ByteBuffer buffer = bufferManager.allocate(capacity);
    try {
      assertThat(buffer.capacity(), is(capacity));
    } finally {
      bufferManager.deallocate(buffer);
    }
  }

}
