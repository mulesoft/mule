/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.queue;

import org.mule.util.store.QueueStoreAdapter;
import org.mule.util.store.SimpleMemoryObjectStore;

import java.io.Serializable;

public class NoPersistenceTestCase extends AbstractTransactionQueueManagerTestCase
{
    @Override
    protected TransactionalQueueManager createQueueManager() throws Exception
    {
        TransactionalQueueManager mgr = new TransactionalQueueManager();
        mgr.setDefaultQueueConfiguration(new QueueConfiguration(0, new QueueStoreAdapter<Serializable>(new SimpleMemoryObjectStore<Serializable>())));
        return mgr;
    }

    @Override
    protected boolean isPersistent()
    {
        return false;
    }
}
