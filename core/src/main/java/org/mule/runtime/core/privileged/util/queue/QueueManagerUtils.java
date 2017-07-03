/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
   * @param queueName name of the queue where the object is added.
   * @return the file corresponding to the enqueued object
   * @throws IOException if there is any problem creating the file.
   */
  public static File getFirstQueueFileForTesting(File workingFolder, String queueName) throws IOException {
    File firstQueueFile = DualRandomAccessFileQueueStoreDelegate
        .getFirstQueueFileForTesting(queueName, workingFolder.getAbsolutePath());
    return FileUtils.createFile(firstQueueFile.getAbsolutePath());
  }
}
