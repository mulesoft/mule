/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.transport.PropertyScope;
import org.mule.functional.functional.FlowAssert;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Ignore;
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
    protected String getConfigFile()
    {
        return "foreach-test.xml";
    }

    @Test
    public void defaultConfiguration() throws Exception
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
    public void defaultConfigurationPlusMP() throws Exception
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
    public void defaultConfigurationExpression() throws Exception
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
    public void partitionedConfiguration() throws Exception
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
    public void rootMessageConfiguration() throws Exception
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
    public void counterConfiguration() throws Exception
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

        assertThat(result.getInboundProperty("msg-last-index"), is(3));
    }

    @Test
    public void messageCollectionConfiguration() throws Exception
    {
        List<MuleMessage> list = new ArrayList<>();
        for (int i = 0; i < 10; i++)
        {
            MuleMessage msg = new DefaultMuleMessage("message-" + i, muleContext);
            msg.setProperty("out", "out" + (i+1), PropertyScope.OUTBOUND);
            list.add(msg);
        }

        MuleMessage msgCollection = new DefaultMuleMessage(list, muleContext);
        MuleMessage result = client.send("vm://input-7", msgCollection);
        assertThat(result.getInboundProperty("totalMessages"), is(10));
        assertEquals(msgCollection.getPayload(), result.getPayload());
        FlowAssert.verify("message-collection-config");
    }

    @Test
    public void messageCollectionConfigurationOneWay() throws Exception
    {
        List<MuleMessage> list = new ArrayList<>();
        for (int i = 0; i < 10; i++)
        {
            MuleMessage msg = new DefaultMuleMessage("message-" + i, muleContext);
            msg.setProperty("out", "out" + (i+1), PropertyScope.INBOUND);
            list.add(msg);
        }
        MuleMessage msgCollection = new DefaultMuleMessage(list, muleContext);

        MuleEvent event = getTestEvent("");
        event.setMessage(msgCollection);
        final String flowName = "message-collection-config-one-way";

        runFlow(flowName, event);
        FlowAssert.verify(flowName);
    }

    @Test
    public void mapPayload() throws Exception
    {
        final Map<String, String> payload = new HashMap<String, String>();
        payload.put("name", "david");
        payload.put("surname", "bowie");
        MuleMessage parent = new DefaultMuleMessage(payload, muleContext);

        MuleMessage result = client.send("vm://input-8", parent);
        assertTrue(result.getPayload() instanceof Map);
        Map<?, ?> resultPayload = (Map<?, ?>) result.getPayload();
        assertEquals(payload.size(), resultPayload.size());
        assertThat(result.getInboundProperty("totalMessages"), is(payload.size()));
        assertSame(payload, resultPayload);
    }

    @Test
    public void mapExpression() throws Exception
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
        assertThat(result.getInboundProperty("totalMessages"), is(names.size()));
    }

    static String sampleXml = "<PurchaseOrder>" + "<Address><Name>Ellen Adams</Name></Address>" + "<Items>"
                              + "<Item PartNumber=\"872-AA\"><Price>140</Price></Item>"
                        + "<Item PartNumber=\"926-AA\"><Price>35</Price></Item>" + "</Items>" + "</PurchaseOrder>";

    @Test
    public void xmlUpdate() throws Exception
    {
        client.send("vm://input-10", sampleXml, null);
        FlowAssert.verify("process-order-update");
    }

    @Test
    public void xmlUpdateByteArray() throws Exception
    {
        byte[] xmlba = sampleXml.getBytes();
        client.send("vm://input-10", xmlba, null);
        FlowAssert.verify("process-order-update");
    }

    @Ignore("MULE-9285")
    @Test
    public void xmlUpdateInputStream() throws Exception
    {
        InputStream xmlis = new ByteArrayInputStream(sampleXml.getBytes());
        client.send("vm://input-10-is", xmlis, null);
        FlowAssert.verify("process-order-update-is");
    }

    @Test
    public void xmlUpdateMel() throws Exception
    {
        client.send("vm://input-10-mel", sampleXml, null);
        FlowAssert.verify("process-order-update-mel");
    }

    @Test
    public void jsonUpdate() throws Exception
    {
        String json = "{\"order\": {\"name\": \"Ellen\", \"email\": \"ellen@mail.com\", \"items\": [{\"key1\": \"value1\"}, {\"key2\": \"value2\"}] } }";
        client.send("vm://input-11", json, null);
        FlowAssert.verify("process-json-update");
    }

    @Test
    public void arrayPayload() throws Exception
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
    public void variableScope() throws Exception
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
    public void twoOneAfterAnother() throws Exception
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

        assertThat(result.getInboundProperty("msg-total-messages"), is(3));
    }

    @Test
    public void nestedConfig() throws Exception
    {
        final List<List<String>> payload = createNestedPayload();
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
    public void nestedCounters() throws Exception
    {
        final List<List<String>> payload = createNestedPayload();
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
                assertEquals("The nested counters are not consistent.", (Object) out.getInboundProperty("j"), j+1);
            }
            out = client.request("vm://out", getTestTimeoutSecs());
            assertThat("The nested counters are not consistent", out.getInboundProperty("i"), is(i +1));
        }
    }

    private List<List<String>> createNestedPayload()
    {
        final List<List<String>> payload = new ArrayList<>();
        final List<String> elem1 = new ArrayList<String>();
        final List<String> elem2 = new ArrayList<String>();
        final List<String> elem3 = new ArrayList<String>();
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
    public void propertiesRestored() throws Exception
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
    public void mvelList() throws Exception
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
    public void mvelMap() throws Exception
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
    public void mvelCollection() throws Exception
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
    public void mvelArray() throws Exception
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
    public void requestReply() throws Exception
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
