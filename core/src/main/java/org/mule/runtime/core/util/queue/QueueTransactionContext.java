/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.queue;

import java.io.Serializable;

/**
 * Contract for a transactional context for queue operations.
 */
public interface QueueTransactionContext
{

    public boolean offer(QueueStore queue, Serializable item, long offerTimeout)
            throws InterruptedException;

    public void untake(QueueStore queue, Serializable item) throws InterruptedException;

    public void clear(QueueStore queue) throws InterruptedException;

    public Serializable poll(QueueStore queue, long pollTimeout)
            throws InterruptedException;

    public Serializable peek(QueueStore queue) throws InterruptedException;

    public int size(QueueStore queue);

}
