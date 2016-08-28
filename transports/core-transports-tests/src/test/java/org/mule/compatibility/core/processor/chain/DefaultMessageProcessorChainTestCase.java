/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.compatibility.core.processor.chain;

import static org.apache.commons.lang.RandomStringUtils.randomNumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.construct.flow.DefaultFlowProcessingStrategy;
import org.mule.runtime.core.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@SmallTest
@SuppressWarnings("deprecation")
public class DefaultMessageProcessorChainTestCase extends AbstractMuleTestCase {

  protected MuleContext muleContext;

  protected MessageExchangePattern exchangePattern;
  protected boolean nonBlocking;
  protected boolean synchronous;
  private volatile int threads = 1;

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{REQUEST_RESPONSE, false, true},
        {REQUEST_RESPONSE, false, false},
        {REQUEST_RESPONSE, true, true},
        {REQUEST_RESPONSE, true, false},
        {ONE_WAY, false, true},
        {ONE_WAY, false, false},
        {ONE_WAY, true, true}});
  }

  public DefaultMessageProcessorChainTestCase(MessageExchangePattern exchangePattern, boolean nonBlocking, boolean synchronous) {
    this.exchangePattern = exchangePattern;
    this.nonBlocking = nonBlocking;
    this.synchronous = synchronous;
  }

  @Before
  public void before() {
    muleContext = mock(MuleContext.class);
    MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
    when(muleConfiguration.isContainerMode()).thenReturn(false);
    when(muleConfiguration.getId()).thenReturn(randomNumeric(3));
    when(muleConfiguration.getShutdownTimeout()).thenReturn(1000);
    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
  }

  @Test
  public void testOneWayOutboundEndpointWithService() throws Exception {
    MuleEvent event = getTestEventUsingFlow("");

    MessageProcessor mp = mock(MessageProcessor.class, withSettings().extraInterfaces(OutboundEndpoint.class));
    OutboundEndpoint outboundEndpoint = (OutboundEndpoint) mp;
    when(outboundEndpoint.getExchangePattern()).thenReturn(ONE_WAY);

    MessageProcessorChain chain = new DefaultMessageProcessorChainBuilder(muleContext).chain(mp).build();
    MuleEvent response = chain.process(event);
    assertNull(response);

    assertEquals(1, threads);
  }

  @Test
  public void testOneWayOutboundEndpointWithFlow() throws Exception {
    MuleEvent event = getTestEventUsingFlow("");

    MessageProcessor mp = mock(MessageProcessor.class, withSettings().extraInterfaces(OutboundEndpoint.class));
    OutboundEndpoint outboundEndpoint = (OutboundEndpoint) mp;
    when(outboundEndpoint.getExchangePattern()).thenReturn(ONE_WAY);
    when(outboundEndpoint.mayReturnVoidEvent()).thenAnswer(invocation -> {
      MessageExchangePattern exchangePattern = ((OutboundEndpoint) invocation.getMock()).getExchangePattern();
      return exchangePattern == null ? true : !exchangePattern.hasResponse();
    });
    when(mp.process(any(MuleEvent.class))).thenReturn(VoidMuleEvent.getInstance());

    MessageProcessorChain chain = new DefaultMessageProcessorChainBuilder(muleContext).chain(mp).build();
    MuleEvent response = chain.process(event);
    assertThat(event.getMessage(), is(response.getMessage()));

    assertEquals(1, threads);
  }

  protected MuleEvent getTestEventUsingFlow(Object data) throws Exception {
    MuleEvent event = mock(MuleEvent.class);
    MuleMessage message = MuleMessage.builder().payload(data).build();
    when(event.getId()).thenReturn(randomNumeric(3));
    when(event.getMessage()).thenReturn(message);
    when(event.getExchangePattern()).thenReturn(exchangePattern);
    when(event.getFlowCallStack()).thenReturn(new DefaultFlowCallStack());
    Pipeline mockFlow = mock(Flow.class);
    when(mockFlow.getProcessingStrategy())
        .thenReturn(nonBlocking ? new NonBlockingProcessingStrategy() : new DefaultFlowProcessingStrategy());
    when(mockFlow.getMuleContext()).thenReturn(muleContext);
    when(event.getSession()).thenReturn(mock(MuleSession.class));
    when(event.isSynchronous()).thenReturn(synchronous);
    when(event.isAllowNonBlocking()).thenReturn(!synchronous && nonBlocking);
    // when(event.isNonBlocking()).thenReturn(nonBlocking);
    return event;
  }

}
