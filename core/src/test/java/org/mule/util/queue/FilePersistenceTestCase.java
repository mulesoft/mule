/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.queue;

import org.mule.api.store.QueueStore;
import org.mule.util.store.QueueStoreAdapter;
import org.mule.util.store.QueuePersistenceObjectStore;

import java.io.Serializable;

public class FilePersistenceTestCase extends AbstractTransactionQueueManagerTestCase
{
    @Override
    protected TransactionalQueueManager createQueueManager() throws Exception
    {
        QueueStore<Serializable> store = new QueueStoreAdapter<Serializable>(new QueuePersistenceObjectStore<Serializable>(muleContext));

        TransactionalQueueManager mgr = new TransactionalQueueManager();

        mgr.setDefaultQueueConfiguration(new QueueConfiguration(0, store));
        return mgr;
    }

    @Override
    protected boolean isPersistent()
    {
        return true;
    }
}
