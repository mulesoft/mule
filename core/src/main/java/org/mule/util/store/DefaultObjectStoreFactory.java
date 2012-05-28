/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import org.mule.api.store.ObjectStore;
import org.mule.api.store.QueueObjectStore;

import java.io.Serializable;

/**
 * Create the default object stores
 */
public interface DefaultObjectStoreFactory
{
    ObjectStore<Serializable> createDefaultInMemoryObjectStore();

    ObjectStore<Serializable> createDefaultPersistentObjectStore();

    QueueObjectStore<Serializable> createDefaultInMemoryQueueStore();

    QueueObjectStore<Serializable> createDefaultPersistentQueueStore();

    ObjectStore<Serializable> createDefaultUserObjectStore();
}
