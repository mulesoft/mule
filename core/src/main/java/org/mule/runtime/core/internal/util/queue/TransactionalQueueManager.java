/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.util.queue.DefaultQueueConfiguration;
import org.mule.runtime.core.api.util.queue.QueueConfiguration;
import org.mule.runtime.core.api.util.queue.QueueSession;
import org.mule.runtime.core.internal.util.journal.queue.LocalTxQueueTransactionJournal;
import org.mule.runtime.core.internal.util.journal.queue.LocalTxQueueTransactionRecoverer;
import org.mule.runtime.core.internal.util.journal.queue.XaTxQueueTransactionJournal;
import org.mule.runtime.core.internal.util.xa.XaTransactionRecoverer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * The Transactional Queue Manager is responsible for creating and Managing transactional Queues. Queues can also be persistent by
 * setting a persistence configuration for the queue.
 */
public class TransactionalQueueManager extends AbstractQueueManager {

  private LocalTxQueueTransactionJournal localTxTransactionJournal;
  private LocalTxQueueTransactionRecoverer localTxQueueTransactionRecoverer;
  private XaTxQueueTransactionJournal xaTransactionJournal;
  private XaTransactionRecoverer xaTransactionRecoverer;
  private QueueXaResourceManager queueXaResourceManager = new QueueXaResourceManager();
  // Due to current VMConnector and TransactionQueueManager relationship we must close all the recovered queues
  // since queue configuration is applied after recovery and not taking into consideration once queues are created
  // for recovery. See https://www.mulesoft.org/jira/browse/MULE-7420
  private Map<String, RecoverableQueueStore> queuesAccessedForRecovery = new HashMap<String, RecoverableQueueStore>();

  /**
   * {@inheritDoc}
   *
   * @return an instance of {@link TransactionalQueueSession}
   */
  @Override
  public synchronized QueueSession getQueueSession() {
    return new TransactionalQueueSession(this, queueXaResourceManager, queueXaResourceManager, xaTransactionRecoverer,
                                         localTxTransactionJournal, getMuleContext());
  }

  protected DefaultQueueStore createQueueStore(String name, QueueConfiguration config) {
    return new DefaultQueueStore(name, getMuleContext(), config);
  }

  @Override
  protected void doDispose() {
    if (localTxTransactionJournal != null) {
      localTxTransactionJournal.close();
    }
    if (xaTransactionJournal != null) {
      xaTransactionJournal.close();
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    String workingDirectory = getMuleContext().getConfiguration().getWorkingDirectory();
    int queueTransactionFilesSizeInMegabytes = getMuleContext().getConfiguration().getMaxQueueTransactionFilesSizeInMegabytes();
    localTxTransactionJournal = new LocalTxQueueTransactionJournal(workingDirectory + File.separator + "queue-tx-log",
                                                                   getMuleContext(), queueTransactionFilesSizeInMegabytes);
    localTxQueueTransactionRecoverer = new LocalTxQueueTransactionRecoverer(localTxTransactionJournal, this);
    xaTransactionJournal = new XaTxQueueTransactionJournal(workingDirectory + File.separator + "queue-xa-tx-log",
                                                           getMuleContext(), queueTransactionFilesSizeInMegabytes);
    xaTransactionRecoverer = new XaTransactionRecoverer(xaTransactionJournal, this);
  }

  @Override
  public RecoverableQueueStore getRecoveryQueue(String queueName) {
    if (queuesAccessedForRecovery.containsKey(queueName)) {
      return queuesAccessedForRecovery.get(queueName);
    }
    DefaultQueueStore queueStore = createQueueStore(queueName, new DefaultQueueConfiguration(0, true));
    queuesAccessedForRecovery.put(queueName, queueStore);
    return queueStore;
  }

  @Override
  public void start() throws MuleException {
    queueXaResourceManager.start();
    localTxQueueTransactionRecoverer.recover();
    for (QueueStore queueStore : queuesAccessedForRecovery.values()) {
      queueStore.close();
    }
    queuesAccessedForRecovery.clear();
  }

  @Override
  public void stop() throws MuleException {
    queueXaResourceManager.stop();
  }
}
