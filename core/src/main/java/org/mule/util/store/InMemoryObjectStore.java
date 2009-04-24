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

import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentSkipListMap;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.helpers.Utils;

/**
 * <code>InMemoryObjectStore</code> implements an optionally bounded
 * in-memory store for message IDs with periodic expiry of old entries. The bounded size
 * is a <i>soft</i> limit and only enforced periodically by the expiry process; this
 * means that the store may temporarily exceed its maximum size between expiry runs, but
 * will eventually shrink to its configured size.DO
 */
public class InMemoryObjectStore extends AbstractMonitoredObjectStore
{
    protected ConcurrentSkipListMap store;

    public InMemoryObjectStore()
    {
        this.store = new ConcurrentSkipListMap();
    }

    /**
     * Check whether the given Object is already registered with this store.
     *
     * @param id the ID to check
     * @return <code>true</code> if the ID is stored or <code>false</code> if it could
     *         not be found
     * @throws IllegalArgumentException if the given ID is <code>null</code>
     * @throws Exception                if any implementation-specific error occured, e.g. when the store
     *                                  is not available
     */
    public boolean containsObject(String id) throws Exception
    {
        if (id == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("id").toString());
        }

        // this is a relaxed check so we don't need to synchronize on the store.
        return store.values().contains(new StoredObject(id, null));
    }

    /**
     * Store the given Object.
     *
     * @param id the ID to store
     * @return <code>true</code> if the ID was stored properly, or <code>false</code>
     *         if it already existed
     * @throws IllegalArgumentException if the given ID cannot be stored or is
     *                                  <code>null</code>
     * @throws Exception                if the store is not available or any other
     *                                  implementation-specific error occured
     */
    public boolean storeObject(String id, Object item) throws Exception
    {
        if (id == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("id").toString());
        }

        // this block is unfortunately necessary to counter a possible race condition
        // between multiple nonatomic calls to containsId/storeId, which are
        // only necessary because of the nonatomic calls to isMatch/process by
        // DefaultInboundRouterCollection.route().
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
                written = (store.putIfAbsent(new Long(Utils.nanoTime()), obj) == null);
            }

            return true;
        }
    }

    /**
     * Retrieve the given Object.
     *
     * @param id the ID to store
     * @return the object instance associated with this id or null if there was no entry for the supplied id.
     * @throws IllegalArgumentException if the given ID cannot be stored or is
     *                                  <code>null</code>
     * @throws Exception                if the store is not available or any other
     *                                  implementation-specific error occured
     */
    public Object retrieveObject(String id) throws Exception
    {
        StoredObject obj = (StoredObject)store.get(id);
        if(obj!=null)
        {
            return obj.getItem();
        }
        return null;
    }

    public boolean removeObject(String id) throws Exception
    {
        StoredObject obj = (StoredObject)store.get(id);
        if(obj!=null)
        {
            return store.remove(obj) !=null;
        }
        return true;
    }

    public final void expire()
    {
        // this is not guaranteed to be precise, but we don't mind
        int currentSize = store.size();

        // first trim to maxSize if necessary
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

        // expire further if entry TTLs are enabled
        if (entryTTL > 0 && currentSize != 0)
        {
            final long now = Utils.nanoTime();
            int expiredEntries = 0;
            Map.Entry oldestEntry;

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

        public int hashCode()
        {
            return id.hashCode();
        }


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
