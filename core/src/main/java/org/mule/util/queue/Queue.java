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

/**
 * <code>Queue</code> TODO
 */
public interface Queue
{

    /**
     * Returns the number of elements in this queue.
     * 
     * @return
     */
    int size();

    /**
     * Puts a new object in this queue and wait if necessary.
     * 
     * @param o the object to put
     */
    void put(Object o) throws InterruptedException;

    /**
     * Blocks and retrieves an object from this queue.
     * 
     * @return an object.
     */
    Object take() throws InterruptedException;

    Object peek() throws InterruptedException;

    Object poll(long timeout) throws InterruptedException;

    boolean offer(Object o, long timeout) throws InterruptedException;

}
