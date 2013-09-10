/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
