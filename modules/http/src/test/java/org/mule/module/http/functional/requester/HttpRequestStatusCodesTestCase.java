/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.construct.Flow;
import org.mule.module.http.internal.request.ResponseValidatorException;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.junit.Test;


public class HttpRequestStatusCodesTestCase extends AbstractHttpRequestTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "http-request-status-codes-config.xml";
    }

    @Test
    public void defaultStatusCodeValidatorSuccess() throws Exception
    {
        assertSuccess(200, "default");
    }

    @Test
    public void defaultStatusCodeValidatorFailure() throws Exception
    {
        assertFailure(500, "default");
    }

    @Test
    public void successStatusCodeValidatorSuccess() throws Exception
    {
        assertSuccess(409, "success");
    }

    @Test
    public void successStatusCodeValidatorFailure() throws Exception
    {
        assertFailure(200, "success");
    }

    @Test
    public void failureStatusCodeValidatorSuccess() throws Exception
    {
        assertSuccess(200, "failure");
    }

    @Test
    public void failureStatusCodeValidatorFailure() throws Exception
    {
        assertFailure(201, "failure");
    }


    private void assertSuccess(int statusCode, String flowName) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct(flowName);
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        event.getMessage().setInvocationProperty("code", statusCode);
        flow.process(event);

    }

    private void assertFailure(int statusCode, String flowName) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct(flowName);
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        event.getMessage().setInvocationProperty("code", statusCode);

        try
        {
            flow.process(event);
            fail();
        }
        catch (MessagingException e)
        {
            MuleMessage response = e.getEvent().getMessage();
            assertNotNull(response.getExceptionPayload());
            assertTrue(response.getExceptionPayload().getException() instanceof ResponseValidatorException);
        }
    }


    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        int statusCode = Integer.parseInt(request.getParameter("code"));

        response.setContentType("text/html");
        response.setStatus(statusCode);
        response.getWriter().print(DEFAULT_RESPONSE);
    }
}
