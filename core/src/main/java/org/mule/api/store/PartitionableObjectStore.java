/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.store;

import java.io.Serializable;
import java.util.List;

public interface PartitionableObjectStore<T extends Serializable> extends ListableObjectStore<T>
{
    boolean contains(Serializable key, String partitionName) throws ObjectStoreException;

    void store(Serializable key, T value, String partitionName) throws ObjectStoreException;

    T retrieve(Serializable key, String partitionName) throws ObjectStoreException;

    T remove(Serializable key, String partitionName) throws ObjectStoreException;

    List<Serializable> allKeys(String partitionName) throws ObjectStoreException;

    List<String> allPartitions() throws ObjectStoreException;

    void open(String partitionName) throws ObjectStoreException;

    void close(String partitionName) throws ObjectStoreException;

    void disposePartition(String partitionName) throws ObjectStoreException;
}
