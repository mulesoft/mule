/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.streaming;

import org.mule.api.Closeable;

import java.util.NoSuchElementException;

/**
 * General interface for components able to consume data from any specific resource
 * or stream, following the Producer-Consumer design pattern. Implementing this
 * interface does not guarantee thread safeness. Check each particular implementation
 * for information about that
 */
public interface Consumer<T> extends Closeable
{

    /**
     * Retrieves the next available item.
     * 
     * @return an object of type T if available
     * @throws {@link org.mule.api.streaming.ClosedConsumerException.ClosedConsumerException}
     *         if the consumer is already closed
     */
    public T consume() throws NoSuchElementException;

    /**
     * Returns <code>true</code> if no more items are available or if the consumer
     * was closed. When this method returns <code>true</code>, implementors of this
     * class are require to invoke the {@link org.mule.api.Closeable.close()} method
     * before returning in order to release resources as quickly as possible. Users
     * of this component who no longer need this require before it is fully consumed
     * are also required to close it.
     * 
     * @return <code>true</code> if no more items are available. <code>false</code>
     *         otherwise
     */
    public boolean isConsumed();

    /**
     * returns the total amount of items available for consumption. In some
     * scenarios, it might not be possible/convenient to actually retrieve this value
     * or it might not be available at this point. -1 is returned in such a case.
     */
    public int totalAvailable();

}
