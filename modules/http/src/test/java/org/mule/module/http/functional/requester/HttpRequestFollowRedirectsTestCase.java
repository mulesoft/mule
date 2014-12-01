/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HttpRequestFollowRedirectsTestCase extends AbstractHttpRequestTestCase
{
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final String REDIRECTED = "Redirected.";
    private static final String MOVED = "Moved.";
    private static final String FLOW_VAR_KEY = "redirect";

    private MuleEvent testEvent;

    @Before
    public void setUp() throws Exception
    {
        testEvent = getTestEvent(TEST_MESSAGE);
    }

    @Override
    protected String getConfigFile()
    {
        return "http-request-follow-redirects-config.xml";
    }

    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if(baseRequest.getUri().getPath().equals("/redirect"))
        {
            response.getWriter().print(REDIRECTED);
        }
        else
        {
            response.setHeader("Location", String.format("http://localhost:%s/redirect", httpPort.getNumber()));
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            response.getWriter().print(MOVED);
        }
    }

    @Test
    public void followRedirectsByDefault() throws Exception
    {
        testRedirect("default", REDIRECTED);
    }

    @Test
    public void followRedirectsTrueInRequestElement() throws Exception
    {
        testRedirect("followRedirects", REDIRECTED);
    }

    @Test
    public void followRedirectsFalseInRequestElement() throws Exception
    {
        testRedirect("dontFollowRedirects", MOVED);
    }

    @Test
    public void followRedirectsWithBooleanExpression() throws Exception
    {
        testRedirectExpression("followRedirectsExpression", MOVED, false);
    }

    @Test
    public void followRedirectsWithStringExpression() throws Exception
    {
        testRedirectExpression("followRedirectsExpression",MOVED, "false");
    }

    @Test
    public void followRedirectsFalseInRequestConfigElement() throws Exception
    {
        testRedirect("fromConfig", MOVED);
    }

    @Test
    public void followRedirectsOverride() throws Exception
    {
        testRedirect("overrideConfig", REDIRECTED);
    }

    @Test
    public void followRedirectsExpressionInRequestConfigElement() throws Exception
    {
        testRedirectExpression("fromConfigExpression", MOVED, false);
    }

    private void testRedirectExpression(String flowName, String expectedPayload, Object flowVar) throws Exception
    {
        testEvent.setFlowVariable(FLOW_VAR_KEY, flowVar);
        testRedirect(flowName, expectedPayload);
    }

    private void testRedirect(String flowName, String expectedPayload) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct(flowName);
        MuleEvent result = flow.process(testEvent);
        assertThat(result.getMessage().getPayloadAsString(), is(expectedPayload));
    }
}
