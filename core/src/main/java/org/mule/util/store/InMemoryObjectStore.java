/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import org.mule.config.i18n.CoreMessages;

import java.util.Iterator;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentSkipListMap;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.helpers.Utils;

/**
 * <code>InMemoryObjectStore</code> implements an optionally bounded
 * in-memory store for message IDs with periodic expiry of old entries. The bounded size
 * is a <i>soft</i> limit and only enforced periodically by the expiry process; this
 * means that the store may temporarily exceed its maximum size between expiry runs, but
 * will eventually shrink to its configured size.
 */
public class InMemoryObjectStore extends AbstractMonitoredObjectStore
{
    protected ConcurrentSkipListMap/*<Long, StoredObject>*/ store;

    public InMemoryObjectStore()
    {
        this.store = new ConcurrentSkipListMap();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsObject(String id) throws Exception
    {
        if (id == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("id").toString());
        }

        synchronized (store)
        {
            return store.values().contains(new StoredObject(id, null));
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean storeObject(String id, Object item) throws Exception
    {
        if (id == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("id").toString());
        }

        // this block is unfortunately necessary to counter a possible race condition
        // between multiple nonatomic calls to containsObject/storeObject
        StoredObject obj = new StoredObject(id, item);
        synchronized (store)
        {
            if (store.values().contains(obj))
            {
                return false;
            }

            boolean written = false;
            while (!written)
            {
                Long key = Long.valueOf(Utils.nanoTime());
                written = (store.put(key, obj) == null);
            }

            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object retrieveObject(String id) throws Exception
    {
        synchronized (store)
        {
            Map.Entry<?, ?> entry = findEntry(id);
            if (entry != null)
            {
                StoredObject object = (StoredObject) entry.getValue();
                return object.getItem();
            }
        }
        return null;
    }

    public boolean removeObject(String id) throws Exception
    {
        synchronized (store)
        {
            Map.Entry<?, ?> entry = findEntry(id);
            if (entry != null)
            {
                Object removedObject = store.remove(entry.getKey());
                return (removedObject != null);
            }
        }
        return true;
    }
    
    private Map.Entry<?, ?> findEntry(String id)
    {
        Iterator<?> entryIterator = store.entrySet().iterator();
        while (entryIterator.hasNext())
        {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entryIterator.next();
            
            StoredObject object = (StoredObject) entry.getValue();
            if (object.getId().equals(id))
            {
                return entry;
            }
        }
        return null;
    }

    public final void expire()
    {
        // this is not guaranteed to be precise, but we don't mind
        int currentSize = store.size();
        
        // first trim to maxSize if necessary
        currentSize = trimToMaxSize(currentSize);

        // expire further if entry TTLs are enabled
        if ((entryTTL > 0) && (currentSize != 0))
        {
            final long now = Utils.nanoTime();
            int expiredEntries = 0;
            Map.Entry<?, ?> oldestEntry;

            purge:
            while ((oldestEntry = store.firstEntry()) != null)
            {
                Long oldestKey = (Long) oldestEntry.getKey();
                long oldestKeyValue = oldestKey.longValue();

                if (TimeUnit.NANOSECONDS.toMillis(now - oldestKeyValue) >= entryTTL)
                {
                    store.remove(oldestKey);
                    expiredEntries++;
                }
                else
                {
                    break purge;
                }
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Expired " + expiredEntries + " old entries");
            }
        }
    }

    private int trimToMaxSize(int currentSize)
    {
        if (maxEntries < 0)
        {
            return currentSize;
        }
        
        int excess = (currentSize - maxEntries);
        if (excess > 0)
        {
            while (currentSize > maxEntries)
            {
                store.pollFirstEntry();
                currentSize--;
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Expired " + excess + " excess entries");
            }
        }
        return currentSize;
    }

    /**
     * Represents the object stored in the store. This class holds the Object itslef and its ID.
     */
    protected static class StoredObject
    {
        private String id;
        private Object item;

        public StoredObject(String id, Object item)
        {
            this.id = id;
            this.item = item;
        }

        public String getId()
        {
            return id;
        }

        public Object getItem()
        {
            return item;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            StoredObject that = (StoredObject) o;

            if (!id.equals(that.id))
            {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            return id.hashCode();
        }

        @Override
        public String toString()
        {
            final StringBuffer sb = new StringBuffer();
            sb.append("StoredObject");
            sb.append("{id='").append(id).append('\'');
            sb.append(", item=").append(item);
            sb.append('}');
            return sb.toString();
        }
    }
}
