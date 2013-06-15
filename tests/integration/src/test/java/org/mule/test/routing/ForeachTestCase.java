/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMessageCollection;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.client.MuleClient;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.functional.FlowAssert;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.kahadb.util.ByteArrayInputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ForeachTestCase extends FunctionalTestCase
{
    @Rule
    public SystemProperty systemProperty = new SystemProperty("batch.size", "3");

    private MuleClient client;

    @Before
    public void setUp() throws Exception
    {
        client = muleContext.getClient();
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
        for (int i = 0; i < 10; i++)
        {
            MuleMessage msg = new DefaultMuleMessage("message-" + i, muleContext);
            msg.setProperty("out", "out" + (i+1), PropertyScope.OUTBOUND);
            msgCollection.addMessage(msg);
        }

        MuleMessage result = client.send("vm://input-7", msgCollection);
        assertEquals(10, result.getInboundProperty("totalMessages"));
        assertEquals(msgCollection.getPayload(), result.getPayload());
        FlowAssert.verify("message-collection-config");
    }

    @Test
    public void testMessageCollectionConfigurationOneWay() throws Exception
    {
        MuleMessageCollection msgCollection = new DefaultMessageCollection(muleContext);
        for (int i = 0; i < 10; i++)
        {
            MuleMessage msg = new DefaultMuleMessage("message-" + i, muleContext);
            msg.setProperty("out", "out" + (i+1), PropertyScope.OUTBOUND);
            msgCollection.addMessage(msg);
        }

        client.dispatch("vm://input-71", msgCollection);
        FlowAssert.verify("message-collection-config-one-way");
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

    static String sampleXml = "<PurchaseOrder>" + "<Address><Name>Ellen Adams</Name></Address>" + "<Items>"
                              + "<Item PartNumber=\"872-AA\"><Price>140</Price></Item>"
                        + "<Item PartNumber=\"926-AA\"><Price>35</Price></Item>" + "</Items>" + "</PurchaseOrder>";

    @Test
    public void testXmlUpdate() throws Exception
    {
        client.send("vm://input-10", sampleXml, null);
        FlowAssert.verify("process-order-update");
    }

    @Test
    public void testXmlUpdateByteArray() throws Exception
    {
        byte[] xmlba = sampleXml.getBytes();
        client.send("vm://input-10", xmlba, null);
        FlowAssert.verify("process-order-update");
    }

    @Test
    public void testXmlUpdateInputStream() throws Exception
    {
        InputStream xmlis = new ByteArrayInputStream(sampleXml.getBytes());
        client.send("vm://input-10-is", xmlis, null);
        FlowAssert.verify("process-order-update-is");
    }

    @Test
    public void testXmlUpdateMel() throws Exception
    {
        client.send("vm://input-10-mel", sampleXml, null);
        FlowAssert.verify("process-order-update-mel");
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

    @Test
    public void testVariableScope() throws Exception
    {
        final Collection<String> payload = new ArrayList<String>();
        payload.add("pedro");
        payload.add("rodolfo");
        payload.add("roque");
        MuleMessage parent = new DefaultMuleMessage(payload, muleContext);

        MuleMessage result = client.send("vm://input-13", parent);
        assertTrue("Counter variable should not be visible outside foreach scope.",
                result.getExceptionPayload() != null &&
                result.getExceptionPayload().getException().getCause() instanceof RequiredValueException);
    }

    @Test
    public void testTwoOneAfterAnother() throws Exception
    {
        final Collection<String> payload = new ArrayList<String>();
        payload.add("rosa");
        payload.add("maria");
        payload.add("florencia");
        MuleMessage parent = new DefaultMuleMessage(payload, muleContext);

        MuleMessage result = client.send("vm://input-14", parent);
        assertTrue(result.getPayload() instanceof Collection);
        Collection<?> resultPayload = (Collection<?>) result.getPayload();
        assertEquals(3, resultPayload.size());
        assertSame(payload, resultPayload);

        assertEquals(3, result.getInboundProperty("msg-total-messages"));
    }

    @Test
    public void testNestedConfig() throws Exception
    {
        final ArrayList<ArrayList<String>> payload = createNestedPayload();
        MuleMessage parent = new DefaultMuleMessage(payload, muleContext);

        MuleMessage result = client.send("vm://input-15", parent);
        assertTrue(result.getPayload() instanceof Collection);
        Collection<?> resultPayload = (Collection<?>) result.getPayload();
        assertEquals(3, resultPayload.size());
        assertSame(payload, resultPayload);

        MuleMessage out;
        for(int i = 0; i < payload.size(); i++)
        {
            for(int j = 0; j < payload.get(i).size(); j++)
            {
                out = client.request("vm://out", getTestTimeoutSecs());
                assertTrue(out.getPayload() instanceof String);
                assertEquals(payload.get(i).get(j), out.getPayload());
            }
        }
    }

    @Test
    public void testNestedCounters() throws Exception
    {
        final ArrayList<ArrayList<String>> payload = createNestedPayload();
        MuleMessage parent = new DefaultMuleMessage(payload, muleContext);

        MuleMessage result = client.send("vm://input-16", parent);
        assertTrue(result.getPayload() instanceof Collection);
        Collection<?> resultPayload = (Collection<?>) result.getPayload();
        assertEquals(3, resultPayload.size());
        assertSame(payload, resultPayload);

        MuleMessage out;
        for(int i = 0; i < payload.size(); i++)
        {
            for(int j = 0; j < payload.get(i).size(); j++)
            {
                out = client.request("vm://out", getTestTimeoutSecs());
                assertEquals("The nested counters are not consistent.", j+1, out.getInboundProperty("j"));
            }
            out = client.request("vm://out", getTestTimeoutSecs());
            assertEquals("The nested counters are not consistent", i+1, out.getInboundProperty("i"));
        }
    }

    private ArrayList<ArrayList<String>> createNestedPayload()
    {
        final ArrayList<ArrayList<String>> payload = new ArrayList<ArrayList<String>>();
        final ArrayList<String> elem1 = new ArrayList<String>();
        final ArrayList<String> elem2 = new ArrayList<String>();
        final ArrayList<String> elem3 = new ArrayList<String>();
        elem1.add("a1");
        elem1.add("a2");
        elem1.add("a3");
        elem2.add("b1");
        elem2.add("b2");
        elem3.add("c1");
        payload.add(elem1);
        payload.add(elem2);
        payload.add(elem3);

        return payload;
    }

    @Test
    public void testPropertiesRestored() throws Exception
    {
        String[] payload = {"uno", "dos", "tres"};
        MuleMessage parent = new DefaultMuleMessage(payload, muleContext);

        MuleMessage result = client.send("vm://input-17", parent);
        assertTrue(result.getPayload() instanceof String[]);
        String[] resultPayload = (String[]) result.getPayload();
        assertEquals(payload.length, resultPayload.length);
        assertSame(payload, resultPayload);
        FlowAssert.verify("foreach-properties-restored");
    }



    @Test
    public void testMVELList() throws Exception
    {
        MuleMessage parent = new DefaultMuleMessage(null, muleContext);
        client.send("vm://input-18", parent);

        MuleMessage out = client.request("vm://out", getTestTimeoutSecs());
        assertTrue(out.getPayload() instanceof String);
        String outPayload = (String) out.getPayload();
        assertEquals("foo", outPayload);

        out = client.request("vm://out", getTestTimeoutSecs());
        assertTrue(out.getPayload() instanceof String);
        outPayload = (String) out.getPayload();
        assertEquals("bar", outPayload);
    }

    @Test
    public void testMVELMap() throws Exception
    {
        MuleMessage parent = new DefaultMuleMessage(null, muleContext);
        client.send("vm://input-19", parent);

        Map<String,String> m = new HashMap<String, String>();
        m.put("key1", "val1");
        m.put("key2", "val2");

        MuleMessage out = client.request("vm://out", getTestTimeoutSecs());
        assertTrue(out.getPayload() instanceof String);
        String outPayload = (String) out.getPayload();
        assertTrue(m.containsValue(outPayload));

        out = client.request("vm://out", getTestTimeoutSecs());
        assertTrue(out.getPayload() instanceof String);
        outPayload = (String) out.getPayload();
        assertTrue(m.containsValue(outPayload));
    }

    @Test
    public void testMVELCollection() throws Exception
    {
        MuleMessage parent = new DefaultMuleMessage(null, muleContext);
        client.send("vm://input-20", parent);

        Map<String,String> m = new HashMap<String, String>();
        m.put("key1", "val1");
        m.put("key2", "val2");

        MuleMessage out = client.request("vm://out", getTestTimeoutSecs());
        assertTrue(out.getPayload() instanceof String);
        String outPayload = (String) out.getPayload();
        assertTrue(m.containsValue(outPayload));

        out = client.request("vm://out", getTestTimeoutSecs());
        assertTrue(out.getPayload() instanceof String);
        outPayload = (String) out.getPayload();
        assertTrue(m.containsValue(outPayload));
    }

    @Test
    public void testMVELArray() throws Exception
    {
        MuleMessage parent = new DefaultMuleMessage(null, muleContext);

        client.send("vm://input-21", parent);

        MuleMessage out = client.request("vm://out", getTestTimeoutSecs());
        assertTrue(out.getPayload() instanceof String);
        String outPayload = (String) out.getPayload();

        assertEquals("foo", outPayload);
        FlowAssert.verify("mvel-array");

        out = client.request("vm://out", getTestTimeoutSecs());
        assertTrue(out.getPayload() instanceof String);
        outPayload = (String) out.getPayload();
        assertEquals("bar", outPayload);
    }

    @Test
    public void testRequestReply() throws Exception
    {
        MuleMessage parent = new DefaultMuleMessage(null, muleContext);
        MuleMessage msg = client.send("vm://input-22", parent);
        assertNotNull(msg);
        assertEquals(msg.getProperty("processedMessages", PropertyScope.SESSION), "0123");
    }
    
    @Test
    public void foreachWithAsync() throws Exception
    {
        final int size = 20;
        List<String> list = new ArrayList<String>(size);

        for (int i = 0; i < size; i++)
        {
            list.add(RandomStringUtils.randomAlphabetic(10));
        }

        CountDownLatch latch = new CountDownLatch(size);
        MuleEvent event = getTestEvent(list);
        event.setFlowVariable("latch", latch);

        this.testFlow("foreachWithAsync", event);

        latch.await(10, TimeUnit.SECONDS);
    }
}
