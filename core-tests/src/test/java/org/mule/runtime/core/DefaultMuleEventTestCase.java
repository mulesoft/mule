/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MessageContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.construct.flow.DefaultFlowProcessingStrategy;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.matcher.DataTypeMatcher;
import org.mule.tck.size.SmallTest;

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class DefaultMuleEventTestCase extends AbstractMuleContextTestCase {

  public static final Charset CUSTOM_ENCODING = UTF_8;
  public static final String PROPERTY_NAME = "test";
  public static final String PROPERTY_VALUE = "foo";

  private final MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
  private MuleMessage muleMessage = MuleMessage.builder().payload("test-data").build();
  private Flow flow;
  private MessageContext messageContext;
  private DefaultMuleEvent muleEvent;

  @Before
  public void before() throws Exception {
    flow = getTestFlow();
    messageContext = DefaultMessageContext.create(flow, TEST_CONNECTOR);
    muleEvent = new DefaultMuleEvent(messageContext, muleMessage, REQUEST_RESPONSE, flow);
  }

  @Test
  public void setFlowVariableDefaultDataType() throws Exception {
    muleEvent.setFlowVariable(PROPERTY_NAME, PROPERTY_VALUE);

    DataType dataType = muleEvent.getFlowVariableDataType(PROPERTY_NAME);
    assertThat(dataType, DataTypeMatcher.like(String.class, MediaType.ANY, null));
  }

  @Test
  public void setFlowVariableCustomDataType() throws Exception {
    DataType dataType = DataType.builder().type(String.class).mediaType(APPLICATION_XML).charset(CUSTOM_ENCODING).build();

    muleEvent.setFlowVariable(PROPERTY_NAME, PROPERTY_VALUE, dataType);

    DataType actualDataType = muleEvent.getFlowVariableDataType(PROPERTY_NAME);
    assertThat(actualDataType, DataTypeMatcher.like(String.class, APPLICATION_XML, CUSTOM_ENCODING));
  }

  @Test
  public void setSessionVariableDefaultDataType() throws Exception {
    muleEvent.getSession().setProperty(PROPERTY_NAME, PROPERTY_VALUE);

    DataType dataType = muleEvent.getSession().getPropertyDataType(PROPERTY_NAME);
    assertThat(dataType, DataTypeMatcher.like(String.class, MediaType.ANY, null));
  }

  @Test
  public void setSessionVariableCustomDataType() throws Exception {
    DataType dataType = DataType.builder().type(String.class).mediaType(APPLICATION_XML).charset(CUSTOM_ENCODING).build();

    muleEvent.getSession().setProperty(PROPERTY_NAME, PROPERTY_VALUE, dataType);

    DataType actualDataType = muleEvent.getSession().getPropertyDataType(PROPERTY_NAME);
    assertThat(actualDataType, DataTypeMatcher.like(String.class, APPLICATION_XML, CUSTOM_ENCODING));
  }

  @Test
  public void defaultProcessingStrategyRequestResponse() throws Exception {
    Flow flow = spy(this.flow);
    when(flow.getProcessingStrategy()).thenReturn(new DefaultFlowProcessingStrategy());
    when(flow.getMuleContext()).thenReturn(muleContext);
    DefaultMuleEvent event = new DefaultMuleEvent(messageContext, muleMessage, REQUEST_RESPONSE, flow);
    assertThat(event.isSynchronous(), equalTo(true));
    assertThat(event.isTransacted(), equalTo(false));
  }

  @Test
  public void defaultProcessingStrategyOneWay() throws Exception {
    Flow flow = spy(this.flow);
    when(flow.getProcessingStrategy()).thenReturn(new DefaultFlowProcessingStrategy());
    when(flow.getMuleContext()).thenReturn(muleContext);
    DefaultMuleEvent event = new DefaultMuleEvent(messageContext, muleMessage, ONE_WAY, flow);
    assertThat(event.isSynchronous(), equalTo(false));
    assertThat(event.isTransacted(), equalTo(false));
  }

  @Test
  public void syncProcessingStrategyRequestResponse() throws Exception {
    Flow flow = spy(this.flow);
    when(flow.isSynchronous()).thenReturn(true);
    when(flow.getMuleContext()).thenReturn(muleContext);
    DefaultMuleEvent event = new DefaultMuleEvent(messageContext, muleMessage, REQUEST_RESPONSE, flow);
    assertThat(event.isSynchronous(), equalTo(true));
    assertThat(event.isTransacted(), equalTo(false));
  }

  @Test
  public void syncProcessingStrategyOneWay() throws Exception {
    Flow flow = spy(this.flow);
    when(flow.isSynchronous()).thenReturn(true);
    when(flow.getMuleContext()).thenReturn(muleContext);
    DefaultMuleEvent event = new DefaultMuleEvent(messageContext, muleMessage, ONE_WAY, flow);
    assertThat(event.isSynchronous(), equalTo(true));
    assertThat(event.isTransacted(), equalTo(false));
  }

  @Test
  public void inboundPropertyForceSyncRequestResponse() throws Exception {
    Flow flow = spy(this.flow);
    when(flow.isSynchronous()).thenReturn(false);
    when(flow.getMuleContext()).thenReturn(muleContext);
    MuleEvent event = MuleEvent.builder(messageContext).message(muleMessage).exchangePattern(REQUEST_RESPONSE).flow(flow)
        .synchronous(true).build();
    assertThat(event.isSynchronous(), equalTo(true));
    assertThat(event.isTransacted(), equalTo(false));
  }

  @Test
  public void inboundPropertyForceSyncOneWay() throws Exception {
    Flow flow = spy(this.flow);
    when(flow.isSynchronous()).thenReturn(false);
    when(flow.getMuleContext()).thenReturn(muleContext);
    MuleEvent event =
        MuleEvent.builder(messageContext).message(muleMessage).exchangePattern(ONE_WAY).flow(flow).synchronous(true).build();
    assertThat(event.isSynchronous(), equalTo(true));
    assertThat(event.isTransacted(), equalTo(false));
  }

  @Test
  public void nonBlockingProcessingStrategyRequestResponse() throws Exception {
    Flow flow = spy(this.flow);
    when(flow.isSynchronous()).thenReturn(false);
    when(flow.getProcessingStrategy()).thenReturn(new NonBlockingProcessingStrategy());
    when(flow.getMuleContext()).thenReturn(muleContext);
    DefaultMuleEvent event = new DefaultMuleEvent(messageContext, muleMessage, REQUEST_RESPONSE, flow);
    assertThat(event.isSynchronous(), equalTo(false));
    assertThat(event.isTransacted(), equalTo(false));
  }

  @Test
  public void nonBlockingProcessingStrategyOneWay() throws Exception {
    Flow flow = spy(this.flow);
    when(flow.isSynchronous()).thenReturn(false);
    when(flow.getProcessingStrategy()).thenReturn(new NonBlockingProcessingStrategy());
    when(flow.getMuleContext()).thenReturn(muleContext);
    DefaultMuleEvent event = new DefaultMuleEvent(messageContext, muleMessage, ONE_WAY, flow);
    assertThat(event.isSynchronous(), equalTo(false));
    assertThat(event.isTransacted(), equalTo(false));
  }
}
