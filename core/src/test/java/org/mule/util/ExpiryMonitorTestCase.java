/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
