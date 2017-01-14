/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.compatibility.core.processor.chain;

import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.core.api.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.api.MessageExchangePattern.REQUEST_RESPONSE;

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MessageExchangePattern;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.processor.strategy.DefaultFlowProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.LegacyNonBlockingProcessingStrategyFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@SmallTest
@SuppressWarnings("deprecation")
public class DefaultMessageProcessorChainTestCase extends AbstractMuleContextTestCase {

  protected MessageExchangePattern exchangePattern;
  protected boolean nonBlocking;
  private volatile int threads = 1;

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{REQUEST_RESPONSE, false},
        {REQUEST_RESPONSE, true},
        {ONE_WAY, false},
        {ONE_WAY, true}});
  }

  public DefaultMessageProcessorChainTestCase(MessageExchangePattern exchangePattern, boolean nonBlocking) {
    this.exchangePattern = exchangePattern;
    this.nonBlocking = nonBlocking;
  }

  @Test
  public void testOneWayOutboundEndpointWithService() throws Exception {
    Event event = getTestEventUsingFlow("");

    Processor mp = mock(Processor.class, withSettings().extraInterfaces(OutboundEndpoint.class));
    OutboundEndpoint outboundEndpoint = (OutboundEndpoint) mp;
    when(outboundEndpoint.getExchangePattern()).thenReturn(ONE_WAY);

    MessageProcessorChain chain = new DefaultMessageProcessorChainBuilder().chain(mp).build();
    Event response = process(chain, event);
    assertNull(response);

    assertEquals(1, threads);
  }

  @Test
  public void testOneWayOutboundEndpointWithFlow() throws Exception {
    Event event = getTestEventUsingFlow("");

    Processor mp = mock(Processor.class, withSettings().extraInterfaces(OutboundEndpoint.class));
    OutboundEndpoint outboundEndpoint = (OutboundEndpoint) mp;
    when(outboundEndpoint.getExchangePattern()).thenReturn(ONE_WAY);
    when(outboundEndpoint.mayReturnVoidEvent()).thenAnswer(invocation -> {
      MessageExchangePattern exchangePattern = ((OutboundEndpoint) invocation.getMock()).getExchangePattern();
      return exchangePattern == null ? true : !exchangePattern.hasResponse();
    });
    when(mp.process(any(Event.class))).thenReturn(event);

    MessageProcessorChain chain = new DefaultMessageProcessorChainBuilder().chain(mp).build();
    Event response = process(chain, event);
    assertThat(event.getMessage(), is(response.getMessage()));

    assertEquals(1, threads);
  }

  protected Event getTestEventUsingFlow(Object data) throws Exception {
    Event event = mock(Event.class);
    InternalMessage message = InternalMessage.builder().payload(data).build();
    when(event.getMessage()).thenReturn(message);
    when(event.getExchangePattern()).thenReturn(exchangePattern);
    when(event.getFlowCallStack()).thenReturn(new DefaultFlowCallStack());
    when(event.getError()).thenReturn(empty());
    Pipeline mockFlow = mock(Flow.class);
    when(mockFlow.getProcessingStrategy())
        .thenReturn(nonBlocking
            ? new LegacyNonBlockingProcessingStrategyFactory().create(muleContext,
                                                                      DefaultMessageProcessorChainTestCase.class.getSimpleName())
            : new DefaultFlowProcessingStrategyFactory().create(muleContext,
                                                                DefaultMessageProcessorChainTestCase.class.getSimpleName()));
    when(mockFlow.getMuleContext()).thenReturn(muleContext);
    when(event.getSession()).thenReturn(mock(MuleSession.class));
    return event;
  }

}
