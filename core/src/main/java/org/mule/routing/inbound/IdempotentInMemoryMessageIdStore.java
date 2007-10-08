/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.inbound;

import org.mule.config.i18n.CoreMessages;
import org.mule.util.concurrent.DaemonThreadFactory;

import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentSkipListMap;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.helpers.Utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>IdempotentInMemoryMessageIdStore</code> implements an optionally bounded
 * in-memory store for message IDs with periodic expiry of old entries. The bounded size
 * is a <i>soft</i> limit and only enforced periodically by the expiry process; this
 * means that the store may temporarily exceed its maximum size between expiry runs, but
 * will eventually shrink to its configured size.
 */
public class IdempotentInMemoryMessageIdStore implements IdempotentMessageIdStore
{
    protected final Log logger = LogFactory.getLog(this.getClass());
    protected final ConcurrentSkipListMap store;
    protected final ScheduledThreadPoolExecutor scheduler;
    protected final int maxEntries;
    protected final int entryTTL;
    protected final int expirationInterval;

    /**
     * Default constructor for IdempotentInMemoryMessageIdStore.
     * 
     * @param name a name for this store, can be used for logging and identification
     *            purposes
     * @param maxEntries the maximum number of entries that this store keeps around.
     *            Specify <em>-1</em> if the store is supposed to be "unbounded".
     * @param entryTTL the time-to-live for each message ID, specified in seconds, or
     *            <em>-1</em> for entries that should never expire. <b>DO NOT</b>
     *            combine this with an unbounded store!
     * @param expirationInterval the interval for periodic bounded size enforcement and
     *            entry expiration, specified in seconds. Arbitrary positive values
     *            between 1 second and several hours or days are possible, but should be
     *            chosen carefully according to the expected message rate to prevent
     *            OutOfMemory conditions.
     * @see IdempotentReceiver#createMessageIdStore()
     * @throws {@link IllegalArgumentException} if non-positive values are specified for
     *             <code>expirationInterval</code>
     */
    public IdempotentInMemoryMessageIdStore(String name, int maxEntries, int entryTTL, int expirationInterval)
    {
        super();
        this.store = new ConcurrentSkipListMap();
        this.maxEntries = (maxEntries >= 0 ? maxEntries : Integer.MAX_VALUE);
        this.entryTTL = entryTTL;

        if (expirationInterval <= 0)
        {
            throw new IllegalArgumentException(CoreMessages.propertyHasInvalidValue("expirationInterval",
                new Integer(expirationInterval)).toString());
        }

        this.expirationInterval = expirationInterval;

        this.scheduler = new ScheduledThreadPoolExecutor(1);
        scheduler.setThreadFactory(new DaemonThreadFactory(name + "-IdempotentMessageIdStore"));
        scheduler.scheduleWithFixedDelay(new Expirer(), this.expirationInterval, this.expirationInterval,
            TimeUnit.SECONDS);
    }

    public boolean containsId(Object id) throws IllegalArgumentException, Exception
    {
        if (id == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("id").toString());
        }

        // this is a relaxed check so we don't need to synchronize on the store.
        return store.values().contains(id);
    }

    public boolean storeId(Object id) throws IllegalArgumentException, Exception
    {
        if (id == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("id").toString());
        }

        // this block is unfortunately necessary to counter a possible race condition
        // between multiple nonatomic calls to containsId/storeId, which are
        // only necessary because of the nonatomic calls to isMatch/process by
        // InboundRouterCollection.route().
        synchronized (store)
        {
            if (store.values().contains(id))
            {
                return false;
            }

            boolean written = false;
            while (!written)
            {
                written = (store.putIfAbsent(new Long(Utils.nanoTime()), id) == null);
            }

            return true;
        }
    }

    protected void expire()
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

            purge : while ((oldestEntry = store.firstEntry()) != null)
            {
                Long oldestKey = (Long) oldestEntry.getKey();
                long oldestKeyValue = oldestKey.longValue();

                if (TimeUnit.NANOSECONDS.toSeconds(now - oldestKeyValue) >= entryTTL)
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

    protected class Expirer implements Runnable
    {
        public void run()
        {
            try
            {
                // timed expiry MUST NOT throw anything..
                IdempotentInMemoryMessageIdStore.this.expire();
            }
            catch (Exception ex)
            {
                // ..but if it does, at least log the error
                logger.error(ex.getMessage(), ex);
            }
        }
    }

}
