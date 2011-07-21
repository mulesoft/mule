/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.store;

import java.io.Serializable;
import java.util.List;

public interface PartitionableObjectStore<T extends Serializable> extends ListableObjectStore<T>
{
    public boolean contains(Serializable key, String partitionName) throws ObjectStoreException;

    public void store(Serializable key, T value, String partitionName) throws ObjectStoreException;

    public T retrieve(Serializable key, String partitionName) throws ObjectStoreException;

    public T remove(Serializable key, String partitionName) throws ObjectStoreException;

    public List<Serializable> allKeys(String partitionName) throws ObjectStoreException;

    public List<String> allPartitions() throws ObjectStoreException;

    public void open(String partitionName) throws ObjectStoreException;

    public void close(String partitionName) throws ObjectStoreException;
    
    public void disposePartition(String partitionName) throws ObjectStoreException;
}
