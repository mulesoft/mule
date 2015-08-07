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

    private static final String MOVED_URI = "/testPath";
    private static final String REDIRECT_URI = "/redirect";
    private static final String REDIRECT_WITH_PARAMS_URI = REDIRECT_URI + "?param1=value1&param2=value2";

    private MuleEvent testEvent;
    private boolean addParams = false;

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
    protected boolean enableHttps()
    {
        return true;
    }

    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        extractBaseRequestParts(baseRequest);
        if(baseRequest.getUri().getPath().startsWith("/redirect"))
        {
            response.getWriter().print(REDIRECTED);
        }
        else
        {
            response.setHeader("Location", String.format("http://localhost:%s%s", httpPort.getNumber(), getRedirectUri()));
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            response.getWriter().print(MOVED);
        }
    }

    private String getRedirectUri()
    {
        return addParams ? REDIRECT_WITH_PARAMS_URI : REDIRECT_URI;
    }

    @Test
    public void followRedirectsByDefault() throws Exception
    {
        testRedirect("default", REDIRECTED, REDIRECT_URI);
    }

    @Test
    public void followRedirectsHttps() throws Exception
    {
        testRedirect("followRedirectsHttps", REDIRECTED, REDIRECT_URI);
    }

    @Test
    public void followRedirectsTrueInRequestElement() throws Exception
    {
        testRedirect("followRedirects", REDIRECTED, REDIRECT_URI);
    }

    @Test
    public void followRedirectsFalseInRequestElement() throws Exception
    {
        testRedirect("dontFollowRedirects", MOVED, MOVED_URI);
    }

    @Test
    public void followRedirectsWithBooleanExpression() throws Exception
    {
        testRedirectExpression("followRedirectsExpression", MOVED, MOVED_URI, false);
    }

    @Test
    public void followRedirectsWithStringExpression() throws Exception
    {
        testRedirectExpression("followRedirectsExpression",MOVED, MOVED_URI, "false");
    }

    @Test
    public void followRedirectsFalseInRequestConfigElement() throws Exception
    {
        testRedirect("fromConfig", MOVED, MOVED_URI);
    }

    @Test
    public void followRedirectsOverride() throws Exception
    {
        testRedirect("overrideConfig", REDIRECTED, REDIRECT_URI);
    }

    @Test
    public void followRedirectsExpressionInRequestConfigElement() throws Exception
    {
        testRedirectExpression("fromConfigExpression", REDIRECTED, REDIRECT_URI, true);
    }

    @Test
    public void followRedirectsWithParamsByDefault() throws Exception
    {
        addParams = true;
        testRedirect("default", REDIRECTED, REDIRECT_WITH_PARAMS_URI);
    }

    @Test
    public void followRedirectsWithParamsHttps() throws Exception
    {
        addParams = true;
        testRedirect("followRedirectsHttps", REDIRECTED, REDIRECT_WITH_PARAMS_URI);
    }

    private void testRedirectExpression(String flowName, String expectedPayload, String expectedPath, Object flowVar) throws Exception
    {
        testEvent.setFlowVariable(FLOW_VAR_KEY, flowVar);
        testRedirect(flowName, expectedPayload, expectedPath);
    }

    private void testRedirect(String flowName, String expectedPayload, String expectedPath) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct(flowName);
        MuleEvent result = flow.process(testEvent);
        assertThat(result.getMessage().getPayloadAsString(), is(expectedPayload));
        assertThat(uri, is(expectedPath));
    }
}
