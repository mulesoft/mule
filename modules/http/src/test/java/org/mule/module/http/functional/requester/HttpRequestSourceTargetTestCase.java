/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.transport.NullPayload;
import org.mule.util.IOUtils;

import java.io.InputStream;

import org.junit.Test;


public class HttpRequestSourceTargetTestCase extends AbstractHttpRequestTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "http-request-source-target-config.xml";
    }

    @Test
    public void requestBodyFromPayloadSource() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("payloadSourceFlow");
        flow.process(getTestEvent(TEST_MESSAGE));
        assertThat(body, equalTo(TEST_MESSAGE));
    }

    @Test
    public void requestBodyFromCustomSource() throws Exception
    {
        sendRequestFromCustomSourceAndAssertResponse(TEST_MESSAGE);
    }

    @Test
    public void requestBodyFromCustomSourceAndNullPayload() throws Exception
    {
        sendRequestFromCustomSourceAndAssertResponse(NullPayload.getInstance());
    }

    private void sendRequestFromCustomSourceAndAssertResponse(Object payload) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("customSourceFlow");
        MuleEvent event = getTestEvent(payload);
        event.setFlowVariable("customSource", "customValue");
        flow.process(event);
        assertThat(body, equalTo("customValue"));
    }

    @Test
    public void responseBodyToPayloadTarget() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("payloadTargetFlow");
        MuleEvent event = flow.process(getTestEvent(TEST_MESSAGE));
        assertThat(event.getMessage().getPayloadAsString(), equalTo(DEFAULT_RESPONSE));
    }

    @Test
    public void responseBodyToCustomTarget() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("customTargetFlow");
        MuleEvent event = flow.process(getTestEvent(TEST_MESSAGE));
        InputStream customTarget = event.getMessage().getOutboundProperty("customTarget");
        assertThat(customTarget, notNullValue());
        assertThat(IOUtils.toString(customTarget), equalTo(DEFAULT_RESPONSE));
        assertThat(event.getMessage().getPayloadAsString(), equalTo(TEST_MESSAGE));
    }

}
