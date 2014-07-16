/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.construct.Flow;

import org.junit.Test;

public class DynamicAllTestCase extends DynamicRouterTestCase
{
    private static final String DYNAMIC_ALL = "dynamicAll";

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/dynamic-all-config.xml";
    }

    @Override
    public String getFlowName()
    {
        return "dynamicAll";
    }

    @Test
    public void withRoutes() throws Exception
    {
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_A));
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_B));
        runFlowAndAssertResponse(getTestFlow(DYNAMIC_ALL), LETTER_A, LETTER_B);
    }

    @Test(expected = MessagingException.class)
    public void worksWithFirstFailingRouteAndSecondGood() throws Exception
    {
        CustomRouteResolver.routes.add(new CustomRouteResolver.FailingMessageProcessor());
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_B));
        runFlowAndAssertResponse(getTestFlow(DYNAMIC_ALL), DOES_NOT_MATTER);
    }

    @Test(expected = MessagingException.class)
    public void worksWithFirstRouteGoodAndSecondFails() throws Exception
    {
        CustomRouteResolver.routes.add(new CustomRouteResolver.FailingMessageProcessor());
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_B));
        runFlowAndAssertResponse(getTestFlow(DYNAMIC_ALL), DOES_NOT_MATTER);
    }

    @Test
    public void oneRoute() throws Exception
    {
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_A));
        MuleEvent result = getTestFlow(DYNAMIC_ALL).process(getTestEvent(TEST_MESSAGE));
        assertThat(result.getMessage().getPayloadAsString(), is(LETTER_A));
    }

    @Test
    public void oneRouteWithCustomResultAggregator() throws Exception
    {
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor(LETTER_A));
        runFlowAndAssertResponse((Flow) getFlowConstruct("dynamicAllResultAggregator"), getTestEvent(TEST_MESSAGE), LETTER_A);
    }

    private MuleEvent runFlowAndAssertResponse(Flow flow, String... letters) throws Exception
    {
        return runFlowAndAssertResponse(flow, getTestEvent(TEST_MESSAGE), letters);
    }

    private MuleEvent runFlowAndAssertResponse(Flow flow, MuleEvent event, String... letters) throws Exception
    {
        MuleEvent resultEvent = flow.process(event);
        MuleMessageCollection messageCollection = (MuleMessageCollection) resultEvent.getMessage();
        for (int i = 0; i < letters.length; i++)
        {
            MuleMessage message = messageCollection.getMessage(i);
            assertThat(message.getPayloadAsString(), is(letters[i]));
        }
        return resultEvent;
    }

}
