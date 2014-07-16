/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.WorkManager;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;

@RunWith(MockitoJUnitRunner.class)
public class HttpMessageProcessTemplateTestCase
{
    public static final String ENCODING = "UTF-8";

    public static final String PAYLOAD = "TEST PAYLOAD";

    public static final String ROOT_MESSAGE_ID = "myRootMessageId";

    public static final String CLIENT_ADDRESS = "1.1.1.1";

    public static final String PROXY_1_ADDRESS = "2.2.2.2";

    public static final String PROXY_2_ADDRESS = "3.3.3.3";

    public static final String CLIENT_ONLY_X_FORWARDED_FOR = CLIENT_ADDRESS;

    public static final String ONE_PROXY_X_FORWARDED_FOR = CLIENT_ADDRESS + "," + PROXY_1_ADDRESS;

    public static final String TWO_PROXY_X_FORWARDED_FOR = CLIENT_ADDRESS + "," + PROXY_1_ADDRESS + "," + PROXY_2_ADDRESS;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    HttpMessageReceiver messageReceiver;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    HttpServerConnection httpServerConnection;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    WorkManager flowExecutionWorkManager;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    MuleContext context;

    DefaultMuleMessage message;

    @Before
    public void prepare() throws MuleException
    {
        message = new DefaultMuleMessage(PAYLOAD, context);
        message.setInboundProperty(MuleProperties.MULE_ROOT_MESSAGE_ID_PROPERTY, ROOT_MESSAGE_ID);
        message.setInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY, "/");

        when(messageReceiver.getEndpoint().getEncoding()).thenReturn(ENCODING);
        when(messageReceiver.createMuleMessage(any(), anyString())).thenReturn(message);
        when(messageReceiver.getEndpoint().getEndpointURI().getAddress()).thenReturn("http://127.0.0.1/");
    }

    @Test
    public void checkCreateMessageFromSourceWithoutXForwardedFor() throws MuleException
    {
        when(httpServerConnection.getRemoteClientAddress()).thenReturn(CLIENT_ADDRESS);

        HttpMessageProcessTemplate template = new HttpMessageProcessTemplate(messageReceiver, httpServerConnection);
        MuleMessage retMessage = template.createMessageFromSource(PAYLOAD);

        assertThat(retMessage, is(notNullValue()));
        assertThat(retMessage.getPayload(), is(notNullValue()));
        assertThat(retMessage.<String>getInboundProperty(MuleProperties.MULE_REMOTE_CLIENT_ADDRESS), is(equalTo(CLIENT_ADDRESS)));
        assertThat(retMessage.<String>getInboundProperty(MuleProperties.MULE_PROXY_ADDRESS), is(nullValue()));
    }

    @Test
    public void checkCreateMessageFromSourceWithXForwardedForClientOnly() throws MuleException
    {
        when(httpServerConnection.getRemoteClientAddress()).thenReturn(PROXY_1_ADDRESS);

        HttpMessageProcessTemplate template = new HttpMessageProcessTemplate(messageReceiver, httpServerConnection);
        message.setInboundProperty(HttpConstants.HEADER_X_FORWARDED_FOR, CLIENT_ONLY_X_FORWARDED_FOR);

        MuleMessage retMessage = template.createMessageFromSource(PAYLOAD);
        assertThat(retMessage, is(notNullValue()));
        assertThat(retMessage.getPayload(), is(notNullValue()));
        assertThat(retMessage.<String>getInboundProperty(MuleProperties.MULE_REMOTE_CLIENT_ADDRESS), is(equalTo(CLIENT_ADDRESS)));
        assertThat(retMessage.<String>getInboundProperty(MuleProperties.MULE_PROXY_ADDRESS), is(equalTo(PROXY_1_ADDRESS)));
    }

    @Test
    public void checkCreateMessageFromSourceWithXForwardedForOneProxy() throws MuleException
    {
        when(httpServerConnection.getRemoteClientAddress()).thenReturn(PROXY_1_ADDRESS);

        HttpMessageProcessTemplate template = new HttpMessageProcessTemplate(messageReceiver, httpServerConnection);
        message.setInboundProperty(HttpConstants.HEADER_X_FORWARDED_FOR, ONE_PROXY_X_FORWARDED_FOR);

        MuleMessage retMessage = template.createMessageFromSource(PAYLOAD);
        assertThat(retMessage, is(notNullValue()));
        assertThat(retMessage.getPayload(), is(notNullValue()));
        assertThat(retMessage.<String>getInboundProperty(MuleProperties.MULE_REMOTE_CLIENT_ADDRESS), is(equalTo(CLIENT_ADDRESS)));
        assertThat(retMessage.<String>getInboundProperty(MuleProperties.MULE_PROXY_ADDRESS), is(equalTo(PROXY_1_ADDRESS)));
    }

    @Test
    public void checkCreateMessageFromSourceWithXForwardedForTwoProxy() throws MuleException
    {
        when(httpServerConnection.getRemoteClientAddress()).thenReturn(PROXY_2_ADDRESS);

        HttpMessageProcessTemplate template = new HttpMessageProcessTemplate(messageReceiver, httpServerConnection);
        message.setInboundProperty(HttpConstants.HEADER_X_FORWARDED_FOR, TWO_PROXY_X_FORWARDED_FOR);

        MuleMessage retMessage = template.createMessageFromSource(PAYLOAD);
        assertThat(retMessage, is(notNullValue()));
        assertThat(retMessage.getPayload(), is(notNullValue()));
        assertThat(retMessage.<String>getInboundProperty(MuleProperties.MULE_REMOTE_CLIENT_ADDRESS), is(equalTo(CLIENT_ADDRESS)));
        assertThat(retMessage.<String>getInboundProperty(MuleProperties.MULE_PROXY_ADDRESS), is(equalTo(PROXY_2_ADDRESS)));
    }

}
