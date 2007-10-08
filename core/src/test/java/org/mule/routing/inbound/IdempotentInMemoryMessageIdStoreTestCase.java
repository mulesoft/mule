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

import org.mule.tck.AbstractMuleTestCase;

public class IdempotentInMemoryMessageIdStoreTestCase extends AbstractMuleTestCase
{

    public void testTimedExpiry() throws Exception
    {
        // entryTTL=3 and expiryInterval=1 will cause background expiry
        IdempotentInMemoryMessageIdStore store = new IdempotentInMemoryMessageIdStore("timed", 2, 3, 1);

        // store entries in quick succession
        assertTrue(store.storeId("1"));
        assertTrue(store.storeId("2"));
        assertTrue(store.storeId("3"));

        // they should still be alive at this point
        assertTrue(store.containsId("1"));
        assertTrue(store.containsId("2"));
        assertTrue(store.containsId("3"));

        // wait until the entry TTL has been exceeded
        Thread.sleep(4000);

        // make sure all values are gone
        assertFalse(store.containsId("1"));
        assertFalse(store.containsId("2"));
        assertFalse(store.containsId("3"));
    }

    public void testMaxSize() throws Exception
    {
        // entryTTL=-1 means we will have to expire manually
        IdempotentInMemoryMessageIdStore store = new IdempotentInMemoryMessageIdStore("bounded", 3, -1, 1);

        assertTrue(store.storeId("1"));
        assertTrue(store.storeId("2"));
        assertTrue(store.storeId("3"));

        assertTrue(store.containsId("1"));
        assertTrue(store.containsId("2"));
        assertTrue(store.containsId("3"));

        // sleep a bit to make sure that entries are not expired, even though the expiry
        // thread is running every second
        Thread.sleep(3000);
        assertTrue(store.containsId("1"));
        assertTrue(store.containsId("2"));
        assertTrue(store.containsId("3"));

        // exceed threshold
        assertTrue(store.storeId("4"));

        // the oldest entry should still be there
        assertTrue(store.containsId("1"));

        // expire manually
        store.expire();
        assertFalse(store.containsId("1"));
        assertTrue(store.containsId("2"));
        assertTrue(store.containsId("3"));
        assertTrue(store.containsId("4"));

        // exceed some more
        assertTrue(store.storeId("5"));
        store.expire();
        assertFalse(store.containsId("2"));
        assertTrue(store.containsId("3"));
        assertTrue(store.containsId("4"));
        assertTrue(store.containsId("5"));

        // and multiple times
        assertTrue(store.storeId("6"));
        assertTrue(store.storeId("7"));
        assertTrue(store.storeId("8"));
        assertTrue(store.storeId("9"));
        store.expire();
        assertTrue(store.containsId("7"));
        assertTrue(store.containsId("8"));
        assertTrue(store.containsId("9"));
        assertFalse(store.containsId("3"));
        assertFalse(store.containsId("4"));
        assertFalse(store.containsId("5"));
        assertFalse(store.containsId("6"));
    }

}
