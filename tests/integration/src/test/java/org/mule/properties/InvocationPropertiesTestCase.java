/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.Flow;
import org.mule.tck.functional.FlowAssert;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class InvocationPropertiesTestCase extends org.mule.tck.junit4.FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @After
    public void clearFlowAssertions()
    {
        FlowAssert.reset();
    }

    @Test
    public void setInvocationPropertyUsingAPIGetInFlow() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());

        message.setProperty("P1", "P1_VALUE", PropertyScope.INVOCATION);

        testFlow("GetInvocationPropertyInFlow", event);
    }

    @Test
    public void setInvocationPropertyInFlowGetUsingAPI() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());

        Flow flowA = (Flow) muleContext.getRegistry().lookupFlowConstruct("SetInvocationPropertyInFlow");
        MuleEvent result = flowA.process(event);

        assertEquals("P1_VALUE", result.getMessage().getProperty("P1", PropertyScope.INVOCATION));
    }

    @Test
    public void sameFlow() throws Exception
    {
        testFlow("SameFlow");
    }

    @Test
    public void differentFlowVMRR() throws Exception
    {
        testFlow("DifferentFlowVMRR");
        FlowAssert.verify("DifferentFlowVMRR-2");
    }

    @Test
    public void differentFlowVMOW() throws Exception
    {
        testFlow("DifferentFlowVMOW");
        FlowAssert.verify("DifferentFlowVMOW-2");
    }

    @Test
    public void differentFlowHTTP() throws Exception
    {
        testFlow("DifferentFlowHTTP");
        FlowAssert.verify("DifferentFlowHTTP-2");
    }

    @Test
    public void asyncOneWayFlow() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());

        Object nonSerializable = new Object();
        message.setInvocationProperty("P1", "value");
        message.setInvocationProperty("P2", nonSerializable);
        message.setInvocationProperty("testThread", Thread.currentThread());

        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("AsyncFlow");
        flow.process(event);

        FlowAssert.verify("AsyncFlow");

        assertNull(message.getInvocationProperty("P3"));
    }

    @Test
    public void vmRequestResponseOutboundEndpointMidFlow() throws Exception
    {
        testFlow("VMRequestResponseEndpointFlowMidFlow");
    }

    @Test
    @Ignore
    public void httpRequestResponseOutboundEndpointMidFlow() throws Exception
    {
        testFlow("HTTPRequestResponseEndpointFlowMidFlow");
    }

    @Test
    public void flowRef() throws Exception
    {
        testFlow("FlowRef");
        FlowAssert.verify("FlowRef-1");
        FlowAssert.verify("FlowRef-2");
        FlowAssert.verify("FlowRef-3");
    }

    @Test
    public void subFlowRef() throws Exception
    {
        testFlow("SubFlowRef");
    }

    @Test
    public void async() throws Exception
    {
        testFlow("Async");
    }

    @Test
    public void wireTap() throws Exception
    {
        testFlow("WireTap");
    }

    @Test
    public void enricher() throws Exception
    {
        testFlow("Enricher");
    }

    @Test
    @Ignore
    /** Router drops invocation properties **/
    public void all() throws Exception
    {
        testFlow("All");
    }

    @Test
    @Ignore
    /** Router drops invocation properties **/
    public void allAsync() throws Exception
    {
        testFlow("AllAsync");
    }

    @Test
    public void split() throws Exception
    {
        List<Fruit> fruitList = new ArrayList<Fruit>();
        fruitList.add(new Apple());
        fruitList.add(new Orange());
        fruitList.add(new Banana());
        testFlow("Split", getTestEvent(fruitList));
    }

    @Test
    @Ignore
    /** Aggregator drops invocation properties **/
    public void aggregate() throws Exception
    {
        List<Fruit> fruitList = new ArrayList<Fruit>();
        fruitList.add(new Apple());
        fruitList.add(new Orange());
        fruitList.add(new Banana());
        testFlow("Aggregate", getTestEvent(fruitList));
        FlowAssert.verify("Split");
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/session/invocation-properties-config.xml";
    }

}
