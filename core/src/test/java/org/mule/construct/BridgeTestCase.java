/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.construct;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.Connector;
import org.mule.tck.MuleTestUtils;

import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BridgeTestCase extends AbstractFlowConstuctTestCase
{
    private Bridge bridge;
    protected Connector testConnector;

    @SuppressWarnings("unchecked")
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        final OutboundEndpoint testOutboundEndpoint = MuleTestUtils.getTestOutboundEndpoint(
            MessageExchangePattern.REQUEST_RESPONSE, muleContext);
        testConnector = testOutboundEndpoint.getConnector();
        muleContext.getRegistry().registerConnector(testConnector);
        testConnector.start();

        bridge = new Bridge("test-bridge", muleContext, directInboundMessageSource, testOutboundEndpoint,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST, MessageExchangePattern.REQUEST_RESPONSE, false);
    }

    @Override
    protected AbstractFlowConstruct getFlowConstruct() throws Exception
    {
        return bridge;
    }

    @Test
    public void testProcess() throws Exception
    {
        bridge.initialise();
        bridge.start();
        MuleEvent response = directInboundMessageSource.process(MuleTestUtils.getTestEvent("hello",
            muleContext));

        assertEquals("hello", response.getMessageAsString());
    }
}
