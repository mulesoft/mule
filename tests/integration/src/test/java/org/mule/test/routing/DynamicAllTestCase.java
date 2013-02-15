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

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

public class DynamicAllTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/dynamic-all-config.xml";
    }

    @Before
    public void clearRoutes()
    {
        CustomRouteResolver.routes.clear();
    }

    @Test(expected = MessagingException.class)
    public void noRoutes() throws Exception
    {
        Flow flow = getTestFlow();
        flow.process(getTestEvent("message"));
    }

    @Test
    public void withRoutes() throws Exception
    {
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor("a"));
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor("b"));
        runFlowAndAssertResponse(getTestFlow(),"a","b");
    }

    @Test(expected = MessagingException.class)
    public void worksWithFirstFailingRouteAndSecondGood() throws Exception
    {
        CustomRouteResolver.routes.add(new CustomRouteResolver.FailingMessageProcessor());
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor("b"));
        runFlowAndAssertResponse(getTestFlow(),"doesnotmatter");
    }

    @Test(expected = MessagingException.class)
    public void worksWithFirstRouteGoodAndSecondFails() throws Exception
    {
        CustomRouteResolver.routes.add(new CustomRouteResolver.FailingMessageProcessor());
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor("b"));
        runFlowAndAssertResponse(getTestFlow(),"doesnotmatter");
    }

    @Test
    public void oneRoute() throws Exception
    {
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor("a"));
        MuleEvent result = getTestFlow().process(getTestEvent(""));
        assertThat(result.getMessage().getPayloadAsString(), is("a"));
    }

    @Test
    public void oneRouteWithCustomResultAggregator() throws Exception
    {
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor("a"));
        runFlowAndAssertResponse((Flow) getFlowConstruct("dynamicAllResultAggregator"),getTestEvent(""),"a");
    }


    private Flow getTestFlow() throws Exception
    {
        return (Flow) getFlowConstruct("dynamicAll");
    }

    private MuleEvent runFlowAndAssertResponse(Flow flow, String... letters) throws Exception
    {
        return runFlowAndAssertResponse(flow, getTestEvent(""), letters);
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
