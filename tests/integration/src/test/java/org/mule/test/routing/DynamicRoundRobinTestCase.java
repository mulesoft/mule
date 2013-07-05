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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleEvent;
import org.mule.construct.Flow;

import org.junit.Test;

public class DynamicRoundRobinTestCase extends DynamicRouterTestCase
{

    private static final String DYNAMIC_ROUND_ROBIN = "dynamicRoundRobin";
    private static final String DYNAMIC_ROUND_ROBIN_CUSTOM_ID = "dynamicRoundRobinWithCustomId";

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/dynamic-round-robin-config.xml";
    }

    @Test
    public void withRoutes() throws Exception
    {
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_A));
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_B));
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_C));
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_D));
        Flow testFlow = getTestFlow(DYNAMIC_ROUND_ROBIN);
        runFlowAndAssertResponse(testFlow, LETTER_A);
        runFlowAndAssertResponse(testFlow, LETTER_B);
        runFlowAndAssertResponse(testFlow, LETTER_C);
        runFlowAndAssertResponse(testFlow, LETTER_D);
        runFlowAndAssertResponse(testFlow, LETTER_A);
        runFlowAndAssertResponse(testFlow, LETTER_B);
        runFlowAndAssertResponse(testFlow, LETTER_C);
        runFlowAndAssertResponse(testFlow, LETTER_D);
    }

    @Test
    public void withRoutesAndCustomId() throws Exception
    {
        IdentifiableCustomRouteResolver.routes.add(new IdentifiableCustomRouteResolver.AddLetterMessageProcessor(LETTER_A));
        IdentifiableCustomRouteResolver.routes.add(new IdentifiableCustomRouteResolver.AddLetterMessageProcessor(LETTER_B));
        IdentifiableCustomRouteResolver.routes.add(new IdentifiableCustomRouteResolver.AddLetterMessageProcessor(LETTER_C));
        IdentifiableCustomRouteResolver.routes.add(new IdentifiableCustomRouteResolver.AddLetterMessageProcessor(LETTER_D));
        Flow testFlow = getTestFlow(DYNAMIC_ROUND_ROBIN_CUSTOM_ID);
        runFlowAndAssertResponse(testFlow, 1, LETTER_A);
        runFlowAndAssertResponse(testFlow, 1, LETTER_B);
        runFlowAndAssertResponse(testFlow, 2, LETTER_A);
        runFlowAndAssertResponse(testFlow, 1, LETTER_C);
        runFlowAndAssertResponse(testFlow, 2, LETTER_B);
        runFlowAndAssertResponse(testFlow, 3, LETTER_A);
        runFlowAndAssertResponse(testFlow, 1, LETTER_D);
        runFlowAndAssertResponse(testFlow, 2, LETTER_C);
    }

    private MuleEvent runFlowAndAssertResponse(Flow flow, Object flowVar, Object expectedMessage) throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        event.setFlowVariable("id", flowVar);
        MuleEvent response = flow.process(event);
        assertThat(response.getMessageAsString(), is(expectedMessage));
        return response;
    }


    @Override
    public String getFlowName()
    {
        return DYNAMIC_ROUND_ROBIN;
    }
}
