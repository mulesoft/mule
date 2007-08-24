/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.queue;

public class FilePersistenceTestCase extends AbstractTransactionQueueManagerTestCase
{

    protected TransactionalQueueManager createQueueManager() throws Exception
    {
        TransactionalQueueManager mgr = new TransactionalQueueManager();
        mgr.setPersistenceStrategy(new FilePersistenceStrategy());
        mgr.setDefaultQueueConfiguration(new QueueConfiguration(true));
        return mgr;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.queue.AbstractTransactionQueueManagerTestCase#isPersistent()
     */
    protected boolean isPersistent()
    {
        return true;
    }

}
