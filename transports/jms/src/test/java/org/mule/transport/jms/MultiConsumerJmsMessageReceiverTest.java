/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.tck.junit4.AbstractMuleTestCase;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MultiConsumerJmsMessageReceiverTest extends AbstractMuleTestCase {


    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JmsConnector mockJmsConnector;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FlowConstruct mockFlowConstruct;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InboundEndpoint mockInboundEndpoint;

    @Test
    public void testTopicReceiverShouldBeStartedOnlyInPrimaryNode() throws Exception
    {
        when(mockJmsConnector.getTopicResolver().isTopic(mockInboundEndpoint,true)).thenReturn(true);
        when(mockInboundEndpoint.getConnector()).thenReturn(mockJmsConnector);
        MultiConsumerJmsMessageReceiver messageReceiver = new MultiConsumerJmsMessageReceiver(mockJmsConnector, mockFlowConstruct, mockInboundEndpoint);
        assertThat("receiver must be started only in primary node", messageReceiver.shouldConsumeInEveryNode(), is(false));
    }

    @Test
    public void testQueueReceiverShouldBeStartedInEveryNode() throws Exception
    {
        when(mockJmsConnector.getTopicResolver().isTopic(mockInboundEndpoint,true)).thenReturn(false);
        when(mockInboundEndpoint.getConnector()).thenReturn(mockJmsConnector);
        MultiConsumerJmsMessageReceiver messageReceiver = new MultiConsumerJmsMessageReceiver(mockJmsConnector, mockFlowConstruct, mockInboundEndpoint);
        assertThat("receiver must be started only in primary node", messageReceiver.shouldConsumeInEveryNode(), is(true));
    }


}
