/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.util.FileUtils.deleteTree;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.junit.Test;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

public class TextFileStoreTestCase extends AbstractMuleContextTestCase
{

    public static final String DIR = ".mule/temp";
    TextFileObjectStore store;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        deleteTree(new File(DIR));
    }

    @Override
    protected void doTearDown() throws Exception
    {
        if (store != null)
        {
            store.dispose();
        }
        deleteTree(new File(DIR));
        super.doTearDown();
    }

    @Test
    public void testTimedExpiry() throws Exception
    {
        // entryTTL=3 and expiryInterval=1 will cause background expiry
        createObjectStore("timed", 3000, 1000);

        // store entries in quick succession
        storeObjects("1", "2", "3");

        // they should still be alive at this point
        assertObjectsInStore("timed.dat", "1", "2", "3");

        // wait until the entry TTL has been exceeded
        Thread.sleep(4000);

        // make sure all values are gone
        assertObjectsExpired("timed.dat", "1", "2", "3");
    }

    @Test
    public void testTimedExpiryWithRestart() throws Exception
    {
        // entryTTL=3 and expiryInterval=1 will cause background expiry
        createObjectStore("timed", 3000, 1000);

        // store entries in quick succession
        storeObjects("1", "2", "3");

        // they should still be alive at this point
        assertObjectsInStore("timed.dat", "1", "2", "3");

        store.dispose();

        createObjectStore("timed", 3000, 1000);

        assertObjectsInStore("timed.dat", "1", "2", "3");

        // wait until the entry TTL has been exceeded
        Thread.sleep(4000);

        // make sure all values are gone
        assertObjectsExpired("timed.dat", "1", "2", "3");

        store.dispose();

        createObjectStore("timed", 3000, 1000);

        // make sure all values are gone
        assertObjectsExpired("timed.dat", "1", "2", "3");
    }

    @Test
    public void testTimedExpiryWithObjects() throws Exception
    {
        // entryTTL=3 and expirationInterval=1 will cause background expiry
        createObjectStore("timed", 3000, 1000);
    }

    @Test
    public void testMaxSize() throws Exception
    {
        // entryTTL=-1 means we will have to expire manually
        createObjectStore("bounded", -1, 1000);

        storeObjects("1", "2", "3");

        assertObjectsInStore("bounded.dat", "1", "2", "3");

        // sleep a bit to make sure that entries are not expired, even though the expiry
        // thread is running every second
        Thread.sleep(3000);

        assertObjectsInStore("bounded.dat", "1", "2", "3");

        // exceed threshold
        storeObjects("4");

        // expire manually in case the expiration
        // monitor has not update the store yet
        store.expire();

        assertObjectsExpired("bounded.dat", "1");
        assertObjectsInStore("bounded.dat", "2", "3", "4");

        // exceed some more
        storeObjects("5");

        store.expire();

        assertObjectsExpired("bounded.dat", "2");
        assertObjectsInStore("bounded.dat", "3", "4", "5");

        // and multiple times
        storeObjects("6", "7", "8", "9");

        store.expire();

        assertObjectsExpired("bounded.dat", "3", "4", "5");
        assertObjectsInStore("bounded.dat", "7", "8", "9");
    }

    private void assertObjectsExpired(String file, String... identifiers) throws Exception
    {
        Properties props = new Properties();
        props.load(new FileInputStream(new File(DIR + "/" + file)));

        for (String id : identifiers)
        {
            assertThat(store.contains(id), equalTo(false));
            assertThat(props.values(), not(hasItem(id)));
        }
    }

    private void assertObjectsInStore(String file, String... identifiers) throws Exception
    {
        Properties props = new Properties();
        props.load(new FileInputStream(new File(DIR + "/" + file)));

        for (String id : identifiers)
        {
            assertThat(store.contains(id), equalTo(true));
            assertThat(props.values(), hasItem(id));
        }
    }

    private void storeObjects(String... objects) throws Exception
    {
        for (String entry : objects)
        {
            store.store(entry, entry);
        }
    }

    private void createObjectStore(String fileName, int ttl, int expirationInterval) throws InitialisationException
    {
        store = new TextFileObjectStore();
        store.setDirectory(DIR);
        store.setMuleContext(muleContext);
        store.setName(fileName);
        store.setMaxEntries(3);
        store.setEntryTTL(ttl);
        store.setExpirationInterval(expirationInterval);
        store.initialise();
    }

}
