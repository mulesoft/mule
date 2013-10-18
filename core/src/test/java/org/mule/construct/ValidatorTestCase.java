/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.construct;

import static org.junit.Assert.assertEquals;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.Connector;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.MuleTestUtils;

import java.util.Collections;

import org.junit.Ignore;
import org.junit.Test;

public class ValidatorTestCase extends AbstractFlowConstuctTestCase
{
    private Validator validator;
    protected Connector testConnector;
    protected OutboundEndpoint testOutboundEndpoint;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        testOutboundEndpoint = MuleTestUtils.getTestOutboundEndpoint(MessageExchangePattern.ONE_WAY,
            muleContext);
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

    @Test
    public void testAck() throws Exception
    {
        validator.initialise();
        validator.start();
        final MuleEvent response = directInboundMessageSource.process(MuleTestUtils.getTestEvent(
            Integer.valueOf(123), muleContext));

        assertEquals("GOOD:123", response.getMessageAsString());
    }

    @Test
    public void testNack() throws Exception
    {
        validator.initialise();
        validator.start();
        final MuleEvent response = directInboundMessageSource.process(MuleTestUtils.getTestEvent("abc",
            muleContext));

        assertEquals("BAD:abc", response.getMessageAsString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testErrorWithoutExpression() throws Exception
    {
        final OutboundEndpoint failingOutboundEndpoint = MuleTestUtils.getTestOutboundEndpoint("failing-oe",
            muleContext, "test://AlwaysFail", Collections.EMPTY_LIST, null, Collections.EMPTY_MAP,
            testConnector);

        validator = new Validator("test-validator", muleContext, directInboundMessageSource,
            failingOutboundEndpoint, new PayloadTypeFilter(Integer.class),
            "#[string:GOOD:#[message:payload]]", "#[string:BAD:#[message:payload]]");

        testAck();
    }

    @Test
    public void testErrorWithExpression() throws Exception
    {
        final OutboundEndpoint failingOutboundEndpoint = MuleTestUtils.getTestOutboundEndpoint(
            MessageExchangePattern.REQUEST_RESPONSE, muleContext, "test://AlwaysFail", testConnector);

        validator = new Validator("test-validator", muleContext, directInboundMessageSource,
            failingOutboundEndpoint, new PayloadTypeFilter(Integer.class),
            "#[string:GOOD:#[message:payload]]", "#[string:BAD:#[message:payload]]",
            "#[string:ERROR:#[message:payload]]");

        validator.initialise();
        validator.start();
        final MuleEvent response = directInboundMessageSource.process(MuleTestUtils.getTestEvent(123,
            muleContext));

        assertEquals("ERROR:123", response.getMessageAsString());
    }

    @Test
    public void testMELExpression() throws InitialisationException
    {
        validator = new Validator("test-validator", muleContext, directInboundMessageSource,
            testOutboundEndpoint, new PayloadTypeFilter(Integer.class), "#['hi']", "#['hi']");

        validator.initialise();
    }

    @Test
    @Ignore
    public void testMELExpressionNoDelimiter() throws InitialisationException
    {
        validator = new Validator("test-validator", muleContext, directInboundMessageSource,
            testOutboundEndpoint, new PayloadTypeFilter(Integer.class), "'hi'", "'hi'");

        validator.initialise();
    }
}
