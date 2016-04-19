/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.transformer.types.MimeTypes.APPLICATION_XML;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.EndpointException;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.construct.flow.DefaultFlowProcessingStrategy;
import org.mule.runtime.core.endpoint.MuleEndpointURI;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.runtime.core.transformer.types.DataTypeFactory;
import org.mule.runtime.core.transformer.types.MimeTypes;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.matcher.DataTypeMatcher;
import org.mule.tck.size.SmallTest;

import java.util.Map;

import org.junit.Test;

@SmallTest
public class DefaultMuleEventTestCase extends AbstractMuleTestCase
{

    public static final String CUSTOM_ENCODING = "UTF-8";
    public static final String PROPERTY_NAME = "test";
    public static final String PROPERTY_VALUE = "foo";

    private final MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
    private final DefaultMuleMessage muleMessage = new DefaultMuleMessage("test-data", (Map<String, Object>) null, muleContext);
    private final DefaultMuleEvent muleEvent = new DefaultMuleEvent(muleMessage, MessageExchangePattern.REQUEST_RESPONSE, (FlowConstruct) null);

    @Test
    public void setFlowVariableDefaultDataType() throws Exception
    {
        muleEvent.setFlowVariable(PROPERTY_NAME, PROPERTY_VALUE);

        DataType<?> dataType = muleEvent.getFlowVariableDataType(PROPERTY_NAME);
        assertThat(dataType, DataTypeMatcher.like(String.class, MimeTypes.ANY, null));
    }

    @Test
    public void setFlowVariableCustomDataType() throws Exception
    {
        DataType dataType = DataTypeFactory.create(String.class, APPLICATION_XML);
        dataType.setEncoding(CUSTOM_ENCODING);

        muleEvent.setFlowVariable(PROPERTY_NAME, PROPERTY_VALUE, dataType);

        DataType<?> actualDataType = muleEvent.getFlowVariableDataType(PROPERTY_NAME);
        assertThat(actualDataType, DataTypeMatcher.like(String.class, APPLICATION_XML, CUSTOM_ENCODING));
    }

    @Test
    public void setSessionVariableDefaultDataType() throws Exception
    {
        muleEvent.getSession().setProperty(PROPERTY_NAME, PROPERTY_VALUE);

        DataType<?> dataType = muleEvent.getSession().getPropertyDataType(PROPERTY_NAME);
        assertThat(dataType, DataTypeMatcher.like(String.class, MimeTypes.ANY, null));
    }

    @Test
    public void setSessionVariableCustomDataType() throws Exception
    {
        DataType dataType = DataTypeFactory.create(String.class, APPLICATION_XML);
        dataType.setEncoding(CUSTOM_ENCODING);

        muleEvent.getSession().setProperty(PROPERTY_NAME, PROPERTY_VALUE, dataType);

        DataType<?> actualDataType = muleEvent.getSession().getPropertyDataType(PROPERTY_NAME);
        assertThat(actualDataType, DataTypeMatcher.like(String.class, APPLICATION_XML, CUSTOM_ENCODING));
    }

    @Test
    public void defaultProcessingStrategyRequestResponse() throws Exception
    {
        Flow flow = mock(Flow.class);
        when(flow.getProcessingStrategy()).thenReturn(new DefaultFlowProcessingStrategy());
        DefaultMuleEvent event = new DefaultMuleEvent(muleMessage, MessageExchangePattern.REQUEST_RESPONSE, flow);
        assertThat(event.isSynchronous(), equalTo(true));
        assertThat(event.isTransacted(), equalTo(false));
    }

    @Test
    public void defaultProcessingStrategyOneWay() throws Exception
    {
        Flow flow = mock(Flow.class);
        when(flow.getProcessingStrategy()).thenReturn(new DefaultFlowProcessingStrategy());
        DefaultMuleEvent event = new DefaultMuleEvent(muleMessage, MessageExchangePattern.ONE_WAY, flow);
        assertThat(event.isSynchronous(), equalTo(false));
        assertThat(event.isTransacted(), equalTo(false));
    }

    @Test
    public void syncProcessingStrategyRequestResponse() throws Exception
    {
        Flow flow = mock(Flow.class);
        when(flow.isSynchronous()).thenReturn(true);
        DefaultMuleEvent event = new DefaultMuleEvent(muleMessage, MessageExchangePattern.REQUEST_RESPONSE, flow);
        assertThat(event.isSynchronous(), equalTo(true));
        assertThat(event.isTransacted(), equalTo(false));
    }

