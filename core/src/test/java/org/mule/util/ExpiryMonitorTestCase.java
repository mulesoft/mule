/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.monitor.Expirable;
import org.mule.util.monitor.ExpiryMonitor;

import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ExpiryMonitorTestCase extends AbstractMuleTestCase
{
    private boolean expired = false;

    protected void doSetUp() throws Exception
    {
        expired = false;
    }

    @Test
    public void testExpiry() throws InterruptedException
    {
        ExpiryMonitor monitor = new ExpiryMonitor("test", 100, null, false);
        Expirable e = new Expirable()
        {
            public void expired()
            {
                expired = true;
            }
        };
        monitor.addExpirable(300, TimeUnit.MILLISECONDS, e);
        Thread.sleep(800);
        assertTrue(expired);
        assertTrue(!monitor.isRegistered(e));
    }

    @Test
    public void testNotExpiry() throws InterruptedException
    {
        ExpiryMonitor monitor = new ExpiryMonitor("test", 100, null, false);
        Expirable e = new Expirable()
        {
            public void expired()
            {
                expired = true;
            }
        };
        monitor.addExpirable(800, TimeUnit.MILLISECONDS, e);
        Thread.sleep(300);
        assertTrue(!expired);
        Thread.sleep(800);
        assertTrue(expired);
        assertTrue(!monitor.isRegistered(e));
    }

    @Test
    public void testExpiryWithReset() throws InterruptedException
    {
        ExpiryMonitor monitor = new ExpiryMonitor("test", 100, null, false);
        Expirable e = new Expirable()
        {
            public void expired()
            {
                expired = true;
            }
        };
        monitor.addExpirable(600, TimeUnit.MILLISECONDS, e);
        Thread.sleep(200);
        assertTrue(!expired);
        monitor.resetExpirable(e);
        Thread.sleep(200);
        assertTrue(!expired);
        Thread.sleep(600);
        assertTrue(expired);

        assertTrue(!monitor.isRegistered(e));
    }

    @Test
    public void testNotExpiryWithRemove() throws InterruptedException
    {
        ExpiryMonitor monitor = new ExpiryMonitor("test", 100, null, false);
        Expirable e = new Expirable()
        {
            public void expired()
            {
                expired = true;
            }
        };
        monitor.addExpirable(1000, TimeUnit.MILLISECONDS, e);
        Thread.sleep(200);
        assertTrue(!expired);
        Thread.sleep(200);
        monitor.removeExpirable(e);
        Thread.sleep(800);
        assertTrue(!expired);
        assertTrue(!monitor.isRegistered(e));
    }

}
