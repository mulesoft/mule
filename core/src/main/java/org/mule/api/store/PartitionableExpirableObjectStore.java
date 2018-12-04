/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.store;

import java.io.Serializable;

/**
 * A {@link PartitionableObjectStore} which is also an {@link ExpirableObjectStore}
 * <p/>
 * {@inheritDoc}
 *
 * @param <T> the generic type of the objects to be stored
 */
public interface PartitionableExpirableObjectStore<T extends Serializable>
    extends ExpirableObjectStore<T>, PartitionableObjectStore<T>
{

    /**
     * Expires eligible entries in the given {@code partitionName}. This method is required to be
     * thread safe and atomic, meaning that while running, all other methods must wait for it to finish.
     *
     * @param entryTTL      expire all entries which were inserted after this number of milliseconds. If lower or equal than zero,
     *                      no items will be expired on a TTL basis
     * @param maxEntries    The max number of entries that this store is allowed to have. If the store has more entries than this,
     *                      it will start removing entries until the boundary is met. The selection criteria is up to each implementation
     * @param partitionName the name of the partition to expire
     * @throws ObjectStoreException in case of failure
     */
    void expire(int entryTTL, int maxEntries, String partitionName) throws ObjectStoreException;
}
