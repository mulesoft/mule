/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.properties;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.api.routing.filter.FilterUnacceptedException;
import org.mule.functional.junit4.FlowRunner;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

public class InvocationPropertiesTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Rule
    public DynamicPort dynamicPort3 = new DynamicPort("port3");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/properties/invocation-properties-config.xml";
    }

    @Test
    public void setInvocationPropertyUsingAPIGetInFlow() throws Exception
    {
        FlowRunner runner = flowRunner("GetInvocationPropertyInFlow").withPayload("data")
                                                                     .withFlowVariable("P1", "P1_VALUE")
                                                                     .asynchronously();
        runner.run();
    }

    @Test
    public void setInvocationPropertyInFlowGetUsingAPI() throws Exception
    {
        FlowRunner runner = flowRunner("SetInvocationPropertyInFlow").withPayload("data").asynchronously();
        MuleEvent event = runner.buildEvent();
        runner.run();

        assertThat(event.getFlowVariable("P1"), is("P1_VALUE"));
    }

    @Test
    public void overwritePropertyValueInFlow() throws Exception
    {
        FlowRunner runner = flowRunner("OverwritePropertyValueInFlow").withPayload("data")
                                                                      .withFlowVariable("P1", "P1_VALUE")
                                                                      .asynchronously();
        MuleEvent event = runner.buildEvent();
        runner.run();
        
        assertThat(event.getFlowVariable("P1"), is("P1_VALUE_NEW"));
    }

    @Test
    public void propagationInSameFlow() throws Exception
    {
        flowRunner("propagationInSameFlow").asynchronously().run();
    }

    @Test
    public void noPropagationInDifferentFlowHttp() throws Exception
    {
        flowRunner("noPropagationInDifferentFlowHttp").asynchronously().runAndVerify("noPropagationInDifferentFlowHttp-2");
    }

    @Test
    public void propagationThroughOneWayFlowSedaQueue() throws Exception
    {
        Object nonSerializable = new Object();
        FlowRunner runner = flowRunner("AsyncFlow").withPayload("data")
                                                   .withFlowVariable("P1", "value")
                                                   .withFlowVariable("P2", nonSerializable)
                                                   .withFlowVariable("testThread", Thread.currentThread())
                                                   .asynchronously();
        MuleEvent event = runner.buildEvent();
        runner.run();

        assertNotNull(event.getFlowVariable("P1"));
        assertNotNull(event.getFlowVariable("P2"));
        assertNull(event.getFlowVariable("P3"));
    }

    @Test
    public void propagationWithHTTPRequestResponseOutboundEndpointMidFlow() throws Exception
    {
        flowRunner("HTTPRequestResponseEndpointFlowMidFlow").asynchronously().run();
    }

    @Test
    public void propagationThroughFlowRefToFlow() throws Exception
    {
        flowRunner("propagationThroughFlowRefToFlow").asynchronously().runAndVerify("FlowRef-1", "FlowRef-2", "FlowRef-3");
    }

    @Test
    public void overwritePropertyValueInFlowViaFlowRef() throws Exception
    {
        flowRunner("OverwriteInFlowRef").asynchronously().run();
    }

    @Test
    public void propagationThroughFlowRefToSubFlow() throws Exception
    {
        flowRunner("propagationThroughFlowRefToSubFlow").asynchronously().run();
    }

    @Test
    public void overwritePropertyValueInSubFlowViaFlowRef() throws Exception
    {
        flowRunner("OverwriteInSubFlowRef").asynchronously().run();
    }

    @Test
    public void propagationThroughAsyncElement() throws Exception
    {
        flowRunner("propagationThroughAsyncElement").asynchronously().run();
    }

    @Test
    public void propertyAddedInAsyncElementNotAddedinFlow() throws Exception
    {
        flowRunner("propertyAddedInAsyncElementNotAddedinFlow").asynchronously().run();
    }

    @Test
    public void propagationThroughWireTap() throws Exception
    {
        flowRunner("propagationThroughWireTap").asynchronously().run();
    }

    @Test
    public void propertyAddedInWireTapNotAddedinFlow() throws Exception
    {
        flowRunner("propertyAddedInWireTapNotAddedinFlow").asynchronously().run();
    }

    @Test
    public void propagationThroughEnricher() throws Exception
    {
        flowRunner("propagationThroughEnricher").asynchronously().run();
    }

    @Test
    public void propertyAddedInEnricherNotAddedinFlow() throws Exception
    {
        flowRunner("propertyAddedInEnricherNotAddedinFlow").asynchronously().run();
    }

    @Test
    /** Router drops invocation properties **/
    public void propagateToRoutesInAll() throws Exception
    {
        flowRunner("propagateToRoutesInAll").asynchronously().run();
    }

    @Test
    public void propagateThroughAllRouterWithResults() throws Exception
    {
        flowRunner("propagateThroughAllRouterWithResults").asynchronously().run();
    }

    @Test
    public void propagateThroughAllRouterWithNoResults() throws Exception
    {
        flowRunner("propagateThroughAllRouterWithNoResults").asynchronously().run();
    }

    @Test
    public void propagateBetweenRoutes() throws Exception
    {
        flowRunner("propagateBetweenRoutes").asynchronously().run();
    }

    @Test
    public void propagateFromRouteToNextProcessorSingleRoute() throws Exception
    {
        flowRunner("propagateFromRouteToNextProcessorSingleRoute").asynchronously().run();
    }

    @Test
    public void propagateFromRouteToNextProcessorMultipleRoutes() throws Exception
    {
        flowRunner("propagateFromRouteToNextProcessorMultipleRoutes").asynchronously().run();
    }

    @Test
    public void propagateFromRouteToNextProcessorNoResult() throws Exception
    {
        flowRunner("propagateFromRouteToNextProcessorNoResult").asynchronously().run();
    }

    @Test
    public void allAsync() throws Exception
    {
        flowRunner("AllAsync").asynchronously().run();
    }

    @Test
    public void propogationOfPropertiesInMessageSplitWithSplitter() throws Exception
    {
        List<Fruit> fruitList = new ArrayList<Fruit>();
        fruitList.add(new Apple());
        fruitList.add(new Orange());
        fruitList.add(new Banana());
        flowRunner("propogationOfPropertiesInMessageSplitWithSplitter").withPayload(fruitList).run();
    }

    @Test
    public void aggregationOfPropertiesFromMultipleMessageWithAggregator() throws Exception
    {
        List<Fruit> fruitList = new ArrayList<Fruit>();
        fruitList.add(new Apple());
        fruitList.add(new Orange());
        fruitList.add(new Banana());
        flowRunner("aggregationOfPropertiesFromMultipleMessageWithAggregator").withPayload(fruitList).runAndVerify("Split");
    }

    @Test
    public void defaultExceptionStrategy() throws Exception
    {
        flowRunner("defaultExceptionStrategy").asynchronously().run();
    }

    @Test
    public void catchExceptionStrategy() throws Exception
    {
        flowRunner("catchExceptionStrategy").asynchronously().run();
    }

    @Test
    public void defaultExceptionStrategyAfterCallSubflow() throws Exception
    {
        Exception e = flowRunner("defaultExceptionStrategyAfterCallingSubflow").withPayload(TEST_PAYLOAD).runExpectingException();
        assertThat(e, instanceOf(FilterUnacceptedException.class));
    }

}
