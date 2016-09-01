/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.compatibility.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.compatibility.core.DefaultMuleEventEndpointUtils.populateFieldsFromInboundEndpoint;

import org.mule.compatibility.core.api.endpoint.EndpointException;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.endpoint.MuleEndpointURI;
import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class DefaultMuleEventTestCase extends AbstractMuleTestCase {

  private final MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
  private final MuleMessage muleMessage = MuleMessage.builder().payload("test-data").build();

  @Test
  public void transactedRequestResponse() throws Exception {
    Flow flow = mock(Flow.class);
    when(flow.isSynchronous()).thenReturn(false);
    when(flow.getMuleContext()).thenReturn(muleContext);
    MuleEvent event =
        MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(muleMessage).flow(flow).build();
    populateFieldsFromInboundEndpoint(event, createMockTransactionalInboundEndpoint());
    assertThat(event.isSynchronous(), equalTo(true));
    assertThat(event.isTransacted(), equalTo(true));
  }

  @Test
  public void transactedOneWay() throws Exception {
    Flow flow = mock(Flow.class);
    when(flow.isSynchronous()).thenReturn(false);
    when(flow.getMuleContext()).thenReturn(muleContext);
    MuleEvent event =
        MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(muleMessage).flow(flow).build();
    populateFieldsFromInboundEndpoint(event, createMockTransactionalInboundEndpoint());
    assertThat(event.isSynchronous(), equalTo(true));
    assertThat(event.isTransacted(), equalTo(true));
  }

  private InboundEndpoint createMockTransactionalInboundEndpoint() throws EndpointException {
    InboundEndpoint inboundEndpoint = mock(InboundEndpoint.class);
    TransactionConfig transactionConfig = mock(TransactionConfig.class);
    when(transactionConfig.isTransacted()).thenReturn(true);
    when(inboundEndpoint.getTransactionConfig()).thenReturn(transactionConfig);
    when(inboundEndpoint.getEndpointURI()).thenReturn(new MuleEndpointURI("test://test", muleContext));
    return inboundEndpoint;
  }
}
