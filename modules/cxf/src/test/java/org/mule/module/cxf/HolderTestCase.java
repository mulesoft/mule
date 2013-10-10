/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transformer.AbstractTransformer;

import java.util.Map;

import javax.xml.ws.Holder;

import org.junit.Rule;
import org.junit.Test;

public class HolderTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "holder-conf.xml";
    }

    @Test
    public void testClientEchoHolder() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("TEST", (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
        MuleMessage received = client.send("vm://echoClient", request);
        assertNotNull(received);
        Object[] payload = (Object[])received.getPayload();
        assertEquals("one-response", payload[0]);
        assertEquals("one-holder1", ((Holder)payload[2]).value);
        assertEquals("one-holder2", ((Holder)payload[3]).value);
    }

    @Test
    public void testClientProxyEchoHolder() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("TEST", (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
        MuleMessage received = client.send("vm://echoClientProxy", request);
        assertNotNull(received);
        Object[] payload = (Object[])received.getPayload();
        assertEquals("one-response", payload[0]);
        assertEquals("one-holder1", ((Holder)payload[1]).value);
        assertEquals("one-holder2", ((Holder)payload[2]).value);
    }

    @Test
    public void testClientProxyEcho2Holder() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("TEST", (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
        MuleMessage received = client.send("vm://echo2ClientProxy", request);
        assertNotNull(received);
        Object[] payload = (Object[])received.getPayload();
        assertEquals("one-response", payload[0]);
        assertEquals("two-holder", ((Holder)payload[1]).value);
    }

    @Test
    public void testClientProxyEcho3Holder() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("TEST", (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
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
            Holder<String> outS1 = new Holder<String>();
            Holder<String> outS2 = new Holder<String>();

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
            Holder<String> outS1 = new Holder<String>();

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
            Holder<String> outS1 = new Holder<String>();

            Object objArray[] = new Object[2];
            objArray[0] = outS1;
            objArray[1] = "one";

            return objArray;
        }
    }

}
