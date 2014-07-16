/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.queue;

import org.mule.util.xa.ResourceManagerException;

/**
 * XA Transaction context for queues
 */
public interface XaQueueTransactionContext extends QueueTransactionContext
{

    /**
     * Commits the current transaction context operations
     *
     * @throws ResourceManagerException
     */
    public void doCommit() throws ResourceManagerException;

    /**
     * Rollbacks the current transaction context operations
     *
     * @throws ResourceManagerException
     */
    public void doRollback() throws ResourceManagerException;

    /**
     * Prepares the current transaction context for a commit
     *
     * @throws ResourceManagerException
     */
    public void doPrepare() throws ResourceManagerException;

}
