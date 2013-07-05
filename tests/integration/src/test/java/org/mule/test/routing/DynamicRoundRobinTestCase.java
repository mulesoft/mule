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

import org.junit.Before;
import org.junit.Test;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class DynamicRoundRobinTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/dynamic-round-robin-config.xml";
    }

    @Before
    public void clearRoutes()
    {
        CustomRouteResolver.routes.clear();
    }

    @Test(expected = MessagingException.class)
    public void noRoutes() throws Exception
    {
        Flow flow = getTestFlow("dynamicRoundRobin");
        flow.process(getTestEvent("message"));
    }

    @Test
    public void withRoutes() throws Exception
    {
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor("a"));
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor("b"));
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor("c"));
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor("d"));
        Flow testFlow = getTestFlow("dynamicRoundRobin");
        runFlowAndAssertResponse(testFlow,"a");
        runFlowAndAssertResponse(testFlow,"b");
        runFlowAndAssertResponse(testFlow,"c");
        runFlowAndAssertResponse(testFlow,"d");
        runFlowAndAssertResponse(testFlow,"a");
        runFlowAndAssertResponse(testFlow,"b");
        runFlowAndAssertResponse(testFlow,"c");
        runFlowAndAssertResponse(testFlow,"d");
    }

    @Test
    public void withRoutesAndCustomId() throws Exception
    {
        IdentifiableCustomRouteResolver.routes.add(new IdentifiableCustomRouteResolver.AddLetterMessageProcessor("a"));
        IdentifiableCustomRouteResolver.routes.add(new IdentifiableCustomRouteResolver.AddLetterMessageProcessor("b"));
        IdentifiableCustomRouteResolver.routes.add(new IdentifiableCustomRouteResolver.AddLetterMessageProcessor("c"));
        IdentifiableCustomRouteResolver.routes.add(new IdentifiableCustomRouteResolver.AddLetterMessageProcessor("d"));
        Flow testFlow = getTestFlow("dynamicRoundRobinWithCustomId");
        runFlowAndAssertResponse(testFlow, 1, "a");
        runFlowAndAssertResponse(testFlow, 1, "b");
        runFlowAndAssertResponse(testFlow, 2, "a");
        runFlowAndAssertResponse(testFlow, 1, "c");
        runFlowAndAssertResponse(testFlow, 2, "b");
        runFlowAndAssertResponse(testFlow, 3, "a");
        runFlowAndAssertResponse(testFlow, 1, "d");
        runFlowAndAssertResponse(testFlow, 2, "c");
    }

    private Flow getTestFlow(String flow) throws Exception
    {
        return (Flow) getFlowConstruct(flow);
    }

    private MuleEvent runFlowAndAssertResponse(Flow flow, Object expectedMessage) throws Exception
    {
        MuleEvent event = flow.process(getTestEvent(""));
        assertThat(event.getMessageAsString(), is(expectedMessage));
        return event;
    }

    private MuleEvent runFlowAndAssertResponse(Flow flow, Object flowVar, Object expectedMessage) throws Exception
    {
        MuleEvent event = getTestEvent("");
        event.setFlowVariable("id", flowVar);
        MuleEvent response = flow.process(event);
        assertThat(response.getMessageAsString(), is(expectedMessage));
        return response;
    }


}
