/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpListenerMethodRoutingTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");

    @Rule
    public SystemProperty path = new SystemProperty("path", "path");

    private final String method;
    private final String expectedContent;

    @Parameterized.Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {{"GET", "GET"}, {"POST", "POST"}, {"OPTIONS", "OPTIONS-DELETE"}, {"DELETE", "OPTIONS-DELETE"}, {"PUT", "ALL"}});
    }

    public HttpListenerMethodRoutingTestCase(String method, String expectedContent)
    {
        this.method = method;
        this.expectedContent = expectedContent;
    }

    @Override
    protected String getConfigFile()
    {
        return "http-listener-method-routing-config.xml";
    }

    @Test
    public void callWithMethod() throws Exception
    {
        sendRequestAndAssertMethod(TEST_MESSAGE);
        assertThat(muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT).getPayloadAsString(), equalTo(TEST_MESSAGE));
    }

    @Test
    public void callWithMethodEmptyBody() throws Exception
    {
        sendRequestAndAssertMethod(null);
    }

    private void sendRequestAndAssertMethod(String payload) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("requestFlow");
        MuleEvent event = getTestEvent(payload);
        event.setFlowVariable("method", method);
        event = flow.process(event);

        assertThat(event.getMessage().<Integer>getInboundProperty(HTTP_STATUS_PROPERTY), is(OK.getStatusCode()));
        assertThat(event.getMessageAsString(), is(expectedContent));
    }

}
