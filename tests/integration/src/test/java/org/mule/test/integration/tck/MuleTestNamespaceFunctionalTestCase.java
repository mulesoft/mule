/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.tck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.ExceptionUtils;

import java.io.FileNotFoundException;

import org.junit.Test;

public class MuleTestNamespaceFunctionalTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/tck/test-namespace-config-flow.xml";
    }

    @Test
    public void testService1() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://service1", "foo", null);
        assertNotNull(message);
        assertNull(message.getExceptionPayload());
        assertEquals("Foo Bar Car Jar", message.getPayloadAsString());
    }

    @Test
    public void testService2() throws Exception
    {
        String result = loadResourceAsString("org/mule/test/integration/tck/test-data.txt");
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://service2", "foo", null);
        assertNotNull(message);
        assertNull(message.getExceptionPayload());
        assertEquals(result, message.getPayloadAsString());
    }

    @Test
    public void testService3() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://service3", "foo", null);
        assertNotNull(message);
        assertNull(message.getExceptionPayload());
        assertEquals("foo received in testService3", message.getPayloadAsString());
    }

    @Test
    public void testService4() throws Exception
    {
        try
        {
            MuleClient client = muleContext.getClient();
            client.send("vm://service4", "foo", null);
        }
        catch (Exception e)
        {
            // expected
        }
    }

    @Test
    public void testService5() throws Exception
    {
        try
        {
            MuleClient client = muleContext.getClient();
            client.send("vm://service5", "foo", null);
        }
        catch (Exception e)
        {
            assertTrue(ExceptionUtils.getRootCause(e) instanceof FileNotFoundException);
        }
    }
}
