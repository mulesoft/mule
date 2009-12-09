/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.tck;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.exceptions.FunctionalTestException;

import java.io.FileNotFoundException;

public class MuleTestNamespaceFunctionalTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/integration/tck/test-namespace-config.xml";
    }

    public void testService1() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("vm://service1", "foo", null);
        assertNotNull(message);
        assertNull(message.getExceptionPayload());
        assertEquals("Foo Bar Car Jar", message.getPayloadAsString());
    }

    public void testService2() throws Exception
    {
        String result = loadResourceAsString("org/mule/test/integration/tck/test-data.txt");
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("vm://service2", "foo", null);
        assertNotNull(message);
        assertNull(message.getExceptionPayload());
        assertEquals(result, message.getPayloadAsString());
    }

    public void testService3() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("vm://service3", "foo", null);
        assertNotNull(message);
        assertNull(message.getExceptionPayload());
        assertEquals("foo received in testService3", message.getPayloadAsString());
    }

    public void testService4() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("vm://service4", "foo", null);
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(FunctionalTestException.EXCEPTION_MESSAGE, message.getExceptionPayload().getMessage());
    }

    public void testService5() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("vm://service5", "foo", null);
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertTrue(message.getExceptionPayload().getRootException() instanceof FileNotFoundException);
    }
}
