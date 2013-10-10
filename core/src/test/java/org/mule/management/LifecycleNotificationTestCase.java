/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management;

import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class LifecycleNotificationTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testManageLifecycle() throws Exception
    {
        assertTrue(muleContext.isInitialised());
//        muleContext.start();
//        assertTrue(muleContext.isStarted());
//        muleContext.stop();
//        assertFalse(muleContext.isStarted());
//        muleContext.dispose();
//        assertTrue(muleContext.isDisposed());
//        muleContext.initialise();
//        assertTrue(muleContext.isInitialised());
//        muleContext.start();
//        assertTrue(muleContext.isStarted());


    }
}
