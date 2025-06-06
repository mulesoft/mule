/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.util.queue.DefaultQueueConfiguration;
import org.mule.runtime.core.internal.serialization.JavaObjectSerializer;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class FilePersistenceTestCase extends AbstractTransactionQueueManagerTestCase {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Override
  protected TransactionalQueueManager createQueueManager() throws Exception {
    TransactionalQueueManager mgr = new TransactionalQueueManager();
    MuleConfiguration mockConfiguration = mock(MuleConfiguration.class);
    when(mockConfiguration.getWorkingDirectory()).thenReturn(temporaryFolder.getRoot().getAbsolutePath());
    when(mockConfiguration.getMaxQueueTransactionFilesSizeInMegabytes()).thenReturn(100);

    mgr.setMuleConfiguration(mockConfiguration);
    mgr.setObjectSerializer(new JavaObjectSerializer(this.getClass().getClassLoader()));
    mgr.initialise();
    mgr.setDefaultQueueConfiguration(new DefaultQueueConfiguration(0, true));
    return mgr;
  }

  @Override
  protected boolean isPersistent() {
    return true;
  }
}
