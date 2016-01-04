/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleEvent;
import org.mule.construct.Flow;

import org.junit.Test;

public class HttpRequestSendBodyTestCase extends AbstractHttpRequestTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "http-request-send-body-config.xml";
    }

    @Test
    public void sendBodyAutoSendsPayloadPost() throws Exception
    {
        assertNotEmptyBody("sendBodyAuto", TEST_MESSAGE, "POST");
    }

    @Test
    public void sendBodyAutoIgnoresPayloadGet() throws Exception
    {
        assertEmptyBody("sendBodyAuto", TEST_MESSAGE, "GET");
    }

    @Test
    public void sendBodyAutoIgnoresNullPayloadPost() throws Exception
    {
        assertEmptyBody("sendBodyAuto", null, "POST");
    }

    @Test
    public void sendBodyNeverIgnoresPayloadPost() throws Exception
    {
        assertEmptyBody("sendBodyNever", TEST_MESSAGE, "POST");
    }
    @Test
    public void sendBodyNeverIgnoresNullPayloadPost() throws Exception
    {
        assertEmptyBody("sendBodyNever", null, "POST");
    }

    @Test
    public void sendBodyAlwaysSendsPayloadGet() throws Exception
    {
        assertNotEmptyBody("sendBodyAlways", TEST_MESSAGE, "GET");
    }

    @Test
    public void sendBodyAlwaysIgnoresNullPayloadGet() throws Exception
    {
        assertEmptyBody("sendBodyAlways", null, "GET");
    }

    private void assertEmptyBody(String flowName, Object payload, String method) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct(flowName);
        MuleEvent event = getTestEvent(payload);
        event.getMessage().setInvocationProperty("method", method);

        flow.process(event);

        assertThat(body, equalTo(""));
        assertThat(headers.containsKey("Content-Length"), is(false));
    }

    private void assertNotEmptyBody(String flowName, Object payload, String method) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct(flowName);
        MuleEvent event = getTestEvent(payload);
        event.getMessage().setInvocationProperty("method", method);

        flow.process(event);

        assertThat(body, equalTo(TEST_MESSAGE));
        assertThat(headers.containsKey("Content-Length"), is(true));
        assertThat(getFirstReceivedHeader("Content-Length"), is(String.valueOf(TEST_MESSAGE.length())));
    }
}
