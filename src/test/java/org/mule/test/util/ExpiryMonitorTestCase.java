/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.util;

import org.mule.tck.NamedTestCase;
import org.mule.util.monitor.Expirable;
import org.mule.util.monitor.ExpiryMonitor;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ExpiryMonitorTestCase extends NamedTestCase
{
    private boolean expired = false;

    protected void setUp() throws Exception
    {
        expired = false;
    }

    public void testExiry() throws InterruptedException
    {
        ExpiryMonitor monitor = new ExpiryMonitor(100);
        Expirable e = new Expirable(){
            public void expired()
            {
                expired=true;
            }
        };
        monitor.addExpirable(300, e);
        Thread.sleep(800);
        assertTrue(expired);
        assertTrue(!monitor.isRegistered(e));
    }

    public void testNotExiry() throws InterruptedException
    {
        ExpiryMonitor monitor = new ExpiryMonitor(100);
        Expirable e = new Expirable(){
            public void expired()
            {
                expired=true;
            }
        };
        monitor.addExpirable(800, e);
        Thread.sleep(300);
        assertTrue(!expired);
        Thread.sleep(800);
        assertTrue(expired);
        assertTrue(!monitor.isRegistered(e));
    }

    public void testExiryWithReset() throws InterruptedException
    {
        ExpiryMonitor monitor = new ExpiryMonitor(100);
        Expirable e = new Expirable(){
            public void expired()
            {
                expired=true;
            }
        };
        monitor.addExpirable(600, e);
        Thread.sleep(200);
        assertTrue(!expired);
        monitor.resetExpirable(e);
        Thread.sleep(200);
        assertTrue(!expired);
        Thread.sleep(600);
        assertTrue(expired);

        assertTrue(!monitor.isRegistered(e));
    }

    public void testNotExiryWithRemove() throws InterruptedException
    {
        ExpiryMonitor monitor = new ExpiryMonitor(100);
        Expirable e = new Expirable(){
            public void expired()
            {
                expired=true;
            }
        };
        monitor.addExpirable(1000, e);
        Thread.sleep(200);
        assertTrue(!expired);
        Thread.sleep(200);
        monitor.removeExpirable(e);
        Thread.sleep(800);
        assertTrue(!expired);
        assertTrue(!monitor.isRegistered(e));
    }
}
