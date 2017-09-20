/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.tck.SerializationTestUtils.addJavaSerializerToMockMuleContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.util.queue.DualRandomAccessFileQueueStoreDelegate;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class DualRandomAccessFileQueueStoreDelegateTestCase extends AbstractMuleTestCase {

  private static final int MAXIMUM_NUMBER_OF_BYTES = 100;
  public static final String TEST_QUEUE_NAME = "test-queue";

  @Rule
  public TemporaryFolder workingDirectory = new TemporaryFolder();

  private MuleContext mockMuleContext;

  @Before
  public void before() {
    mockMuleContext = mock(MuleContext.class);
    when(mockMuleContext.getExecutionClassLoader()).thenReturn(getClass().getClassLoader());
    addJavaSerializerToMockMuleContext(mockMuleContext);
  }

  @Test
  public void nameWithInvalidCharacters() throws IOException {
    String[] testNames = new String[] {"test-test", "test:/test", "test?test", "test:\\test", "test:/test", "test&test",
        "test|test", "seda.queue(post:\\Customer:ApiTest-config.1)",
        "this$is%a#really/big\\name@that?has<a>lot*of+invalid^characters!this$is%a#really/big\\name@that?has<a>lot*of+invalid^chars!"};

    for (String testName : testNames) {
      createAndDisposeQueue(testName);
    }
  }

  @Test
  @Ignore("MULE-13581")
  public void readQueueFileMessagesInOrder() throws Exception {
    MuleTestUtils.testWithSystemProperty(DualRandomAccessFileQueueStoreDelegate.MAX_LENGTH_PER_FILE_PROPERTY_KEY,
                                         String.valueOf(MAXIMUM_NUMBER_OF_BYTES), new MuleTestUtils.TestCallback() {

                                           @Override
                                           public void run() throws Exception {
                                             int lastInsertedMessageIndex = writeDataUntilSecondFileContainsNextMessages();
                                             verifyNextMessage(lastInsertedMessageIndex);
                                           }
                                         });
  }

  @Test
  @Ignore("MULE-13581")
  public void readQueueFileMessagesInOrderWhenControlFileIsCorrupted() throws Exception {
    MuleTestUtils.testWithSystemProperty(DualRandomAccessFileQueueStoreDelegate.MAX_LENGTH_PER_FILE_PROPERTY_KEY,
                                         String.valueOf(MAXIMUM_NUMBER_OF_BYTES), new MuleTestUtils.TestCallback() {

                                           @Override
                                           public void run() throws Exception {
                                             int lastInsertedMessageIndex = writeDataUntilSecondFileContainsNextMessages();
                                             corruptQueueControlData();
                                             verifyNextMessage(lastInsertedMessageIndex);
                                           }
                                         });
  }

  @Test
  public void allFilesDeletedAfterDispose() {
    DualRandomAccessFileQueueStoreDelegate queueStore = createTestQueueStore();
    queueStore.add("item");

    File queueFolder = new File(workingDirectory.getRoot().getAbsolutePath() + "/queuestore");
    assertThat(queueFolder.exists(), is(true));
    assertThat(queueFilesExist(queueFolder), is(true));

    queueStore.dispose();
    assertThat(queueFilesExist(queueFolder), is(false));
  }

  private boolean queueFilesExist(File queueFolder) {
    for (File file : queueFolder.listFiles()) {
      if (file.getName().contains(TEST_QUEUE_NAME)) {
        return true;
      }
    }
    return false;
  }


  private void corruptQueueControlData() throws IOException {
    final DualRandomAccessFileQueueStoreDelegate queueStore = createTestQueueStore();
    final RandomAccessFile randomAccessFile = queueStore.getQueueControlDataFile().getQueueFileProvider().getRandomAccessFile();
    randomAccessFile.seek(0);
    randomAccessFile.write(2000);
    queueStore.close();
  }

  private void verifyNextMessage(int lastInsertedMessageIndex) throws InterruptedException {
    DualRandomAccessFileQueueStoreDelegate queueStore = createTestQueueStore();
    assertThat((String) queueStore.removeFirst(), Is.is(createTestDataForIndex(lastInsertedMessageIndex)));
    queueStore.close();
  }

  private int writeDataUntilSecondFileContainsNextMessages() throws InterruptedException {
    DualRandomAccessFileQueueStoreDelegate queueStore = createTestQueueStore();
    final File initialReadFile = queueStore.getQueueControlDataFile().getCurrentReadFile();
    int numberOfMessagesCreated = 0;
    do {
      queueStore.add(createTestDataForIndex(numberOfMessagesCreated));
      numberOfMessagesCreated++;
    } while (queueStore.getQueueControlDataFile().getCurrentWriteFile().getAbsolutePath()
        .equals(initialReadFile.getAbsolutePath()));
    int lastInsertedMessageIndex = numberOfMessagesCreated - 1;
    for (int i = 0; i < lastInsertedMessageIndex; i++) {
      queueStore.removeFirst();
    }
    // this call updates the read file.
    queueStore.peek();
    assertThat(queueStore.getQueueControlDataFile().getCurrentReadFile().getAbsolutePath(),
               not(initialReadFile.getAbsolutePath()));
    assertThat(queueStore.getQueueControlDataFile().getCurrentWriteFile().getAbsolutePath(),
               not(initialReadFile.getAbsolutePath()));
    queueStore.close();
    return lastInsertedMessageIndex;
  }

  private void createAndDisposeQueue(String queueName) throws IOException {
    DualRandomAccessFileQueueStoreDelegate queue =
        new DualRandomAccessFileQueueStoreDelegate(queueName, workingDirectory.getRoot().getAbsolutePath(), mockMuleContext, 1);
    queue.dispose();
  }

  private String createTestDataForIndex(int numberOfMesagesCreated) {
    return "some value " + numberOfMesagesCreated;
  }

  private DualRandomAccessFileQueueStoreDelegate createTestQueueStore() {
    return new DualRandomAccessFileQueueStoreDelegate(TEST_QUEUE_NAME, workingDirectory.getRoot().getAbsolutePath(),
                                                      mockMuleContext, 0);
  }

}
