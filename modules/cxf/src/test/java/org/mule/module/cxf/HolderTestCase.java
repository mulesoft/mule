/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transformer.AbstractTransformer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.xml.ws.Holder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HolderTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Parameterized.Parameter(0)
    public String config;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"holder-conf.xml"},
                {"holder-conf-httpn.xml"}
        });
    }

    @Override
    protected String getConfigFile()
    {
        return config;
    }

    @Test
    public void testClientEchoHolder() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("TEST", (Map<String,Object>)null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage received = client.send("vm://echoClient", request);
        assertNotNull(received);
        Object[] payload = (Object[])received.getPayload();
        assertEquals("one-response", payload[0]);
        assertEquals(null, payload[1]);
        assertEquals("one-holder1", ((Holder)payload[2]).value);
        assertEquals("one-holder2", ((Holder)payload[3]).value);
    }

    @Test
    public void testClientProxyEchoHolder() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("TEST", (Map<String,Object>)null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage received = client.send("vm://echoClientProxy", request);
        assertNotNull(received);
        Object[] payload = (Object[])received.getPayload();
        assertEquals("one-response", payload[0]);
        assertEquals("one-holder1", ((Holder)payload[1]).value);
        assertEquals("one-holder2", ((Holder)payload[2]).value);
    }

    @Test
    public void testClientEcho2Holder() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("TEST", (Map<String,Object>)null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage received = client.send("vm://echo2Client", request);
        assertNotNull(received);
        Object[] payload = (Object[])received.getPayload();
        assertEquals("one-response", payload[0]);
        assertEquals(null, payload[1]);
        assertEquals("two-holder", ((Holder)payload[2]).value);
    }

    @Test
    public void testClientProxyEcho2Holder() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("TEST", (Map<String,Object>)null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage received = client.send("vm://echo2ClientProxy", request);
        assertNotNull(received);
        Object[] payload = (Object[])received.getPayload();
        assertEquals("one-response", payload[0]);
        assertEquals("two-holder", ((Holder)payload[1]).value);
    }

    @Test
    public void testClientEcho3Holder() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("TEST", (Map<String,Object>)null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage received = client.send("vm://echo3Client", request);
        assertNotNull(received);
        Object[] payload = (Object[])received.getPayload();
        assertEquals(null, payload[0]);
        assertEquals("one", ((Holder)payload[1]).value);
    }

    @Test
    public void testClientProxyEcho3Holder() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("TEST", (Map<String,Object>)null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage received = client.send("vm://echo3ClientProxy", request);
        assertNotNull(received);
        Object[] payload = (Object[])received.getPayload();
        assertEquals(null, payload[0]);
        assertEquals("one", ((Holder)payload[1]).value);
    }

    public static class HolderTransformer extends AbstractTransformer
    {
        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            Holder<String> outS1 = new Holder<>();
            Holder<String> outS2 = new Holder<>();

            Object objArray[] = new Object[3];
            objArray[0] = "one";
            objArray[1] = outS1;
            objArray[2] = outS2;

            return objArray;
        }
    }

    public static class HolderTransformer2 extends AbstractTransformer
    {
        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            Holder<String> outS1 = new Holder<>();

            Object objArray[] = new Object[3];
            objArray[0] = "one";
            objArray[1] = outS1;
            objArray[2] = "two";

            return objArray;
        }
    }

    public static class HolderTransformer3 extends AbstractTransformer
    {
        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            Holder<String> outS1 = new Holder<>();

            Object objArray[] = new Object[2];
            objArray[0] = outS1;
            objArray[1] = "one";

            return objArray;
        }
    }
}
