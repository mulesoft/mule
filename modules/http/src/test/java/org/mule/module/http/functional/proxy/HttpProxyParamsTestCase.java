/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.functional.proxy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.api.MuleEvent;
import org.mule.module.http.functional.requester.AbstractHttpRequestTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.net.ConnectException;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.Is.isA;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;

// TODO - MULE-9563 - improve this suite starting a proxy-server and checking that the request went through
public class HttpProxyParamsTestCase extends AbstractHttpRequestTestCase
{

    @Rule
    public DynamicPort proxyPort = new DynamicPort("proxyPort");
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    protected String getConfigFile() {
        return "http-request-proxy-params-config.xml";
    }

    @Test
    public void proxyWithNonProxyHostsParam() throws Exception
    {
        final MuleEvent event = runFlow("nonProxyParamProxy");
        assertThat((int) event.getMessage().getInboundProperty(HTTP_STATUS_PROPERTY), is(SC_OK));
        assertThat(event.getMessage().getPayloadAsString(), equalTo(DEFAULT_RESPONSE));
    }

    @Test
    public void innerProxyWithNonProxyHostsParam() throws Exception
    {
        final MuleEvent event = runFlow("innerNonProxyParamProxy");
        assertThat((int) event.getMessage().getInboundProperty(HTTP_STATUS_PROPERTY), is(SC_OK));
        assertThat(event.getMessage().getPayloadAsString(), equalTo(DEFAULT_RESPONSE));
    }

    @Test
    public void proxyWithMultipleHostsNonProxyHostsParam() throws Exception
    {
        final MuleEvent event = runFlow("innerNonProxyParamProxyMultipleHosts");
        assertThat((int) event.getMessage().getInboundProperty(HTTP_STATUS_PROPERTY), is(SC_OK));
        assertThat(event.getMessage().getPayloadAsString(), equalTo(DEFAULT_RESPONSE));
    }

    @Test
    public void proxyWithoutNonProxyHostsParam() throws Exception
    {
        expectedException.expectCause(isA(ConnectException.class));
        expectedException.expectMessage(containsString("Error sending HTTP request"));
        runFlow("refAnonymousProxy");
        fail("Request should fail as there is no proxy configured");
    }

    @Test
    public void proxyWithAnotherHostNonProxyHostsParam() throws Exception
    {
        expectedException.expectCause(isA(ConnectException.class));
        expectedException.expectMessage(containsString("Error sending HTTP request"));
        runFlow("innerNonProxyParamProxyAnotherHost");
        fail("Request should fail as there is no proxy configured");
    }

    @Test
    public void noProxy() throws Exception
    {
        final MuleEvent event = runFlow("noProxy");
        assertThat((int) event.getMessage().getInboundProperty(HTTP_STATUS_PROPERTY), is(SC_OK));
        assertThat(event.getMessage().getPayloadAsString(), equalTo(DEFAULT_RESPONSE));
    }

}
