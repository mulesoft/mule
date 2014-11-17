/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.http.api.HttpConstants;
import org.mule.module.http.api.HttpHeaders;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.http.api.requester.HttpRequesterConfig;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeoutException;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HttpRequestWithMuleClientTestCase extends FunctionalTestCase
{

    public static final String PUT_HTTP_METHOD = "PUT";
    @Rule
    public DynamicPort port = new DynamicPort("port");
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    protected String getConfigFile()
    {
        return "http-request-mule-client-config.xml";
    }

    @Test
    public void dispatchRequestUseNewConnectorByDefault() throws MuleException
    {
        muleContext.getClient().dispatch(getUrl(), new DefaultMuleMessage(TEST_MESSAGE, muleContext));
        final MuleMessage vmMessage = getMessageReceivedByFlow();
        assertThat(vmMessage.getPayload(), Is.<Object>is(NullPayload.getInstance().toString()));
    }

    @Ignore //TODO see MULE-8049
    @Test
    public void dispatchHttpPostRequestWithStreamingEnabled() throws Exception
    {
        muleContext.getClient().dispatch(getUrl(), new DefaultMuleMessage(new ByteArrayInputStream(TEST_MESSAGE.getBytes()), muleContext), newOptions().method("POST").build());
        final MuleMessage vmMessage = getMessageReceivedByFlow();
        assertThat(vmMessage, notNullValue());
        assertThat(vmMessage.getPayloadAsString(), is(TEST_MESSAGE));
        assertThat(vmMessage.getInboundProperty(HttpHeaders.Names.TRANSFER_ENCODING), Is.<Object>is(HttpHeaders.Values.CHUNKED));
    }

    @Test
    public void dispatchWithStreamingDisabled() throws Exception
    {
        final HttpRequestOptions options = newOptions().method(PUT_HTTP_METHOD).neverStreamRequest().build();
        muleContext.getClient().dispatch(getUrl(), new DefaultMuleMessage(TEST_MESSAGE, muleContext), options);
        final MuleMessage vmMessage = getMessageReceivedByFlow();
        assertThat(vmMessage.getInboundProperty(HttpHeaders.Names.TRANSFER_ENCODING), nullValue());
        assertThat(vmMessage.getInboundProperty(HttpHeaders.Names.CONTENT_LENGTH), Is.<Object>is("12"));
    }

    @Ignore //TODO see MULE-8049
    @Test
    public void sendHttpPutMethod() throws Exception
    {
        final MuleMessage response = muleContext.getClient().send(getUrl(), new DefaultMuleMessage(TEST_MESSAGE, muleContext), newOptions().method(PUT_HTTP_METHOD).build());
        assertThat(response.getPayloadAsString(), is(TEST_MESSAGE));
        final MuleMessage vmMessage = getMessageReceivedByFlow();
        assertThat(vmMessage.getPayloadAsString(), is(TEST_MESSAGE));
        assertThat(vmMessage.getInboundProperty(HttpConstants.RequestProperties.HTTP_METHOD_PROPERTY), Is.<Object>is(PUT_HTTP_METHOD));
    }

    @Test
    public void sendDisableRedirect() throws Exception
    {
        final MuleMessage response = muleContext.getClient().send(getRedirectUrl(), new DefaultMuleMessage(NullPayload.class, muleContext), newOptions().method(PUT_HTTP_METHOD).disableFollowsRedirect().build());
        assertThat(response.getPayloadAsString(), is("test-response"));
    }

    @Test
    public void sendEnableRedirect() throws Exception
    {
        final MuleMessage response = muleContext.getClient().send(getRedirectUrl(), new DefaultMuleMessage(NullPayload.class, muleContext), newOptions().enableFollowsRedirect().build());
        assertThat(response.getPayloadAsString(), is(NullPayload.getInstance().toString()));
    }

    @Test
    public void setWithTimeout() throws Exception
    {
        expectedException.expectCause(IsInstanceOf.<Throwable>instanceOf(TimeoutException.class));
        muleContext.getClient().send(getTimeoutUrl(), new DefaultMuleMessage(NullPayload.class, muleContext), newOptions().responseTimeout(100).build());
    }

    @Test
    public void sendDisableRedirectByRequestConfig() throws Exception
    {
        final DefaultMuleMessage message = new DefaultMuleMessage(NullPayload.class, muleContext);
        final HttpRequestOptions options = newOptions().method(PUT_HTTP_METHOD).requestConfig(getRequestConfig()).build();
        final MuleMessage response = muleContext.getClient().send(getRedirectUrl(), message, options);
        assertThat(response.getPayloadAsString(), is("test-response"));
    }

    @Test
    public void disableStatusCodeValidation() throws Exception
    {
        final DefaultMuleMessage message = new DefaultMuleMessage(NullPayload.class, muleContext);
        final HttpRequestOptions options = newOptions().disableStatusCodeValidation().build();
        final MuleMessage response = muleContext.getClient().send(getFailureUrl(), message, options);
        assertThat(response.getInboundProperty(HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY), Is.<Object>is(500));
    }

    private MuleMessage getMessageReceivedByFlow() throws MuleException
    {
        return muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);
    }

    private HttpRequesterConfig getRequestConfig()
    {
        return muleContext.getRegistry().get("requestConfig");
    }

    private String getUrl()
    {
        return String.format("http://localhost:%s/path", port.getNumber());
    }

    private String getRedirectUrl()
    {
        return String.format("http://localhost:%s/redirectPath", port.getNumber());
    }

    private String getTimeoutUrl()
    {
        return String.format("http://localhost:%s/timeoutPath", port.getNumber());
    }

    private String getFailureUrl()
    {
        return String.format("http://localhost:%s/failurePath", port.getNumber());
    }
}
