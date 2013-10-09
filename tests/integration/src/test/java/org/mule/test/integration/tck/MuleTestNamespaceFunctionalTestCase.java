/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.tck;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.util.ExceptionUtils;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MuleTestNamespaceFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/tck/test-namespace-config-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/tck/test-namespace-config-flow.xml"}});
    }

    public MuleTestNamespaceFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testService1() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://service1", "foo", null);
        assertNotNull(message);
        assertNull(message.getExceptionPayload());
        assertEquals("Foo Bar Car Jar", message.getPayloadAsString());
    }

    @Test
    public void testService2() throws Exception
    {
        String result = loadResourceAsString("org/mule/test/integration/tck/test-data.txt");
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://service2", "foo", null);
        assertNotNull(message);
        assertNull(message.getExceptionPayload());
        assertEquals(result, message.getPayloadAsString());
    }

    @Test
    public void testService3() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
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
            muleContext.getClient().send("vm://service4", "foo", null);
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
            muleContext.getClient().send("vm://service5", "foo", null);
        }
        catch (Exception e)
        {
            assertTrue(ExceptionUtils.getRootCause(e) instanceof FileNotFoundException);
        }
    }
}
