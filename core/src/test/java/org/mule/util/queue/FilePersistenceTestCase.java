/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
