/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.compatibility.transport.http.HttpConstants.HEADER_X_FORWARDED_FOR;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.context.WorkManager;

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HttpMessageProcessTemplateTestCase {

  public static final Charset ENCODING = UTF_8;

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

  MuleMessage message;

  @Before
  public void prepare() throws MuleException {
    message =
        MuleMessage.builder().payload(PAYLOAD).addInboundProperty(MuleProperties.MULE_ROOT_MESSAGE_ID_PROPERTY, ROOT_MESSAGE_ID)
            .addInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY, "/").build();

    when(messageReceiver.getEndpoint().getEncoding()).thenReturn(ENCODING);
    when(messageReceiver.createMuleMessage(any(), any())).thenAnswer(invocation -> message);
    when(messageReceiver.getEndpoint().getEndpointURI().getAddress()).thenReturn("http://127.0.0.1/");
  }

  @Test
  public void checkCreateMessageFromSourceWithoutXForwardedFor() throws MuleException {
    when(httpServerConnection.getRemoteClientAddress()).thenReturn(CLIENT_ADDRESS);

    HttpMessageProcessTemplate template = new HttpMessageProcessTemplate(messageReceiver, httpServerConnection);
    MuleMessage retMessage = template.createMessageFromSource(PAYLOAD);

    assertThat(retMessage, is(notNullValue()));
    assertThat(retMessage.getPayload(), is(notNullValue()));
    assertThat(retMessage.<String>getInboundProperty(MuleProperties.MULE_REMOTE_CLIENT_ADDRESS), is(equalTo(CLIENT_ADDRESS)));
    assertThat(retMessage.<String>getInboundProperty(MuleProperties.MULE_PROXY_ADDRESS), is(nullValue()));
  }

  @Test
  public void checkCreateMessageFromSourceWithXForwardedForClientOnly() throws MuleException {
    when(httpServerConnection.getRemoteClientAddress()).thenReturn(PROXY_1_ADDRESS);

    HttpMessageProcessTemplate template = new HttpMessageProcessTemplate(messageReceiver, httpServerConnection);
    message = MuleMessage.builder(message).addInboundProperty(HEADER_X_FORWARDED_FOR, CLIENT_ONLY_X_FORWARDED_FOR).build();

    MuleMessage retMessage = template.createMessageFromSource(PAYLOAD);
    assertThat(retMessage, is(notNullValue()));
    assertThat(retMessage.getPayload(), is(notNullValue()));
    assertThat(retMessage.<String>getInboundProperty(MuleProperties.MULE_REMOTE_CLIENT_ADDRESS), is(equalTo(CLIENT_ADDRESS)));
    assertThat(retMessage.<String>getInboundProperty(MuleProperties.MULE_PROXY_ADDRESS), is(equalTo(PROXY_1_ADDRESS)));
  }

  @Test
  public void checkCreateMessageFromSourceWithXForwardedForOneProxy() throws MuleException {
    when(httpServerConnection.getRemoteClientAddress()).thenReturn(PROXY_1_ADDRESS);

    HttpMessageProcessTemplate template = new HttpMessageProcessTemplate(messageReceiver, httpServerConnection);
    message = MuleMessage.builder(message).addInboundProperty(HEADER_X_FORWARDED_FOR, ONE_PROXY_X_FORWARDED_FOR).build();

    MuleMessage retMessage = template.createMessageFromSource(PAYLOAD);
    assertThat(retMessage, is(notNullValue()));
    assertThat(retMessage.getPayload(), is(notNullValue()));
    assertThat(retMessage.<String>getInboundProperty(MuleProperties.MULE_REMOTE_CLIENT_ADDRESS), is(equalTo(CLIENT_ADDRESS)));
    assertThat(retMessage.<String>getInboundProperty(MuleProperties.MULE_PROXY_ADDRESS), is(equalTo(PROXY_1_ADDRESS)));
  }

  @Test
  public void checkCreateMessageFromSourceWithXForwardedForTwoProxy() throws MuleException {
    when(httpServerConnection.getRemoteClientAddress()).thenReturn(PROXY_2_ADDRESS);

    HttpMessageProcessTemplate template = new HttpMessageProcessTemplate(messageReceiver, httpServerConnection);
    message = MuleMessage.builder(message).addInboundProperty(HEADER_X_FORWARDED_FOR, TWO_PROXY_X_FORWARDED_FOR).build();

    MuleMessage retMessage = template.createMessageFromSource(PAYLOAD);
    assertThat(retMessage, is(notNullValue()));
    assertThat(retMessage.getPayload(), is(notNullValue()));
    assertThat(retMessage.<String>getInboundProperty(MuleProperties.MULE_REMOTE_CLIENT_ADDRESS), is(equalTo(CLIENT_ADDRESS)));
    assertThat(retMessage.<String>getInboundProperty(MuleProperties.MULE_PROXY_ADDRESS), is(equalTo(PROXY_2_ADDRESS)));
  }

  @Test
  public void getMuleEventCachesEvent() throws Exception {
    HttpMessageProcessTemplate template = new HttpMessageProcessTemplate(messageReceiver, httpServerConnection);
    template.getMuleEvent();
    template.getMuleEvent();
    verify(messageReceiver, times(1)).createMuleMessage(any(), any());
  }
}
