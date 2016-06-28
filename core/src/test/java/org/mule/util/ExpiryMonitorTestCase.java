/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import static org.junit.Assert.assertTrue;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.util.monitor.Expirable;
import org.mule.util.monitor.ExpiryMonitor;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExpiryMonitorTestCase extends AbstractMuleTestCase
{
    private boolean expired = false;

    private ExpiryMonitor monitor;

    @Before
    public void doSetUp() throws Exception
    {
        expired = false;
        monitor = new ExpiryMonitor("test", 100, null, false);
    }

    @After
    public void after()
    {
        monitor.dispose();
    }

    @Test
    public void testExpiry() throws InterruptedException
    {
        final Expirable e = new Expirable()
        {
            @Override
            public void expired()
            {
                expired = true;
            }
        };
        monitor.addExpirable(300, TimeUnit.MILLISECONDS, e);
        new PollingProber(800, 50).check(new JUnitProbe()
        {
            @Override
            public boolean test()
            {
                assertTrue(expired);
                assertTrue(!monitor.isRegistered(e));
                return true;
            }
        });
    }

    @Test
    public void testNotExpiry() throws InterruptedException
    {
        final Expirable e = new Expirable()
        {
            @Override
            public void expired()
            {
                expired = true;
            }
        };
        monitor.addExpirable(800, TimeUnit.MILLISECONDS, e);
        Thread.sleep(300);
        assertTrue(!expired);
        new PollingProber(800, 50).check(new JUnitProbe()
        {
            @Override
            public boolean test()
            {
                assertTrue(expired);
                assertTrue(!monitor.isRegistered(e));
                return true;
            }
        });
    }

    @Test
    public void testExpiryWithReset() throws InterruptedException
    {
        final Expirable e = new Expirable()
        {
            @Override
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
        new PollingProber(600, 50).check(new JUnitProbe()
        {
            @Override
            public boolean test()
            {
                assertTrue(expired);
                assertTrue(!monitor.isRegistered(e));
                return true;
            }
        });
    }

    @Test
    public void testNotExpiryWithRemove() throws InterruptedException
    {
        final Expirable e = new Expirable()
        {
            @Override
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
        new PollingProber(600, 50).check(new JUnitProbe()
        {
            @Override
            public boolean test()
            {
                assertTrue(!expired);
                assertTrue(!monitor.isRegistered(e));
                return true;
            }
        });
    }

}
