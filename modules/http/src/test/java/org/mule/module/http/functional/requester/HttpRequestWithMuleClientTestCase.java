/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTPS;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import static org.mule.module.http.api.requester.HttpStreamingType.NEVER;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.http.api.HttpConstants;
import org.mule.module.http.api.HttpHeaders;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.http.api.requester.HttpRequesterConfig;
import org.mule.module.http.api.requester.HttpRequesterConfigBuilder;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;
import org.mule.transport.ssl.api.TlsContextFactory;
import org.mule.util.concurrent.Latch;

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
    private static final long RESPONSE_TIMEOUT = 100;
    private static final long SERVER_TIMEOUT = 2000;
    public static final String TEST_RESPONSE = "test-response";

    @Rule
    public DynamicPort port = new DynamicPort("port");
    @Rule
    public DynamicPort httpsPort = new DynamicPort("httpsPort");
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
        muleContext.getClient().dispatch(getUrl(), getTestMuleMessage());
        final MuleMessage vmMessage = getMessageReceivedByFlow();
        assertThat(vmMessage.getPayload(), Is.<Object>is(NullPayload.getInstance().toString()));
    }

    @Ignore("See MULE-8049")
    @Test
    public void dispatchHttpPostRequestWithStreamingEnabled() throws Exception
    {
        muleContext.getClient().dispatch(getUrl(), getTestMuleMessage(new ByteArrayInputStream(TEST_MESSAGE.getBytes())), newOptions().method("POST").build());
        final MuleMessage vmMessage = getMessageReceivedByFlow();
        assertThat(vmMessage, notNullValue());
        assertThat(vmMessage.getPayloadAsString(), is(TEST_MESSAGE));
        assertThat(vmMessage.getInboundProperty(HttpHeaders.Names.TRANSFER_ENCODING), Is.<Object>is(HttpHeaders.Values.CHUNKED));
    }

    @Test
    public void dispatchWithStreamingDisabled() throws Exception
    {
        final HttpRequestOptions options = newOptions().method(PUT_HTTP_METHOD).requestStreamingMode(NEVER).build();
        muleContext.getClient().dispatch(getUrl(), getTestMuleMessage(TEST_MESSAGE), options);
        final MuleMessage vmMessage = getMessageReceivedByFlow();
        assertThat(vmMessage.getInboundProperty(HttpHeaders.Names.TRANSFER_ENCODING), nullValue());
        assertThat(vmMessage.getInboundProperty(HttpHeaders.Names.CONTENT_LENGTH), Is.<Object>is("12"));
    }

    @Ignore("See MULE-8049")
    @Test
    public void sendHttpPutMethod() throws Exception
    {
        final MuleMessage response = muleContext.getClient().send(getUrl(), getTestMuleMessage(TEST_MESSAGE), newOptions().method(PUT_HTTP_METHOD).build());
        assertThat(response.getPayloadAsString(), is(TEST_MESSAGE));
        final MuleMessage vmMessage = getMessageReceivedByFlow();
        assertThat(vmMessage.getPayloadAsString(), is(TEST_MESSAGE));
        assertThat(vmMessage.getInboundProperty(HttpConstants.RequestProperties.HTTP_METHOD_PROPERTY), Is.<Object>is(PUT_HTTP_METHOD));
    }

    @Test
    public void sendDisableRedirect() throws Exception
    {
        final MuleMessage response = muleContext.getClient().send(getRedirectUrl(), getTestMuleMessage(NullPayload.class), newOptions().method(PUT_HTTP_METHOD).disableFollowsRedirect().build());
        assertThat(response.getPayloadAsString(), is("test-response"));
    }

    @Test
    public void sendEnableRedirect() throws Exception
    {
        final MuleMessage response = muleContext.getClient().send(getRedirectUrl(), getTestMuleMessage(NullPayload.class), newOptions().enableFollowsRedirect().build());
        assertThat(response.getPayloadAsString(), is(NullPayload.getInstance().toString()));
    }

    @Test
    public void setWithTimeout() throws Exception
    {
        expectedException.expectCause(IsInstanceOf.<Throwable>instanceOf(TimeoutException.class));
        try
        {
            muleContext.getClient().send(getTimeoutUrl(), getTestMuleMessage(NullPayload.class), newOptions().responseTimeout(RESPONSE_TIMEOUT).build());
        }
        finally
        {
            LatchMessageProcessor.latch.release();
        }
    }

    @Test
    public void sendDisableRedirectByRequestConfig() throws Exception
    {
        final MuleMessage message = getTestMuleMessage(NullPayload.class);
        final HttpRequestOptions options = newOptions().method(PUT_HTTP_METHOD).requestConfig(getRequestConfig()).build();
        final MuleMessage response = muleContext.getClient().send(getRedirectUrl(), message, options);
        assertThat(response.getPayloadAsString(), is(TEST_RESPONSE));
    }

    @Test
    public void disableStatusCodeValidation() throws Exception
    {
        final MuleMessage message = getTestMuleMessage(NullPayload.class);
        final HttpRequestOptions options = newOptions().disableStatusCodeValidation().build();
        final MuleMessage response = muleContext.getClient().send(getFailureUrl(), message, options);
        assertThat(response.getInboundProperty(HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY), Is.<Object>is(500));
    }

    @Test
    public void customRequestConfig() throws Exception
    {
        final MuleMessage message = getTestMuleMessage(NullPayload.class);
        final HttpRequesterConfig requestConfig = new HttpRequesterConfigBuilder(muleContext).setProtocol(HTTPS).setTlsContext(muleContext.getRegistry().<TlsContextFactory>get("tlsContext")).build();
        final HttpRequestOptions options = newOptions().disableStatusCodeValidation().requestConfig(requestConfig).build();
        final MuleMessage response = muleContext.getClient().send(format("https://localhost:%s/", httpsPort.getNumber()), message, options);
        assertThat(response.getInboundProperty(HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY), Is.<Object>is(200));
        assertThat(response.getPayloadAsString(), is(TEST_RESPONSE));
    }

    public static class LatchMessageProcessor implements MessageProcessor
    {
        public static Latch latch = new Latch();

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            try
            {
                latch.await(SERVER_TIMEOUT, MILLISECONDS);
            }
            catch (InterruptedException e)
            {
                throw new DefaultMuleException(e);
            }
            return event;
        }
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
        return format("http://localhost:%s/path", port.getNumber());
    }

    private String getRedirectUrl()
    {
        return format("http://localhost:%s/redirectPath", port.getNumber());
    }

    private String getTimeoutUrl()
    {
        return format("http://localhost:%s/timeoutPath", port.getNumber());
    }

    private String getFailureUrl()
    {
        return format("http://localhost:%s/failurePath", port.getNumber());
    }
}
