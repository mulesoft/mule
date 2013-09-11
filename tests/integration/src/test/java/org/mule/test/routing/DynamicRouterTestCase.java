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
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

public abstract class DynamicRouterTestCase extends FunctionalTestCase
{

    protected static final String LETTER_A = "a";
    protected static final String LETTER_B = "b";
    protected static final String LETTER_C = "c";
    protected static final String LETTER_D = "d";
    protected static final String DOES_NOT_MATTER = "doesnotmatter";

    @Before
    public void clearRoutes()
    {
        CustomRouteResolver.routes.clear();
    }

    @Test(expected = MessagingException.class)
    public void noRoutes() throws Exception
    {
        Flow flow = getTestFlow(getFlowName());
        flow.process(getTestEvent(TEST_MESSAGE));
    }

    public abstract String getFlowName();

    protected MuleEvent runFlowAndAssertResponse(Flow flow, Object expectedMessage) throws Exception
    {
        MuleEvent event = flow.process(getTestEvent(TEST_MESSAGE));
        assertThat(event.getMessageAsString(), is(expectedMessage));
        return event;
    }

    protected Flow getTestFlow(String flow) throws Exception
    {
        return (Flow) getFlowConstruct(flow);
    }
}
