/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.queue;

public class NoPersistenceTestCase extends AbstractTransactionQueueManagerTestCase
{
    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.queue.AbstractTransactionQueueManagerTestCase#createQueueManager()
     */
    protected TransactionalQueueManager createQueueManager() throws Exception
    {
        TransactionalQueueManager mgr = new TransactionalQueueManager();
        mgr.setPersistenceStrategy(new MemoryPersistenceStrategy());
        return mgr;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.queue.AbstractTransactionQueueManagerTestCase#isPersistent()
     */
    protected boolean isPersistent()
    {
        return false;
    }

}
