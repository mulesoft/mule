/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.internal.util.queue.QueueControlDataFile;
import org.mule.runtime.core.internal.util.queue.QueueFileProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class QueueControlDataFileTestCase extends AbstractMuleTestCase {

  private static final File DEFAULT_QUEUE_FILE = new File("default-queue-file-path");
  private static final String QUEUE_CONTROL_DATA_FILE_NAME = "queue-data";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void withNoQueueDataThenUseDefaultFile() {
    final QueueControlDataFile queueControlDataFile = createTestQueueDataControl();
    assertThat(queueControlDataFile.getCurrentWriteFile().getAbsolutePath(), is(DEFAULT_QUEUE_FILE.getAbsolutePath()));
    assertThat(queueControlDataFile.getCurrentReadFile().getAbsolutePath(), is(DEFAULT_QUEUE_FILE.getAbsolutePath()));
    queueControlDataFile.close();
  }

  @Test
  public void queueDataWithPrexistentControlData() {
    final QueueControlDataFile previousQueueControlDataFile = createTestQueueDataControl();
    final String writeFilePath = "test-write-file";
    final String readFilePath = "test-read-file";
    previousQueueControlDataFile.writeControlData(new File(writeFilePath), new File(readFilePath));
    previousQueueControlDataFile.close();

    final QueueControlDataFile newQueueControlDataFile = createTestQueueDataControl();
    assertThat(newQueueControlDataFile.getCurrentReadFile().getAbsolutePath(),
               is(previousQueueControlDataFile.getCurrentReadFile().getAbsolutePath()));
    assertThat(newQueueControlDataFile.getCurrentWriteFile().getAbsolutePath(),
               is(previousQueueControlDataFile.getCurrentWriteFile().getAbsolutePath()));
    newQueueControlDataFile.close();
  }

  @Test
  public void queueDataFileDoesNotGrow() {
    final QueueControlDataFile queueControlDataFile = createTestQueueDataControl();
    queueControlDataFile.writeControlData(DEFAULT_QUEUE_FILE, DEFAULT_QUEUE_FILE);
    long initalSize = getTestQueueDataControlFile().length();

    queueControlDataFile.writeControlData(DEFAULT_QUEUE_FILE, DEFAULT_QUEUE_FILE);
    assertThat(getTestQueueDataControlFile().length(), is(initalSize));
  }

  private QueueControlDataFile createTestQueueDataControl() {
    return new QueueControlDataFile(new QueueFileProvider(getTestQueueDataControlFile().getParentFile(),
                                                          getTestQueueDataControlFile().getName()),
                                    DEFAULT_QUEUE_FILE, DEFAULT_QUEUE_FILE);
  }

  private File getTestQueueDataControlFile() {
    return new File(temporaryFolder.getRoot(), QUEUE_CONTROL_DATA_FILE_NAME);
  }
}
