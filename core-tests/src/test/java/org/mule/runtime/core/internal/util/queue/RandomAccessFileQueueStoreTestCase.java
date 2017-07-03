/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.internal.util.queue.QueueFileProvider;
import org.mule.runtime.core.internal.util.queue.RandomAccessFileQueueStore;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class RandomAccessFileQueueStoreTestCase extends AbstractMuleTestCase {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void getLengthWithNoFileContent() {
    final RandomAccessFileQueueStore randomAccessFileQueueStore = createRandomAccessFileQueueStore();
    assertThat(randomAccessFileQueueStore.getLength(), is(0l));
  }

  @Test
  public void getLengthWithOneElement() {
    final RandomAccessFileQueueStore randomAccessFileQueueStore = createRandomAccessFileQueueStore();
    final long dataSize = 10;
    randomAccessFileQueueStore.addLast(new byte[(int) dataSize]);
    assertThat(randomAccessFileQueueStore.getLength(), is(dataSize + RandomAccessFileQueueStore.CONTROL_DATA_SIZE));
  }

  @Test
  public void getLengthWithSeveralElements() {
    final RandomAccessFileQueueStore randomAccessFileQueueStore = createRandomAccessFileQueueStore();
    final long dataSize = 10;
    final byte[] data = new byte[(int) dataSize];
    final int numberOfElements = 10;
    for (int i = 0; i < numberOfElements; i++) {
      randomAccessFileQueueStore.addLast(data);
    }
    assertThat(randomAccessFileQueueStore.getLength(),
               is((dataSize + RandomAccessFileQueueStore.CONTROL_DATA_SIZE) * numberOfElements));
  }

  private RandomAccessFileQueueStore createRandomAccessFileQueueStore() {
    return new RandomAccessFileQueueStore(new QueueFileProvider(temporaryFolder.getRoot(), "datafile"));
  }
}
