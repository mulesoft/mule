/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.api.LocatedMuleException.INFO_LOCATION_KEY;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.PropertyScope;
import org.mule.functional.functional.FlowAssert;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

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

        MuleMessage result = flowRunner("minimal-config").withPayload(payload).run().getMessage();
        assertThat(result.getPayload(), instanceOf(Collection.class));
        Collection<?> resultPayload = (Collection<?>) result.getPayload();
        assertThat(resultPayload, hasSize(2));
        assertSame(payload, resultPayload);

        MuleMessage out = client.request("test://out", getTestTimeoutSecs());
        assertThat(out.getPayload(), instanceOf(String.class));
        assertThat(out.getPayload(), is("julio"));

        out = client.request("test://out", getTestTimeoutSecs());
        assertThat(out.getPayload(), instanceOf(String.class));
        assertThat(out.getPayload(), is("sosa"));
    }

    @Test
    public void defaultConfigurationPlusMP() throws Exception
    {
        final Collection<String> payload = new ArrayList<String>();
        payload.add("syd");
        payload.add("barrett");

        MuleMessage result = flowRunner("minimal-config-plus-mp").withPayload(payload).run().getMessage();
        assertThat(result.getPayload(), instanceOf(Collection.class));
        Collection<?> resultPayload = (Collection<?>) result.getPayload();
        assertThat(resultPayload, hasSize(3));
        assertSame(payload, resultPayload);

        MuleMessage out = client.request("test://out", getTestTimeoutSecs());
        assertThat(out.getPayload(), instanceOf(String.class));
        assertThat(out.getPayload(), is("syd"));

        out = client.request("test://out", getTestTimeoutSecs());
        assertThat(out.getPayload(), instanceOf(String.class));
        assertThat(out.getPayload(), is("barrett"));
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

        MuleMessage result = flowRunner("minimal-config-expression").withPayload("message payload")
                                                                    .withInboundProperties(props)
                                                                    .run()
                                                                    .getMessage();

        assertThat(result.getPayload(), instanceOf(String.class));
        assertThat(((Collection<?>) message.getOutboundProperty("names")), hasSize(names.size()));

        MuleMessage out = client.request("test://out", getTestTimeoutSecs());
        assertThat(out.getPayload(), instanceOf(String.class));
        assertThat(out.getPayload(), is("residente"));

        out = client.request("test://out", getTestTimeoutSecs());
        assertThat(out.getPayload(), instanceOf(String.class));
        assertThat(out.getPayload(), is("visitante"));
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

        MuleMessage result = flowRunner("partitioned-config").withPayload(payload).run().getMessage();
        assertThat(result.getPayload(), instanceOf(Collection.class));
        Collection<?> resultPayload = (Collection<?>) result.getPayload();
        assertThat(resultPayload, hasSize(5));
        assertSame(payload, resultPayload);

        MuleMessage out = client.request("test://out", getTestTimeoutSecs());
        assertThat(out.getPayload(), instanceOf(Collection.class));
        Collection<?> outPayload = (Collection<?>) out.getPayload();
        assertThat(outPayload, hasSize(3));

        out = client.request("test://out", getTestTimeoutSecs());
        assertThat(out.getPayload(), instanceOf(Collection.class));
        outPayload = (Collection<?>) out.getPayload();
        assertThat(outPayload, hasSize(2));
    }

    @Test
    public void rootMessageConfiguration() throws Exception
    {
        final Collection<String> payload = new ArrayList<String>();
        payload.add("pyotr");
        payload.add("ilych");

        MuleMessage result = flowRunner("parent-message-config").withPayload(payload).run().getMessage();
        assertThat(result.getPayload(), instanceOf(Collection.class));
        Collection<?> resultPayload = (Collection<?>) result.getPayload();
        assertThat(resultPayload, hasSize(2));
        assertSame(payload, resultPayload);

        assertSame(payload, ((MuleMessage) result.getOutboundProperty("parent")).getPayload());
    }

    @Test
    public void counterConfiguration() throws Exception
    {
        final Collection<String> payload = new ArrayList<String>();
        payload.add("wolfgang");
        payload.add("amadeus");
        payload.add("mozart");

        MuleMessage result = flowRunner("counter-config").withPayload(payload).run().getMessage();
        assertThat(result.getPayload(), instanceOf(Collection.class));
        Collection<?> resultPayload = (Collection<?>) result.getPayload();
        assertThat(resultPayload, hasSize(3));
        assertSame(payload, resultPayload);

        assertThat(result.getOutboundProperty("msg-last-index"), is(3));
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
        MuleMessage result = flowRunner("message-collection-config").withPayload(msgCollection).run().getMessage();
        assertThat(result.getOutboundProperty("totalMessages"), is(10));
        assertThat(result.getPayload(), is(msgCollection.getPayload()));
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
        final String flowName = "message-collection-config-one-way";
        flowRunner(flowName).withPayload(list).run();
        FlowAssert.verify(flowName);
    }

    @Test
    public void mapPayload() throws Exception
    {
        final Map<String, String> payload = new HashMap<String, String>();
        payload.put("name", "david");
        payload.put("surname", "bowie");

        MuleMessage result = flowRunner("map-config").withPayload(payload).run().getMessage();
        assertThat(result.getPayload(), instanceOf(Map.class));
        Map<?, ?> resultPayload = (Map<?, ?>) result.getPayload();
        assertThat(resultPayload.entrySet(), hasSize(payload.size()));
        assertThat(result.getOutboundProperty("totalMessages"), is(payload.size()));
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

        MuleMessage result = flowRunner("map-expression-config").withPayload("message payload")
                                                                .withInboundProperties(props)
                                                                .run()
                                                                .getMessage();

        assertThat(result.getPayload(), instanceOf(String.class));
        assertThat(((Collection<?>) message.getOutboundProperty("names")), hasSize(names.size()));
        assertThat(result.getOutboundProperty("totalMessages"), is(names.size()));
    }

    static String sampleXml = "<PurchaseOrder>" + "<Address><Name>Ellen Adams</Name></Address>" + "<Items>"
                              + "<Item PartNumber=\"872-AA\"><Price>140</Price></Item>"
                        + "<Item PartNumber=\"926-AA\"><Price>35</Price></Item>" + "</Items>" + "</PurchaseOrder>";

    @Test
    public void xmlUpdate() throws Exception
    {
        xpath(sampleXml);
    }

    private void xpath(Object payload) throws Exception {
        MuleEvent result = flowRunner("process-order-update").withPayload(payload).run();
        int total = result.getFlowVariable("total");
        assertThat(total, is(greaterThan(0)));
    }

    @Ignore("MULE-9285")
    @Test
    public void xmlUpdateByteArray() throws Exception
    {
        xpath(sampleXml.getBytes());
    }

    @Test
    public void jsonUpdate() throws Exception
    {
        String json = "{\"order\": {\"name\": \"Ellen\", \"email\": \"ellen@mail.com\", \"items\": [{\"key1\": \"value1\"}, {\"key2\": \"value2\"}] } }";
        flowRunner("process-json-update").withPayload(json).run();
        FlowAssert.verify("process-json-update");
    }

    @Test
    public void arrayPayload() throws Exception
    {
        String[] payload = {"uno", "dos", "tres"};

        MuleMessage result = flowRunner("array-expression-config").withPayload(payload).run().getMessage();
        assertThat(result.getPayload(), instanceOf(String[].class));
        String[] resultPayload = (String[]) result.getPayload();
        assertThat(resultPayload, arrayWithSize(payload.length));
        assertSame(payload, resultPayload);
        FlowAssert.verify("array-expression-config");
    }

    @Test
    public void variableScope() throws Exception
    {
        final Collection<String> payload = new ArrayList<>();
        payload.add("pedro");
        payload.add("rodolfo");
        payload.add("roque");

        flowRunner("counter-scope").withPayload(payload).run();

        FlowAssert.verify("counter-scope");
    }

    @Test
    public void twoOneAfterAnother() throws Exception
    {
        final Collection<String> payload = new ArrayList<String>();
        payload.add("rosa");
        payload.add("maria");
        payload.add("florencia");

        MuleMessage result = flowRunner("counter-two-foreach-independence").withPayload(payload).run().getMessage();
        assertThat(result.getPayload(), instanceOf(Collection.class));
        Collection<?> resultPayload = (Collection<?>) result.getPayload();
        assertThat(resultPayload, hasSize(3));
        assertSame(payload, resultPayload);

        assertThat(result.getOutboundProperty("msg-total-messages"), is(3));
    }

    @Test
    public void nestedConfig() throws Exception
    {
        final List<List<String>> payload = createNestedPayload();

        MuleMessage result = flowRunner("nested-foreach").withPayload(payload).run().getMessage();
        assertThat(result.getPayload(), instanceOf(Collection.class));
        Collection<?> resultPayload = (Collection<?>) result.getPayload();
        assertThat(resultPayload, hasSize(3));
        assertSame(payload, resultPayload);

        MuleMessage out;
        for(int i = 0; i < payload.size(); i++)
        {
            for(int j = 0; j < payload.get(i).size(); j++)
            {
                out = client.request("test://out", getTestTimeoutSecs());
                assertThat(out.getPayload(), instanceOf(String.class));
                assertThat(out.getPayload(), is(payload.get(i).get(j)));
            }
        }
    }

    @Test
    public void nestedCounters() throws Exception
    {
        final List<List<String>> payload = createNestedPayload();

        MuleMessage result = flowRunner("nested-foreach-counters").withPayload(payload).run().getMessage();
        assertThat(result.getPayload(), instanceOf(Collection.class));
        Collection<?> resultPayload = (Collection<?>) result.getPayload();
        assertThat(resultPayload, hasSize(3));
        assertSame(payload, resultPayload);

        MuleMessage out;
        for(int i = 0; i < payload.size(); i++)
        {
            for(int j = 0; j < payload.get(i).size(); j++)
            {
                out = client.request("test://out", getTestTimeoutSecs());
                assertThat("The nested counters are not consistent.", out.getOutboundProperty("j"), is(j + 1));
            }
            out = client.request("test://out", getTestTimeoutSecs());
            assertThat("The nested counters are not consistent", out.getOutboundProperty("i"), is(i + 1));
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

        MuleMessage result = flowRunner("foreach-properties-restored").withPayload(payload).run().getMessage();
        assertThat(result.getPayload(), instanceOf(String[].class));
        String[] resultPayload = (String[]) result.getPayload();
        assertThat(resultPayload, arrayWithSize(payload.length));
        assertSame(payload, resultPayload);
        FlowAssert.verify("foreach-properties-restored");
    }

    @Test
    public void mvelList() throws Exception
    {
        runFlow("mvel-list");

        MuleMessage out = client.request("test://out", getTestTimeoutSecs());
        assertThat(out.getPayload(), instanceOf(String.class));
        String outPayload = (String) out.getPayload();
        assertThat(outPayload, is("foo"));

        out = client.request("test://out", getTestTimeoutSecs());
        assertThat(out.getPayload(), instanceOf(String.class));
        outPayload = (String) out.getPayload();
        assertThat(outPayload, is("bar"));
    }

    @Test
    public void mvelMap() throws Exception
    {
        runFlow("mvel-map");

        Map<String,String> m = new HashMap<String, String>();
        m.put("key1", "val1");
        m.put("key2", "val2");

        MuleMessage out = client.request("test://out", getTestTimeoutSecs());
        assertThat(out.getPayload(), instanceOf(String.class));
        String outPayload = (String) out.getPayload();
        assertTrue(m.containsValue(outPayload));

        out = client.request("test://out", getTestTimeoutSecs());
        assertThat(out.getPayload(), instanceOf(String.class));
        outPayload = (String) out.getPayload();
        assertTrue(m.containsValue(outPayload));
    }

    @Test
    public void mvelCollection() throws Exception
    {
        runFlow("mvel-collection");

        Map<String,String> m = new HashMap<String, String>();
        m.put("key1", "val1");
        m.put("key2", "val2");

        MuleMessage out = client.request("test://out", getTestTimeoutSecs());
        assertThat(out.getPayload(), instanceOf(String.class));
        String outPayload = (String) out.getPayload();
        assertTrue(m.containsValue(outPayload));

        out = client.request("test://out", getTestTimeoutSecs());
        assertThat(out.getPayload(), instanceOf(String.class));
        outPayload = (String) out.getPayload();
        assertTrue(m.containsValue(outPayload));
    }

    @Test
    public void mvelArray() throws Exception
    {
        runFlow("mvel-array");

        MuleMessage out = client.request("test://out", getTestTimeoutSecs());
        assertThat(out.getPayload(), instanceOf(String.class));
        String outPayload = (String) out.getPayload();

        assertThat(outPayload, is("foo"));
        FlowAssert.verify("mvel-array");

        out = client.request("test://out", getTestTimeoutSecs());
        assertThat(out.getPayload(), instanceOf(String.class));
        outPayload = (String) out.getPayload();
        assertThat(outPayload, is("bar"));
    }

    @Test
    public void mvelError() throws Exception
    {
        MessagingException me = flowRunner("mvel-error").runExpectingException();
        assertThat((String) me.getInfo().get(INFO_LOCATION_KEY), startsWith("/mvel-error/processors/0 @"));
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
        flowRunner("foreachWithAsync").withPayload(list).withFlowVariable("latch", latch).run();

        latch.await(10, TimeUnit.SECONDS);
    }

    @Test
    public void initializesForeachOnSubFLow() throws Exception
    {
        getSubFlow("sub-flow-with-foreach");
    }
}
