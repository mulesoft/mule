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

import org.mule.api.DefaultMuleException;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.construct.Flow;
import org.mule.routing.DynamicRouteResolver;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class DynamicFirstSuccessfulTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/dynamic-first-successful-config.xml";
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
        runFlowAndAssertResponse(getTestFlow(),"a");
    }

    @Test
    public void worksWithFirstFailingRouteAndSecondGood() throws Exception
    {
        CustomRouteResolver.routes.add(new CustomRouteResolver.FailingMessageProcessor());
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor("b"));
        runFlowAndAssertResponse(getTestFlow(),"b");
    }

    @Test(expected = MessagingException.class)
    public void worksWithAllFailingProcessor() throws Exception
    {
        CustomRouteResolver.routes.add(new CustomRouteResolver.FailingMessageProcessor());
        CustomRouteResolver.routes.add(new CustomRouteResolver.FailingMessageProcessor());
        runFlowAndAssertResponse(getTestFlow(),"b");
    }

    @Test
    public void allRoutesReceiveSameMessage() throws Exception
    {
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterTHenFailsMessageProcessor("a"));
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor("b"));
        runFlowAndAssertResponse(getTestFlow(),"b");
    }

    @Test
    public void failureExpressionNotFailingNotMatchingExpression() throws Exception
    {
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor("a"));
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor("b"));
        runFlowAndAssertResponse(getTestFlowWithExpression(),"a");
    }

    @Test
    public void failureExpressionNotFailingButMatchingExpression() throws Exception
    {
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor("f"));
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor("b"));
        runFlowAndAssertResponse(getTestFlowWithExpression(),"b");
    }

    @Test(expected = MessagingException.class)
    public void failureExpressionFailingAndMatchingExpression() throws Exception
    {
        CustomRouteResolver.routes.add(new CustomRouteResolver.FailingMessageProcessor());
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor("f"));
        runFlowAndAssertResponse(getTestFlowWithExpression(), "doesnotmatter");
    }

    @Test(expected = MessagingException.class)
    public void allFailingExpression() throws Exception
    {
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor("fofo"));
        CustomRouteResolver.routes.add(new CustomRouteResolver.AddLetterMessageProcessor("fafa"));
        runFlowAndAssertResponse(getTestFlowWithExpression(),"doesnotmatter");
    }

    private Flow getTestFlow() throws Exception
    {
        return (Flow) getFlowConstruct("dynamicFirstSuccessful");
    }

    private Flow getTestFlowWithExpression() throws Exception
    {
        return (Flow) getFlowConstruct("dynamicFirstSuccessfulWithExpression");
    }

    private MuleEvent runFlowAndAssertResponse(Flow flow, Object expectedMessage) throws Exception
    {
        MuleEvent event = flow.process(getTestEvent(""));
        assertThat(event.getMessageAsString(), is(expectedMessage));
        return event;
    }

}
