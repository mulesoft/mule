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

import java.io.Serializable;

/**
 * <code>Queue</code> TODO
 */
public interface Queue
{
    /**
     * Returns the number of elements in this queue.
     */
    int size();

    /**
     * Puts a new object in this queue and wait if necessary.
     */
    void put(Serializable object) throws InterruptedException;

    /**
     * Blocks and retrieves an object from this queue.
     *
     * @return an object.
     */
    Serializable take() throws InterruptedException;

    void untake(Serializable item) throws InterruptedException;

    Serializable peek() throws InterruptedException;

    Serializable poll(long timeout) throws InterruptedException;

    boolean offer(Serializable object, long timeout) throws InterruptedException;

    String getName();
}
