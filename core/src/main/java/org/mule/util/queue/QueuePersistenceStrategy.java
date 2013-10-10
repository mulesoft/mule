/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.queue;

import java.io.IOException;
import java.util.List;

/**
 * <code>QueuePersistenceStrategy</code> defines the The api to a persistent queue
 * store. A persistence strategy can be transient (in memory or non-restorable) or
 * non-transient such as File system or DB.
 */
public interface QueuePersistenceStrategy
{

    public interface Holder
    {
        Object getId();

        String getQueue();
    }


    /**
     * Stores an object and returns its generated id.
     * 
     * @param obj the object to be stored
     * @return the id of the stored object
     * @throws IOException
     */
    Object store(String queue, Object obj) throws IOException;

    /**
     * Loads an object specified by the given id.
     * 
     * @param id the id of the stored object
     * @return the object
     * @throws IOException
     */
    Object load(String queue, Object id) throws IOException;

    /**
     * Removes the object specified by the given id from the store.
     * 
     * @param id the id of the stored object
     * @throws IOException
     */
    void remove(String queue, Object id) throws IOException;

    /**
     * Retrieves the ids of the stored objects.
     * 
     * @return the list of ids
     * @throws IOException
     */
    List restore() throws IOException;

    /**
     * Open the store.
     * 
     * @throws IOException
     */
    void open() throws IOException;

    /**
     * Closes the store.
     * 
     * @throws IOException
     */
    void close() throws IOException;

    boolean isTransient();

}
