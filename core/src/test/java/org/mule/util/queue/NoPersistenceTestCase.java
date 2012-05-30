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
