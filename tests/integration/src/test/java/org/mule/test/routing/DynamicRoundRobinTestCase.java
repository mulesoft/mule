/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleEvent;
import org.mule.construct.Flow;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class DynamicRoundRobinTestCase extends DynamicRouterTestCase
{
    private static final String DYNAMIC_ROUND_ROBIN = "dynamicRoundRobin";
    private static final String DYNAMIC_ROUND_ROBIN_CUSTOM_ID = "dynamicRoundRobinWithCustomId";
    private static final String MULTIPLE_ROUND_ROBIN = "multipleDynamicRoundRobin";
    private static final String MULTIPLE_ROUND_ROBIN_WITH_ID = "multipleDynamicRoundRobinWithId";
    private static final String ID = "id";
    private static final String FIRST_ROUTER_VAR = "first-router";
    private static final boolean FIRST_ROUTER = true;
    private static final int ID_1 = 1;
    private static final int ID_2 = 2;
    private static final int ID_3 = 3;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/dynamic-round-robin-config.xml";
    }

    @Test
    public void withRoutes() throws Exception
    {
        initCustomResolver();
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
        initIdentifiableCustomRouteResolver();
        Flow testFlow = getTestFlow(DYNAMIC_ROUND_ROBIN_CUSTOM_ID);
        runFlowAndAssertResponse(testFlow, Collections.<String, Object>singletonMap(ID, ID_1), LETTER_A);
        runFlowAndAssertResponse(testFlow, Collections.<String, Object>singletonMap(ID, ID_1), LETTER_B);
        runFlowAndAssertResponse(testFlow, Collections.<String, Object>singletonMap(ID, ID_2), LETTER_A);
        runFlowAndAssertResponse(testFlow, Collections.<String, Object>singletonMap(ID, ID_1), LETTER_C);
        runFlowAndAssertResponse(testFlow, Collections.<String, Object>singletonMap(ID, ID_2), LETTER_B);
        runFlowAndAssertResponse(testFlow, Collections.<String, Object>singletonMap(ID, ID_3), LETTER_A);
        runFlowAndAssertResponse(testFlow, Collections.<String, Object>singletonMap(ID, ID_1), LETTER_D);
        runFlowAndAssertResponse(testFlow, Collections.<String, Object>singletonMap(ID, ID_2), LETTER_C);
    }

    @Test
    public void testMultipleDynamicRouters() throws Exception
    {
        initCustomResolver();
        Flow testFlow = getTestFlow(MULTIPLE_ROUND_ROBIN);
        runFlowAndAssertResponse(testFlow, Collections.<String, Object>singletonMap(FIRST_ROUTER_VAR, FIRST_ROUTER), LETTER_A);
        runFlowAndAssertResponse(testFlow, Collections.<String, Object>singletonMap(FIRST_ROUTER_VAR, FIRST_ROUTER), LETTER_B);
        runFlowAndAssertResponse(testFlow, Collections.<String, Object>singletonMap(FIRST_ROUTER_VAR, !FIRST_ROUTER), LETTER_A);
        runFlowAndAssertResponse(testFlow, Collections.<String, Object>singletonMap(FIRST_ROUTER_VAR, FIRST_ROUTER), LETTER_C);
        runFlowAndAssertResponse(testFlow, Collections.<String, Object>singletonMap(FIRST_ROUTER_VAR, !FIRST_ROUTER), LETTER_B);
    }

    @Test
    public void testMultipleDynamicRoutersWithId() throws Exception
    {
        initIdentifiableCustomRouteResolver();
        Flow testFlow = getTestFlow(MULTIPLE_ROUND_ROBIN_WITH_ID);
        runFlowAndAssertResponse(testFlow, getFlowVarsMap(FIRST_ROUTER, ID_1), LETTER_A);
        runFlowAndAssertResponse(testFlow, getFlowVarsMap(FIRST_ROUTER, ID_1), LETTER_B);
        runFlowAndAssertResponse(testFlow, getFlowVarsMap(FIRST_ROUTER, ID_2), LETTER_A);
        runFlowAndAssertResponse(testFlow, getFlowVarsMap(!FIRST_ROUTER, ID_1), LETTER_A);
        runFlowAndAssertResponse(testFlow, getFlowVarsMap(!FIRST_ROUTER, ID_2), LETTER_A);
        runFlowAndAssertResponse(testFlow, getFlowVarsMap(!FIRST_ROUTER, ID_1), LETTER_B);
    }

    private Map<String, Object> getFlowVarsMap(boolean firstRouter, int id)
    {
        Map<String, Object> flowVars = new HashMap<String, Object>();
        flowVars.put(FIRST_ROUTER_VAR, firstRouter);
        flowVars.put(ID, id);
        return flowVars;
    }

    private void initCustomResolver()
    {
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_A));
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_B));
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_C));
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_D));
    }

    private void initIdentifiableCustomRouteResolver()
    {
        IdentifiableCustomRouteResolver.routes.add(new IdentifiableCustomRouteResolver.AddLetterMessageProcessor(LETTER_A));
        IdentifiableCustomRouteResolver.routes.add(new IdentifiableCustomRouteResolver.AddLetterMessageProcessor(LETTER_B));
        IdentifiableCustomRouteResolver.routes.add(new IdentifiableCustomRouteResolver.AddLetterMessageProcessor(LETTER_C));
        IdentifiableCustomRouteResolver.routes.add(new IdentifiableCustomRouteResolver.AddLetterMessageProcessor(LETTER_D));
    }

    private MuleEvent runFlowAndAssertResponse(Flow flow, Map<String, Object> flowVars, Object expectedMessage) throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        if(flowVars != null)
        {
            for(String key : flowVars.keySet())
            {
                event.setFlowVariable(key, flowVars.get(key));
            }
        }
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
