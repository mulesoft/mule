/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.FileUtils;

import java.io.File;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TextFileStoreTestCase extends AbstractMuleContextTestCase
{
    public static final String DIR = ".mule/temp";
    TextFileObjectStore store;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        FileUtils.deleteTree(new File(DIR));
    }

    @Override
    protected void doTearDown() throws Exception
    {
        if (store != null)
        {
            store.dispose();
        }
        FileUtils.deleteTree(new File(DIR));
        super.doTearDown();
    }

    @Test
    public void testTimedExpiry() throws Exception
    {
        // entryTTL=3 and expiryInterval=1 will cause background expiry
        store = new TextFileObjectStore();
        store.setDirectory(DIR);
        store.setMuleContext(muleContext);
        store.setName("timed");
        store.setMaxEntries(3);
        store.setEntryTTL(3000);
        store.setExpirationInterval(1000);
        store.initialise();

        // store entries in quick succession
        store.store("1", "1");
        store.store("2", "2");
        store.store("3", "3");

        // they should still be alive at this point
        assertTrue(store.contains("1"));
        assertTrue(store.contains("2"));
        assertTrue(store.contains("3"));

        // wait until the entry TTL has been exceeded
        Thread.sleep(4000);

        // make sure all values are gone
        assertFalse(store.contains("1"));
        assertFalse(store.contains("2"));
        assertFalse(store.contains("3"));
    }

    @Test
    public void testTimedExpiryWithRestart() throws Exception
    {
        // entryTTL=3 and expiryInterval=1 will cause background expiry
        store = new TextFileObjectStore();
        store.setDirectory(DIR);
        store.setMuleContext(muleContext);
        store.setName("timed");
        store.setMaxEntries(3);
        store.setEntryTTL(3000);
        store.setExpirationInterval(1000);
        store.initialise();

        // store entries in quick succession
        store.store("1", "1");
        store.store("2", "2");
        store.store("3", "3");

        // they should still be alive at this point
        assertTrue(store.contains("1"));
        assertTrue(store.contains("2"));
        assertTrue(store.contains("3"));

        store.dispose();

        store = new TextFileObjectStore();
        store.setDirectory(DIR);
        store.setMuleContext(muleContext);
        store.setName("timed");
        store.setMaxEntries(3);
        store.setEntryTTL(3000);
        store.setExpirationInterval(1000);
        store.initialise();

        assertTrue(store.contains("1"));
        assertTrue(store.contains("2"));
        assertTrue(store.contains("3"));

        // wait until the entry TTL has been exceeded
        Thread.sleep(4000);

        // make sure all values are gone
        assertFalse(store.contains("1"));
        assertFalse(store.contains("2"));
        assertFalse(store.contains("3"));

        store.dispose();

        store = new TextFileObjectStore();
        store.setDirectory(DIR);
        store.setMuleContext(muleContext);
        store.setName("timed");
        store.setMaxEntries(3);
        store.setEntryTTL(3000);
        store.setExpirationInterval(1000);
        store.initialise();

        // make sure all values are gone
        assertFalse(store.contains("1"));
        assertFalse(store.contains("2"));
        assertFalse(store.contains("3"));
    }

    @Test
    public void testTimedExpiryWithObjects() throws Exception
    {
        // entryTTL=3 and expiryInterval=1 will cause background expiry
        store = new TextFileObjectStore();
        store.setDirectory(DIR);
        store.setMuleContext(muleContext);
        store.setName("timed");
        store.setMaxEntries(3);
        store.setEntryTTL(3000);
        store.setExpirationInterval(1000);
        store.initialise();
    }

    @Test
    public void testMaxSize() throws Exception
    {
        // entryTTL=-1 means we will have to expire manually
        store = new TextFileObjectStore();
        store.setDirectory(DIR);
        store.setMuleContext(muleContext);
        store.setName("bounded");
        store.setMaxEntries(3);
        store.setEntryTTL(-1);
        store.setExpirationInterval(1000);
        store.initialise();


        store.store("1", "1");
        store.store("2", "2");
        store.store("3", "3");

        assertTrue(store.contains("1"));
        assertTrue(store.contains("2"));
        assertTrue(store.contains("3"));

        // sleep a bit to make sure that entries are not expired, even though the expiry
        // thread is running every second
        Thread.sleep(3000);
        assertTrue(store.contains("1"));
        assertTrue(store.contains("2"));
        assertTrue(store.contains("3"));

        // exceed threshold
        store.store("4", "4");

        // the oldest entry should still be there
        assertTrue(store.contains("1"));

        // expire manually
        store.expire();
        assertFalse(store.contains("1"));
        assertTrue(store.contains("2"));
        assertTrue(store.contains("3"));
        assertTrue(store.contains("4"));

        // exceed some more
        store.store("5", "5");
        store.expire();
        assertFalse(store.contains("2"));
        assertTrue(store.contains("3"));
        assertTrue(store.contains("4"));
        assertTrue(store.contains("5"));

        // and multiple times
        store.store("6", "6");
        store.store("7", "7");
        store.store("8", "8");
        store.store("9", "9");

        store.expire();
        assertTrue(store.contains("7"));
        assertTrue(store.contains("8"));
        assertTrue(store.contains("9"));
        assertFalse(store.contains("3"));
        assertFalse(store.contains("4"));
        assertFalse(store.contains("5"));
        assertFalse(store.contains("6"));
    }

}
