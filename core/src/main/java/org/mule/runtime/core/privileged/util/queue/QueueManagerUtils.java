/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.util.queue;

import org.mule.runtime.core.api.util.FileUtils;
import org.mule.runtime.core.internal.util.queue.DualRandomAccessFileQueueStoreDelegate;

import java.io.File;
import java.io.IOException;

/**
 * Provides utilities to work with queues for testing purposes without having to export all the classes in
 * {@code org.mule.runtime.core.internal.util.queue} package. For testing purposes only.
 *
 * @since 4.0
 */
public class QueueManagerUtils {

  private QueueManagerUtils() {}

  /**
   * Creates a file corresponding to the fist enqueued object in a given queue
   *
   * @param workingFolder container's working folder.
   * @param queueName     name of the queue where the object is added.
   * @return the file corresponding to the enqueued object
   * @throws IOException if there is any problem creating the file.
   */
  public static File getFirstQueueFileForTesting(File workingFolder, String queueName) throws IOException {
    File firstQueueFile = DualRandomAccessFileQueueStoreDelegate
        .getFirstQueueFileForTesting(queueName, workingFolder.getAbsolutePath());
    return FileUtils.createFile(firstQueueFile.getAbsolutePath());
  }
}
