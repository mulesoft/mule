/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
public class XaTransactedJmsMessageReceiverTest extends AbstractMuleTestCase {


    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JmsConnector mockJmsConnector;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FlowConstruct mockFlowConstruct;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InboundEndpoint mockInboundEndpoint;

    @Test
    public void testTopicReceiverShouldBeStartedOnlyInPrimaryNode() throws Exception
    {
        when(mockJmsConnector.getTopicResolver().isTopic(mockInboundEndpoint)).thenReturn(true);
        when(mockInboundEndpoint.getConnector()).thenReturn(mockJmsConnector);
        XaTransactedJmsMessageReceiver messageReceiver = new XaTransactedJmsMessageReceiver(mockJmsConnector, mockFlowConstruct, mockInboundEndpoint);
        assertThat("receiver must be started only in primary node", messageReceiver.shouldConsumeInEveryNode(), is(false));
    }

    @Test
    public void testQueueReceiverShouldBeStartedInEveryNode() throws Exception
    {
        when(mockJmsConnector.getTopicResolver().isTopic(mockInboundEndpoint)).thenReturn(false);
        when(mockInboundEndpoint.getConnector()).thenReturn(mockJmsConnector);
        XaTransactedJmsMessageReceiver messageReceiver = new XaTransactedJmsMessageReceiver(mockJmsConnector, mockFlowConstruct, mockInboundEndpoint);
        assertThat("receiver must be started only in primary node", messageReceiver.shouldConsumeInEveryNode(), is(true));
    }


}
