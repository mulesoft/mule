/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.Connector;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.MuleTestUtils;

public class ValidatorTestCase extends AbstractFlowConstuctTestCase
{
    private Validator validator;
    protected Connector testConnector;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        final OutboundEndpoint testOutboundEndpoint = MuleTestUtils.getTestOutboundEndpoint(
            MessageExchangePattern.ONE_WAY, muleContext);
        testConnector = testOutboundEndpoint.getConnector();
        muleContext.getRegistry().registerConnector(testConnector);
        testConnector.start();

        validator = new Validator("test-validator", muleContext, directInboundMessageSource,
            testOutboundEndpoint, new PayloadTypeFilter(Integer.class), "#[string:GOOD:#[message:payload]]",
            "#[string:BAD:#[message:payload]]");
    }

    @Override
    protected AbstractFlowConstruct getFlowConstruct() throws Exception
    {
        return validator;
    }

    public void testAck() throws Exception
    {
        validator.initialise();
        validator.start();
        MuleEvent response = directInboundMessageSource.process(MuleTestUtils.getTestInboundEvent(
            Integer.valueOf(123), muleContext));

        assertEquals("GOOD:123", response.getMessageAsString());
    }

    public void testNack() throws Exception
    {
        validator.initialise();
        validator.start();
        MuleEvent response = directInboundMessageSource.process(MuleTestUtils.getTestInboundEvent("abc",
            muleContext));

        assertEquals("BAD:abc", response.getMessageAsString());
    }
}
