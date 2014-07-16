/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import static org.mule.api.store.ObjectStoreManager.UNBOUNDED;

import org.mule.api.store.ObjectAlreadyExistsException;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.PartitionableExpirableObjectStore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

public class PartitionedInMemoryObjectStore<T extends Serializable> extends AbstractPartitionedObjectStore<T>
    implements PartitionableExpirableObjectStore<T>
{
    private ConcurrentMap<String, ConcurrentMap<Serializable, T>> partitions = new ConcurrentHashMap<String, ConcurrentMap<Serializable, T>>();
    private ConcurrentMap<String, ConcurrentSkipListMap<Long, Serializable>> expiryInfoPartition = new ConcurrentHashMap<String, ConcurrentSkipListMap<Long, Serializable>>();

    @Override
    public boolean isPersistent()
    {
        return false;
    }

    @Override
    public boolean contains(Serializable key, String partitionName) throws ObjectStoreException
    {
        if (partitions.containsKey(partitionName))
        {
            return partitions.get(partitionName).containsKey(key);
        }
        else
        {
            return false;
        }
    }

    @Override
    public void store(Serializable key, T value, String partitionName) throws ObjectStoreException
    {
        T oldValue = getPartition(partitionName).putIfAbsent(key, value);
        if (oldValue != null)
        {
            throw new ObjectAlreadyExistsException();
        }
        getExpirtyInfoPartition(partitionName).put(Long.valueOf(System.nanoTime()), key);
    }

    @Override
    public T retrieve(Serializable key, String partitionName) throws ObjectStoreException
    {
        T value = getPartition(partitionName).get(key);
        if (value == null)
        {
            throw new ObjectDoesNotExistException();
        }
        return value;
    }

    @Override
    public T remove(Serializable key, String partitionName) throws ObjectStoreException
    {
        T removedValue = getPartition(partitionName).remove(key);
        if (removedValue == null)
        {
            throw new ObjectDoesNotExistException();
        }

        // TODO possibly have a reverse map to make this more efficient
        Iterator<Map.Entry<Long, Serializable>> localIterator = getExpirtyInfoPartition(partitionName).entrySet()
            .iterator();
        Map.Entry<Long, Serializable> localEntry;
        Long timestamp = null;
        while (localIterator.hasNext())
        {
            localEntry = localIterator.next();
            if (key.equals(localEntry.getValue()))
            {
                timestamp = localEntry.getKey();
                break;
            }
        }
        getExpirtyInfoPartition(partitionName).remove(timestamp);
        return removedValue;
    }

    @Override
    public List<Serializable> allKeys(String partitionName) throws ObjectStoreException
    {
        return new ArrayList<Serializable>(getPartition(partitionName).keySet());
    }
    
    @Override
    public void clear(String partitionName) throws ObjectStoreException
    {
        this.getPartition(partitionName).clear();
    }

    @Override
    public List<String> allPartitions() throws ObjectStoreException
    {
        return new ArrayList<String>(partitions.keySet());
    }

    private ConcurrentMap<Serializable, T> getPartition(String partitionName)
    {
        ConcurrentMap<Serializable, T> partition = partitions.get(partitionName);
        if (partition == null)
        {
            partition = new ConcurrentHashMap<Serializable, T>();
            ConcurrentMap<Serializable, T> previous = partitions.putIfAbsent(partitionName, partition);
            if (previous != null)
            {
                partition = previous;
            }
        }
        return partition;
    }

    private ConcurrentSkipListMap<Long, Serializable> getExpirtyInfoPartition(String partitionName)
    {
        ConcurrentSkipListMap<Long, Serializable> partition = expiryInfoPartition.get(partitionName);
        if (partition == null)
        {
            partition = new ConcurrentSkipListMap<Long, Serializable>();
            ConcurrentSkipListMap<Long, Serializable> previous = expiryInfoPartition.putIfAbsent(
                partitionName, partition);
            if (previous != null)
            {
                partition = previous;
            }
        }
        return partition;
    }

    @Override
    public void open(String partitionName) throws ObjectStoreException
    {
        // Nothing to do
    }

    @Override
    public void close(String partitionName) throws ObjectStoreException
    {
        // Nothing to do
    }

    @Override
    public void expire(int entryTTL, int maxEntries) throws ObjectStoreException
    {
        expire(entryTTL, maxEntries, DEFAULT_PARTITION);
    }

    @Override
    public void expire(int entryTTL, int maxEntries, String partitionName) throws ObjectStoreException
    {
        final long now = System.nanoTime();
        int expiredEntries = 0;
        Map.Entry<Long, Serializable> oldestEntry;
        ConcurrentSkipListMap<Long, Serializable> store = getExpirtyInfoPartition(partitionName);
        ConcurrentMap<Serializable, T> partition = getPartition(partitionName);

        trimToMaxSize(store, maxEntries, partition);

        if (entryTTL == UNBOUNDED)
        {
            return;
        }

        while ((oldestEntry = store.firstEntry()) != null)
        {
            Long oldestKey = oldestEntry.getKey();
            long oldestKeyValue = oldestKey.longValue();

            if (TimeUnit.NANOSECONDS.toMillis(now - oldestKeyValue) >= entryTTL)
            {
                partition.remove(oldestEntry.getValue());
                store.remove(oldestKey);
                expiredEntries++;
            }
            else
            {
                break;
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Expired " + expiredEntries + " old entries");
        }
    }

    private void trimToMaxSize(ConcurrentSkipListMap<Long, Serializable> store,
                               int maxEntries,
                               ConcurrentMap<Serializable, T> partition)
    {
        if (maxEntries == UNBOUNDED)
        {
            return;
        }

        int currentSize = store.size();
        int excess = (currentSize - maxEntries);
        if (excess > 0)
        {
            while (currentSize > maxEntries)
            {
                Entry<Long, Serializable> toRemove = store.pollFirstEntry();
                partition.remove(toRemove.getValue());
                currentSize--;
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Expired " + excess + " excess entries");
            }
        }
    }

    @Override
    public void disposePartition(String partitionName) throws ObjectStoreException
    {
        removeAndClear(partitions, partitionName);
        removeAndClear(expiryInfoPartition, partitionName);
    }

    private void removeAndClear(Map<String, ? extends Map> map, String key)
    {
        Map partition = map.remove(key);
        if(partition!=null)
        {
            partition.clear();
        }
    }

}
