/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.jms.*;

import static javax.jms.Message.DEFAULT_PRIORITY;
import static javax.jms.Message.DEFAULT_TIME_TO_LIVE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mule.transport.jms.JmsConstants.PERSISTENT_DELIVERY_PROPERTY;
import static org.mule.transport.jms.JmsConstants.PRIORITY_PROPERTY;
import static org.mule.transport.jms.JmsConstants.TIME_TO_LIVE_PROPERTY;

@RunWith(PowerMockRunner.class)
@PrepareForTest(JmsMessageDispatcher.class)
@PowerMockIgnore("javax.management.*")
public class JmsMessageDispatcherTestCase extends AbstractMuleContextTestCase
{
    final private boolean defaultPersistentDelivery = true;

    @Mock
    private MuleEvent event;

    @Mock
    private OutboundEndpoint outboundEndpoint;

    @Mock
    private JmsConnector jmsConnector;

    @Mock
    private JmsSupport jmsSupport;

    @Mock
    private MessageProducer messageProducer;

    private MuleMessage muleMessage;
    private JmsMessageDispatcher jmsMessageDispatcher;
    private Message payload;

    @Before
    public void setup() throws Exception
    {
        MuleContext context = mock(MuleContext.class);
        DefaultMuleConfiguration muleConfiguration = mock(DefaultMuleConfiguration.class);
        payload = new ActiveMQTextMessage();
        outboundEndpoint = mock(OutboundEndpoint.class);
        MessageExchangePattern messageExchangePattern = mock(MessageExchangePattern.class);
        jmsConnector = mock(JmsConnector.class);
        JmsTopicResolver jmsTopicResolver = mock(JmsTopicResolver.class);
        jmsSupport = mock(JmsSupport.class);
        Destination destination = mock(ActiveMQDestination.class);
        messageProducer = mock(MessageProducer.class);

        when(context.getConfiguration()).thenReturn(muleConfiguration);
        when(event.getMuleContext()).thenReturn(context);

        muleMessage = new DefaultMuleMessage(payload, context);
        when(event.getMessage()).thenReturn(muleMessage);

        when(outboundEndpoint.getConnector()).thenReturn(jmsConnector);
        when(outboundEndpoint.getExchangePattern()).thenReturn(messageExchangePattern);
        when(messageExchangePattern.hasResponse()).thenReturn(true);
        when(jmsConnector.getTopicResolver()).thenReturn(jmsTopicResolver);
        when(jmsTopicResolver.isTopic(any(ImmutableEndpoint.class), any(Boolean.class))).thenReturn(true);
        when(jmsConnector.getJmsSupport()).thenReturn(jmsSupport);
        when(jmsSupport.createDestination(any(Session.class), eq(outboundEndpoint))).thenReturn(destination);
        when(jmsSupport.createProducer(any(Session.class), eq(destination), anyBoolean())).thenReturn(messageProducer);

        jmsMessageDispatcher = PowerMockito.spy(new JmsMessageDispatcher(outboundEndpoint));
        PowerMockito.doReturn(false).when(jmsMessageDispatcher, "isTransacted");
        PowerMockito.doReturn(false).when(jmsMessageDispatcher, "isUseReplyToDestination", eq(event), anyBoolean(), anyBoolean());
    }

    @Test
    public void defaultSettings() throws Exception
    {
        when(jmsConnector.isPersistentDelivery()).thenReturn(defaultPersistentDelivery);

        jmsMessageDispatcher.doSend(event);

        verify(jmsSupport).send(eq(messageProducer), eq(payload), eq(defaultPersistentDelivery),
                                eq(DEFAULT_PRIORITY), eq(DEFAULT_TIME_TO_LIVE), anyBoolean(), eq(outboundEndpoint));
    }

    @Test
    public void customSettingsByOutboundProperties() throws Exception
    {
        when(jmsConnector.isPersistentDelivery()).thenReturn(defaultPersistentDelivery);
        muleMessage.setOutboundProperty(TIME_TO_LIVE_PROPERTY, "15123");
        muleMessage.setOutboundProperty(PRIORITY_PROPERTY, "7");
        muleMessage.setOutboundProperty(PERSISTENT_DELIVERY_PROPERTY, false);

        jmsMessageDispatcher.doSend(event);

        verify(jmsSupport).send(eq(messageProducer), eq(payload), eq(false),
                                eq(7), eq(15123L), anyBoolean(), eq(outboundEndpoint));
    }
}
