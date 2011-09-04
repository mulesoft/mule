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

import org.mule.api.store.ObjectStoreException;

import java.io.Serializable;

/**
 * A QueueInfoDelegate that can take objects directly from its store
 */
public interface TakingQueueInfoDelegate extends QueueInfoDelegate
{
    /**
     * Poll the queue for its first member, and, if there is one, remove and return the corresponding object
     * from the object store
     */
    Serializable takeFromObjectStore(long timeout) throws InterruptedException;

    void writeToObjectStore(Serializable data) throws InterruptedException,ObjectStoreException;
}
