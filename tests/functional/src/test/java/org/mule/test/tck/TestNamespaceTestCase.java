/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.tck;

import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestComponent2;

import java.io.IOException;

public class TestNamespaceTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "test-namespace-config.xml";
    }

    public void testComponent1Config() throws Exception
    {
        Object object = getComponent("testService1");
        assertNotNull(object);
        assertTrue(object instanceof FunctionalTestComponent2);
        FunctionalTestComponent2 ftc = (FunctionalTestComponent2
                ) object;

        assertFalse(ftc.isEnableMessageHistory());
        assertFalse(ftc.isEnableNotifications());
        assertNull(ftc.getAppendString());
        assertEquals("Foo Bar Car Jar", ftc.getReturnData());
    }

    public void testComponent2Config() throws Exception
    {
        String testData = loadResourceAsString("test-data.txt");
        Object object = getComponent("testService2");
        assertNotNull(object);
        assertTrue(object instanceof FunctionalTestComponent2);
        FunctionalTestComponent2 ftc = (FunctionalTestComponent2) object;

        assertTrue(ftc.isThrowException());
        assertNotNull(ftc.getExceptionToThrow());
        assertTrue(ftc.getExceptionToThrow().isAssignableFrom(IOException.class));

        assertEquals(testData, ftc.getReturnData());

        assertTrue(ftc.isEnableMessageHistory());
        assertTrue(ftc.isEnableNotifications());
        assertNull(ftc.getAppendString());
    }

    public void testComponent3Config() throws Exception
    {
        Object object = getComponent("testService3");
        assertNotNull(object);
        assertTrue(object instanceof FunctionalTestComponent2);
        FunctionalTestComponent2 ftc = (FunctionalTestComponent2) object;

        assertFalse(ftc.isEnableMessageHistory());
        assertTrue(ftc.isEnableNotifications());
        assertEquals(" ${mule:serviceName}", ftc.getAppendString());
        assertNull(ftc.getReturnData());
    }

}
