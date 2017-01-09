/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.transport.MessageReceiver;
import org.mule.compatibility.core.endpoint.MuleEndpointURI;
import org.mule.compatibility.core.message.CompatibilityMessage;
import org.mule.compatibility.core.message.MuleCompatibilityMessageBuilder;
import org.mule.compatibility.core.session.NullSessionHandler;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.TransformationService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.transaction.MuleTransactionConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.mule.TestMessageReceiver;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * <li>Correct return value for ONE_WAY/REQUEST_RESPONSE endpoints.
 * <li>//TODO: Test default transformers are applied
 * <li>//TODO: Test root message id propagation
 * <li>//TODO: Test filter exception
 * <li>//TODO: Test response security context
 */
@RunWith(value = MockitoJUnitRunner.class)
@SmallTest
public class MessageReceiverTestCase extends AbstractMuleTestCase {

  @Mock
  private MuleContext muleContext;
  @Mock
  private MuleSession muleSession;
  @Mock
  private TransformationService transformationService;

  @Before
  public void setup() throws MuleException {
    MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
    when(muleContext.getTransformationService()).thenReturn(transformationService);
    when(transformationService.applyTransformers(any(InternalMessage.class), any(Event.class), anyList()))
        .thenAnswer(answer -> answer.getArguments()[0]);
  }

  @Test
  public void routeMessageOneWayReturnsNotNull() throws MuleException {
    MessageReceiver receiver = createMessageReciever(ONE_WAY);

    assertNotNull(receiver.routeMessage(createRequestMessage()));
  }

  @Test
  public void routeMessageRequestResponseReturnsEvent() throws MuleException {
    MessageReceiver receiver = createMessageReciever(REQUEST_RESPONSE);
    CompatibilityMessage request = createRequestMessage();

    assertEquals(request.getPayload().getValue(), receiver.routeMessage(request).getMessage().getPayload().getValue().toString());
  }

  protected CompatibilityMessage createRequestMessage() {
    return (CompatibilityMessage) new MuleCompatibilityMessageBuilder().payload("").build();
  }

  protected MessageReceiver createMessageReciever(MessageExchangePattern mep) throws MuleException {
    AbstractConnector connector = mock(AbstractConnector.class);
    when(connector.getSessionHandler()).thenReturn(new NullSessionHandler());
    when(connector.getMuleContext()).thenReturn(muleContext);

    FlowConstruct flowConstruct = mock(FlowConstruct.class);
    when(flowConstruct.getMuleContext()).thenReturn(muleContext);

    InboundEndpoint endpoint = mock(InboundEndpoint.class);
    when(endpoint.getExchangePattern()).thenReturn(mep);
    when(endpoint.getConnector()).thenReturn(connector);
    when(endpoint.getEndpointURI()).thenReturn(new MuleEndpointURI("test://test", muleContext));
    when(endpoint.getTransactionConfig()).thenReturn(new MuleTransactionConfig());
    when(endpoint.getExchangePattern()).thenReturn(mep);
    when(endpoint.getMuleContext()).thenReturn(muleContext);

    Event responseEvent = mock(Event.class);
    when(responseEvent.getSession()).thenReturn(muleSession);

    Processor listener = mock(Processor.class);
    when(listener.process(any(Event.class))).thenAnswer(invocation -> (Event) invocation.getArguments()[0]);

    MessageReceiver messageReceiver = new TestMessageReceiver(connector, flowConstruct, endpoint);
    messageReceiver.setListener(listener);
    return messageReceiver;
  }

}
