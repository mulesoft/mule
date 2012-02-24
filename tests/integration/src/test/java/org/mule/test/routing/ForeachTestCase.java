/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMessageCollection;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.module.client.MuleClient;
import org.mule.tck.functional.FlowAssert;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class ForeachTestCase extends FunctionalTestCase
{

    private MuleClient client;

    @Before
    public void setUp() throws Exception
    {
        client = new MuleClient(muleContext);
    }

    @Override
    protected String getConfigResources()
    {
        return "foreach-test.xml";
    }

    @Test
    public void testDefaultConfiguration() throws Exception
    {
        final Collection<String> payload = new ArrayList<String>();
        payload.add("julio");
        payload.add("sosa");

        MuleMessage result = client.send("vm://input-1", payload, null);
        assertTrue(result.getPayload() instanceof Collection);
        Collection<?> resultPayload = (Collection<?>) result.getPayload();
        assertEquals(2, resultPayload.size());
        assertSame(payload, resultPayload);

        MuleMessage out = client.request("vm://out", getTestTimeoutSecs());
        assertTrue(out.getPayload() instanceof String);
        assertEquals("julio", out.getPayload());

        out = client.request("vm://out", getTestTimeoutSecs());
        assertTrue(out.getPayload() instanceof String);
        assertEquals("sosa", out.getPayload());
    }

    @Test
    public void testDefaultConfigurationPlusMP() throws Exception
    {
        final Collection<String> payload = new ArrayList<String>();
        payload.add("syd");
        payload.add("barrett");

        MuleMessage result = client.send("vm://input-2", payload, null);
        assertTrue(result.getPayload() instanceof Collection);
        Collection<?> resultPayload = (Collection<?>) result.getPayload();
        assertEquals(3, resultPayload.size());
        assertSame(payload, resultPayload);

        MuleMessage out = client.request("vm://out", getTestTimeoutSecs());
        assertTrue(out.getPayload() instanceof String);
        assertEquals("syd", out.getPayload());

        out = client.request("vm://out", getTestTimeoutSecs());
        assertTrue(out.getPayload() instanceof String);
        assertEquals("barrett", out.getPayload());

    }

    @Test
    public void testDefaultConfigurationExpression() throws Exception
    {
        final Collection<String> names = new ArrayList<String>();
        names.add("residente");
        names.add("visitante");
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("names", names);
        MuleMessage message = new DefaultMuleMessage("message payload", props, muleContext);

        MuleMessage result = client.send("vm://input-3", message);
        assertTrue(result.getPayload() instanceof String);
        assertEquals(names.size(), ((Collection<?>) message.getOutboundProperty("names")).size());

        MuleMessage out = client.request("vm://out", getTestTimeoutSecs());
        assertTrue(out.getPayload() instanceof String);
        assertEquals("residente", out.getPayload());

        out = client.request("vm://out", getTestTimeoutSecs());
        assertTrue(out.getPayload() instanceof String);
        assertEquals("visitante", out.getPayload());
    }

    @Test
    public void testPartitionedConfiguration() throws Exception
    {
        final Collection<String> payload = new ArrayList<String>();
        payload.add("gulp");
        payload.add("oktubre");
        payload.add("un baion");
        payload.add("bang bang");
        payload.add("la mosca");

        MuleMessage result = client.send("vm://input-4", payload, null);
        assertTrue(result.getPayload() instanceof Collection);
        Collection<?> resultPayload = (Collection<?>) result.getPayload();
        assertEquals(5, resultPayload.size());
        assertSame(payload, resultPayload);

        MuleMessage out = client.request("vm://out", getTestTimeoutSecs());
        assertTrue(out.getPayload() instanceof Collection);
        Collection<?> outPayload = (Collection<?>) out.getPayload();
        assertEquals(3, outPayload.size());

        out = client.request("vm://out", getTestTimeoutSecs());
        assertTrue(out.getPayload() instanceof Collection);
        outPayload = (Collection<?>) out.getPayload();
        assertEquals(2, outPayload.size());
    }

    @Test
    public void testRootMessageConfiguration() throws Exception
    {
        final Collection<String> payload = new ArrayList<String>();
        payload.add("pyotr");
        payload.add("ilych");
        MuleMessage parent = new DefaultMuleMessage(payload, muleContext);

        MuleMessage result = client.send("vm://input-5", parent);
        assertTrue(result.getPayload() instanceof Collection);
        Collection<?> resultPayload = (Collection<?>) result.getPayload();
        assertEquals(2, resultPayload.size());
        assertSame(payload, resultPayload);

        assertSame(parent.getPayload(), ((MuleMessage) result.getInboundProperty("parent")).getPayload());
    }

    @Test
    public void testCounterConfiguration() throws Exception
    {
        final Collection<String> payload = new ArrayList<String>();
        payload.add("wolfgang");
        payload.add("amadeus");
        payload.add("mozart");
        MuleMessage parent = new DefaultMuleMessage(payload, muleContext);

        MuleMessage result = client.send("vm://input-6", parent);
        assertTrue(result.getPayload() instanceof Collection);
        Collection<?> resultPayload = (Collection<?>) result.getPayload();
        assertEquals(3, resultPayload.size());
        assertSame(payload, resultPayload);

        assertEquals(result.getInboundProperty("msg-last-index"), 3);
    }

    @Test
    public void testMessageCollectionConfiguration() throws Exception
    {
        MuleMessageCollection msgCollection = new DefaultMessageCollection(muleContext);
        msgCollection.setInvocationProperty("totalMessages", 0);
        for (int i = 0; i < 10; i++)
        {
            MuleMessage msg = new DefaultMuleMessage("message-" + i, muleContext);
            msgCollection.addMessage(msg);
        }

        MuleMessage result = client.send("vm://input-7", msgCollection);
        assertEquals(10, result.getInboundProperty("totalMessages"));
        assertEquals(msgCollection.getPayload(), result.getPayload());
    }

    @Test
    public void testMapPayload() throws Exception
    {
        final Map<String, String> payload = new HashMap<String, String>();
        payload.put("name", "david");
        payload.put("surname", "bowie");
        MuleMessage parent = new DefaultMuleMessage(payload, muleContext);

        MuleMessage result = client.send("vm://input-8", parent);
        assertTrue(result.getPayload() instanceof Map);
        Map<?, ?> resultPayload = (Map<?, ?>) result.getPayload();
        assertEquals(payload.size(), resultPayload.size());
        assertEquals(payload.size(), result.getInboundProperty("totalMessages"));
        assertSame(payload, resultPayload);
    }

    @Test
    public void testMapExpression() throws Exception
    {
        final Collection<String> names = new ArrayList<String>();
        names.add("Sergei");
        names.add("Vasilievich");
        names.add("Rachmaninoff");
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("names", names);
        MuleMessage message = new DefaultMuleMessage("message payload", props, muleContext);

        MuleMessage result = client.send("vm://input-9", message);
        assertTrue(result.getPayload() instanceof String);
        assertEquals(names.size(), ((Collection<?>) message.getOutboundProperty("names")).size());
        assertEquals(names.size(), result.getInboundProperty("totalMessages"));
    }

    @Test
    public void testXmlUpdate() throws Exception
    {
        String xml = "<PurchaseOrder>" + "<Address><Name>Ellen Adams</Name></Address>" + "<Items>" + "<Item PartNumber=\"872-AA\"><Price>140</Price></Item>"
                     + "<Item PartNumber=\"926-AA\"><Price>35</Price></Item>" + "</Items>" + "</PurchaseOrder>";
        client.send("vm://input-10", xml, null);
        FlowAssert.verify("process-order-update");
    }

    @Test
    public void testJsonUpdate() throws Exception
    {
        String json = "{\"order\": {\"name\": \"Ellen\", \"email\": \"ellen@mail.com\", \"items\": [{\"key1\": \"value1\"}, {\"key2\": \"value2\"}] } }";
        client.send("vm://input-11", json, null);
        FlowAssert.verify("process-json-update");
    }

    @Test
    public void testArrayPayload() throws Exception
    {
        String[] payload = {"uno", "dos", "tres"};
        MuleMessage parent = new DefaultMuleMessage(payload, muleContext);

        MuleMessage result = client.send("vm://input-12", parent);
        assertTrue(result.getPayload() instanceof String[]);
        String[] resultPayload = (String[]) result.getPayload();
        assertEquals(payload.length, resultPayload.length);
        assertSame(payload, resultPayload);
        FlowAssert.verify("array-expression-config");
    }

}