    @Test
    public void syncProcessingStrategyOneWay() throws Exception
    {
        Flow flow = mock(Flow.class);
        when(flow.isSynchronous()).thenReturn(true);
        DefaultMuleEvent event = new DefaultMuleEvent(muleMessage, MessageExchangePattern.ONE_WAY, flow);
        assertThat(event.isSynchronous(), equalTo(true));
        assertThat(event.isTransacted(), equalTo(false));
    }

    @Test
    public void inboundPropertyForceSyncRequestResponse() throws Exception
    {
        Flow flow = mock(Flow.class);
        when(flow.isSynchronous()).thenReturn(false);
        muleMessage.setProperty(MuleProperties.MULE_FORCE_SYNC_PROPERTY, true, PropertyScope.INBOUND);
        DefaultMuleEvent event = new DefaultMuleEvent(muleMessage, MessageExchangePattern.REQUEST_RESPONSE, flow);
        assertThat(event.isSynchronous(), equalTo(true));
        assertThat(event.isTransacted(), equalTo(false));
    }

    @Test
    public void inboundPropertyForceSyncOneWay() throws Exception
    {
        Flow flow = mock(Flow.class);
        when(flow.isSynchronous()).thenReturn(false);
        muleMessage.setProperty(MuleProperties.MULE_FORCE_SYNC_PROPERTY, true, PropertyScope.INBOUND);
        DefaultMuleEvent event = new DefaultMuleEvent(muleMessage, MessageExchangePattern.ONE_WAY, flow);
        assertThat(event.isSynchronous(), equalTo(true));
        assertThat(event.isTransacted(), equalTo(false));
    }

    @Test
    public void nonBlockingProcessingStrategyRequestResponse() throws Exception
    {
        Flow flow = mock(Flow.class);
        when(flow.isSynchronous()).thenReturn(false);
        when(flow.getProcessingStrategy()).thenReturn(new NonBlockingProcessingStrategy());
        DefaultMuleEvent event = new DefaultMuleEvent(muleMessage, MessageExchangePattern.REQUEST_RESPONSE, flow);
        assertThat(event.isSynchronous(), equalTo(false));
        assertThat(event.isTransacted(), equalTo(false));
    }

    @Test
    public void nonBlockingProcessingStrategyOneWay() throws Exception
    {
        Flow flow = mock(Flow.class);
        when(flow.isSynchronous()).thenReturn(false);
        when(flow.getProcessingStrategy()).thenReturn(new NonBlockingProcessingStrategy());
        DefaultMuleEvent event = new DefaultMuleEvent(muleMessage, MessageExchangePattern.ONE_WAY, flow);
        assertThat(event.isSynchronous(), equalTo(false));
        assertThat(event.isTransacted(), equalTo(false));
    }

    @Test
    public void transactedRequestResponse() throws Exception
    {
        Flow flow = mock(Flow.class);
        when(flow.isSynchronous()).thenReturn(false);
        DefaultMuleEvent event = new DefaultMuleEvent(muleMessage, flow);
        event.populateFieldsFromInboundEndpoint(createMockTransactionalInboundEndpoint());
        assertThat(event.isSynchronous(), equalTo(true));
        assertThat(event.isTransacted(), equalTo(true));
    }

    @Test
    public void transactedOneWay() throws Exception
    {
        Flow flow = mock(Flow.class);
        when(flow.isSynchronous()).thenReturn(false);
        DefaultMuleEvent event = new DefaultMuleEvent(muleMessage, flow);
        event.populateFieldsFromInboundEndpoint(createMockTransactionalInboundEndpoint());
        assertThat(event.isSynchronous(), equalTo(true));
        assertThat(event.isTransacted(), equalTo(true));
    }

    private InboundEndpoint createMockTransactionalInboundEndpoint() throws EndpointException
    {
        InboundEndpoint inboundEndpoint = mock(InboundEndpoint.class);
        TransactionConfig transactionConfig = mock(TransactionConfig.class);
        when(transactionConfig.isTransacted()).thenReturn(true);
        when(inboundEndpoint.getTransactionConfig()).thenReturn(transactionConfig);
        when(inboundEndpoint.getEndpointURI()).thenReturn(new MuleEndpointURI("test://test", muleContext));
        return inboundEndpoint;
    }
}