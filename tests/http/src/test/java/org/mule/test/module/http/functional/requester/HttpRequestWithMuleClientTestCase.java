/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTPS;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import static org.mule.runtime.module.http.api.requester.HttpStreamingType.NEVER;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.module.http.api.HttpConstants;
import org.mule.runtime.module.http.api.HttpHeaders;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.runtime.module.http.api.requester.HttpRequesterConfig;
import org.mule.runtime.module.http.api.requester.HttpRequesterConfigBuilder;
import org.mule.test.module.http.functional.AbstractHttpTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.ByteArrayInputStream;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Ignore("MULE-9817")
public class HttpRequestWithMuleClientTestCase extends AbstractHttpTestCase {

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
  protected String getConfigFile() {
    return "http-request-mule-client-config.xml";
  }

  @Test
  public void dispatchRequestUseNewConnectorByDefault() throws MuleException {
    muleContext.getClient().dispatch(getUrl(), getTestMuleMessage());
    final MuleMessage receivedMessage = getMessageReceivedByFlow();
    assertThat(receivedMessage.getPayload(), Is.<Object>is(Objects.toString(null)));
  }

  @Ignore("See MULE-8049")
  @Test
  public void dispatchHttpPostRequestWithStreamingEnabled() throws Exception {
    muleContext.getClient().dispatch(getUrl(), getTestMuleMessage(new ByteArrayInputStream(TEST_MESSAGE.getBytes())),
                                     newOptions().method("POST").build());
    final MuleMessage receivedMessage = getMessageReceivedByFlow();
    assertThat(receivedMessage, notNullValue());
    assertThat(getPayloadAsString(receivedMessage), is(TEST_MESSAGE));
    assertThat(receivedMessage.getInboundProperty(HttpHeaders.Names.TRANSFER_ENCODING),
               Is.<Object>is(HttpHeaders.Values.CHUNKED));
  }

  @Test
  public void dispatchWithStreamingDisabled() throws Exception {
    final HttpRequestOptions options = newOptions().method(PUT_HTTP_METHOD).requestStreamingMode(NEVER).build();
    muleContext.getClient().dispatch(getUrl(), getTestMuleMessage(TEST_MESSAGE), options);
    final MuleMessage receivedMessage = getMessageReceivedByFlow();
    assertThat(receivedMessage.getInboundProperty(HttpHeaders.Names.TRANSFER_ENCODING), nullValue());
    assertThat(receivedMessage.getInboundProperty(HttpHeaders.Names.CONTENT_LENGTH), Is.<Object>is("12"));
  }

  @Ignore("See MULE-8049")
  @Test
  public void sendHttpPutMethod() throws Exception {
    final MuleMessage response =
        muleContext.getClient().send(getUrl(), getTestMuleMessage(TEST_MESSAGE), newOptions().method(PUT_HTTP_METHOD).build())
            .getRight();
    assertThat(getPayloadAsString(response), is(TEST_MESSAGE));
    final MuleMessage receivedMessage = getMessageReceivedByFlow();
    assertThat(getPayloadAsString(receivedMessage), is(TEST_MESSAGE));
    assertThat(receivedMessage.getInboundProperty(HttpConstants.RequestProperties.HTTP_METHOD_PROPERTY),
               Is.<Object>is(PUT_HTTP_METHOD));
  }

  @Test
  public void sendDisableRedirect() throws Exception {
    final MuleMessage response =
        muleContext.getClient().send(getRedirectUrl(), MuleMessage.builder().nullPayload().build(),
                                     newOptions().method(PUT_HTTP_METHOD).disableFollowsRedirect().build())
            .getRight();
    assertThat(getPayloadAsString(response), is("test-response"));
  }

  @Test
  public void sendEnableRedirect() throws Exception {
    final MuleMessage response = muleContext.getClient().send(getRedirectUrl(), MuleMessage.builder().nullPayload().build(),
                                                              newOptions().enableFollowsRedirect().build())
        .getRight();
    assertThat(getPayloadAsString(response), is(Objects.toString(null)));
  }

  @Test
  public void setWithTimeout() throws Exception {
    expectedException.expectCause(IsInstanceOf.<Throwable>instanceOf(TimeoutException.class));
    try {
      muleContext.getClient().send(getTimeoutUrl(), MuleMessage.builder().nullPayload().build(),
                                   newOptions().responseTimeout(RESPONSE_TIMEOUT).build());
    } finally {
      LatchMessageProcessor.latch.release();
    }
  }

  @Test
  public void sendDisableRedirectByRequestConfig() throws Exception {
    final HttpRequestOptions options = newOptions().method(PUT_HTTP_METHOD).requestConfig(getRequestConfig()).build();
    final MuleMessage response =
        muleContext.getClient().send(getRedirectUrl(), MuleMessage.builder().nullPayload().build(), options).getRight();
    assertThat(getPayloadAsString(response), is(TEST_RESPONSE));
  }

  @Test
  public void disableStatusCodeValidation() throws Exception {
    final HttpRequestOptions options = newOptions().disableStatusCodeValidation().build();
    final MuleMessage response =
        muleContext.getClient().send(getFailureUrl(), MuleMessage.builder().nullPayload().build(), options).getRight();
    assertThat(response.getInboundProperty(HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY), Is.<Object>is(500));
  }

  @Test
  public void customRequestConfig() throws Exception {
    final HttpRequesterConfig requestConfig = new HttpRequesterConfigBuilder(muleContext).setProtocol(HTTPS)
        .setTlsContext(muleContext.getRegistry().<TlsContextFactory>get("tlsContext")).build();
    final HttpRequestOptions options = newOptions().disableStatusCodeValidation().requestConfig(requestConfig).build();
    final MuleMessage response = muleContext.getClient().send(format("https://localhost:%s/", httpsPort.getNumber()),
                                                              MuleMessage.builder().nullPayload().build(), options)
        .getRight();
    assertThat(response.getInboundProperty(HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY), Is.<Object>is(200));
    assertThat(getPayloadAsString(response), is(TEST_RESPONSE));
  }

  public static class LatchMessageProcessor implements MessageProcessor {

    public static Latch latch = new Latch();

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      try {
        latch.await(SERVER_TIMEOUT, MILLISECONDS);
      } catch (InterruptedException e) {
        throw new DefaultMuleException(e);
      }
      return event;
    }
  }

  private MuleMessage getMessageReceivedByFlow() throws MuleException {
    return muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();
  }

  private HttpRequesterConfig getRequestConfig() {
    return muleContext.getRegistry().get("requestConfig");
  }

  private String getUrl() {
    return format("http://localhost:%s/path", port.getNumber());
  }

  private String getRedirectUrl() {
    return format("http://localhost:%s/redirectPath", port.getNumber());
  }

  private String getTimeoutUrl() {
    return format("http://localhost:%s/timeoutPath", port.getNumber());
  }

  private String getFailureUrl() {
    return format("http://localhost:%s/failurePath", port.getNumber());
  }
}
