/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Names.EXPECT;
import static org.mule.module.http.api.HttpHeaders.Values.CONTINUE;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.module.http.functional.AbstractHttpExpectHeaderServerTestCase;

@RunWith(Parameterized.class)
public class HttpRequestExpectHeaderSuccessServerTestCase extends AbstractHttpExpectHeaderServerTestCase
{

    private static final String REQUEST_FLOW_NAME = "requestFlow";

    private boolean persistentConnection;
    
    public HttpRequestExpectHeaderSuccessServerTestCase(boolean persistentConnection) {
        super(persistentConnection);
        this.persistentConnection = persistentConnection;
    }

    
    @Override
    protected String getConfigFile()
    {
        return "http-request-expect-success-header-config.xml";
    }
    
    @Parameterized.Parameters
    public static List<Object> getParameters()
    {
        return Arrays.asList(new Object[] {
                                             true,
                                             false
        });
    }

    @Test
    public void handlesContinueResponse() throws Exception
    {
        startExpectContinueServer(persistentConnection);

        Flow flow = (Flow) getFlowConstruct(REQUEST_FLOW_NAME);
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        event.getMessage().setOutboundProperty(EXPECT, CONTINUE);

        flow.process(event);
        assertThat(requestBody, equalTo(TEST_MESSAGE));

        stopServer();
    }
    
    @Test
    public void handlesContinueResponseWithoutRequestInHeaderField() throws Exception
    {
        startExpectContinueServer(persistentConnection);

        Flow flow = (Flow) getFlowConstruct(REQUEST_FLOW_NAME);
        MuleEvent event = getTestEvent(TEST_MESSAGE);

        flow.process(event);
        assertThat(requestBody, equalTo(TEST_MESSAGE));

        stopServer();
    }

}
