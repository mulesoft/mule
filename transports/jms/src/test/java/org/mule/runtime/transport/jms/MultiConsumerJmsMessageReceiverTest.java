/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.execution.MessageProcessingManager;
import org.mule.runtime.core.retry.policies.SimpleRetryPolicyTemplate;
import org.mule.tck.junit4.AbstractMuleTestCase;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MultiConsumerJmsMessageReceiverTest extends AbstractMuleTestCase
{

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JmsConnector mockJmsConnector;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FlowConstruct mockFlowConstruct;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InboundEndpoint mockInboundEndpoint;

    @Test
    public void testTopicReceiverShouldBeStartedOnlyInPrimaryNode() throws Exception
    {
        when(mockJmsConnector.getTopicResolver().isTopic(mockInboundEndpoint, true)).thenReturn(true);
        when(mockInboundEndpoint.getConnector()).thenReturn(mockJmsConnector);
        MultiConsumerJmsMessageReceiver messageReceiver = new MultiConsumerJmsMessageReceiver(mockJmsConnector, mockFlowConstruct, mockInboundEndpoint);
        assertThat("receiver must be started only in primary node", messageReceiver.shouldConsumeInEveryNode(), is(false));
    }

    @Test
    public void testQueueReceiverShouldBeStartedInEveryNode() throws Exception
    {
        when(mockJmsConnector.getTopicResolver().isTopic(mockInboundEndpoint, true)).thenReturn(false);
        when(mockInboundEndpoint.getConnector()).thenReturn(mockJmsConnector);
        MultiConsumerJmsMessageReceiver messageReceiver = new MultiConsumerJmsMessageReceiver(mockJmsConnector, mockFlowConstruct, mockInboundEndpoint);
        assertThat("receiver must be started only in primary node", messageReceiver.shouldConsumeInEveryNode(), is(true));
    }

    @Test
    public void messageListenerNotSetTwiceOnMessageReceiver() throws Exception
    {
        when(mockJmsConnector.getTopicResolver().isTopic(mockInboundEndpoint, true)).thenReturn(false);
        when(mockJmsConnector.getNumberOfConsumers()).thenReturn(1);

        MessageConsumer mockMessageConsumer = mock(TestMessageConsumer.class, CALLS_REAL_METHODS);
        when(mockJmsConnector.getJmsSupport()
                    .createConsumer(any(Session.class), any(Destination.class), anyString(), anyBoolean(), anyString(), anyBoolean(), any(InboundEndpoint.class)))
                    .thenReturn(mockMessageConsumer);
        when(mockInboundEndpoint.getConnector()).thenReturn(mockJmsConnector);
        when(mockInboundEndpoint.getMuleContext().getRegistry().get(MuleProperties.OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER)).thenReturn(mock(MessageProcessingManager.class));
        SimpleRetryPolicyTemplate retryPolicyTemplate = new SimpleRetryPolicyTemplate();
        retryPolicyTemplate.setMuleContext(mockJmsConnector.getMuleContext());
        when(mockInboundEndpoint.getRetryPolicyTemplate()).thenReturn(retryPolicyTemplate);
        when(mockInboundEndpoint.getProperties().get(JmsConstants.DURABLE_PROPERTY)).thenReturn("false");
        when(mockInboundEndpoint.getProperties().get(JmsConstants.DURABLE_NAME_PROPERTY)).thenReturn(null);

        MultiConsumerJmsMessageReceiver messageReceiver = new MultiConsumerJmsMessageReceiver(mockJmsConnector, mockFlowConstruct, mockInboundEndpoint);
        messageReceiver.initialise();
        messageReceiver.doStart();
        verify(mockMessageConsumer).setMessageListener(any(MessageListener.class));
        reset(mockMessageConsumer);
        messageReceiver.startSubReceivers();
        verify(mockMessageConsumer, never()).setMessageListener(any(MessageListener.class));
    }

    private abstract class TestMessageConsumer implements MessageConsumer
    {

        private MessageListener messageListener;

        @Override
        public MessageListener getMessageListener() throws JMSException
        {
            return messageListener;
        }

        @Override
        public void setMessageListener(MessageListener messageListener) throws JMSException
        {
            this.messageListener = messageListener;
        }
    }


}
