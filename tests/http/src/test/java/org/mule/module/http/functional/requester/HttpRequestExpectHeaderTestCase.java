/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.HttpStatus.EXPECTATION_FAILED;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import static org.mule.module.http.api.HttpHeaders.Names.EXPECT;
import static org.mule.module.http.api.HttpHeaders.Values.CONTINUE;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.module.http.functional.AbstractHttpExpectHeaderServerTestCase;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;


public class HttpRequestExpectHeaderTestCase extends AbstractHttpExpectHeaderServerTestCase
{

    private static final String REQUEST_FLOW_NAME = "requestFlow";

    @Override
    protected String getConfigFile()
    {
        return "http-request-expect-header-config.xml";
    }

    @Test
    public void handlesContinueResponse() throws Exception
    {
        startExpectContinueServer();

        Flow flow = (Flow) getFlowConstruct(REQUEST_FLOW_NAME);
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        event.getMessage().setOutboundProperty(EXPECT, CONTINUE);

        flow.process(event);
        assertThat(requestBody, equalTo(TEST_MESSAGE));

        stopServer();
    }

    @Test
    public void handlesExpectationFailedResponse() throws Exception
    {
        startExpectFailedServer();

        Flow flow = (Flow) getFlowConstruct(REQUEST_FLOW_NAME);

        // Set a payload that will fail when consumed. As the server rejects the request after processing
        // the header, the client should not send the body.

        MuleEvent event = getTestEvent(new InputStream()
        {
            @Override
            public int read() throws IOException
            {
                throw new IOException("Payload should not be consumed");
            }
        });
        event.getMessage().setOutboundProperty(EXPECT, CONTINUE);

        MuleEvent response = flow.process(event);
        assertThat(response.getMessage().<Integer>getInboundProperty(HTTP_STATUS_PROPERTY),
                   equalTo(EXPECTATION_FAILED.getStatusCode()));

        stopServer();
    }

}
