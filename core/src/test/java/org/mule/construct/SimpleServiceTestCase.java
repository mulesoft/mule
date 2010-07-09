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

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.component.AbstractComponent;
import org.mule.tck.MuleTestUtils;
import org.mule.util.StringUtils;

public class SimpleServiceTestCase extends AbstractFlowConstuctTestCase
{
    private static final StringReverserComponent COMPONENT = new StringReverserComponent();
    private SimpleService simpleService;
    private DirectInboundMessageSource dims;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        dims = new DirectInboundMessageSource();
        simpleService = new SimpleService(muleContext, "test-simple-service", dims, COMPONENT);
    }

    @Override
    protected AbstractFlowConstruct getFlowConstruct()
    {
        return simpleService;
    }

    public void testSendWithSynchronousEndpoint() throws Exception
    {
        simpleService.initialise();
        simpleService.start();
        MuleEvent response = dims.process(MuleTestUtils.getTestEvent("hello",
            getSynchronousTestInboundEndpoint(), muleContext));

        assertEquals("olleh", response.getMessageAsString());
    }

    public void testSendEvenIfEndpointIsAsynchronous() throws Exception
    {
        simpleService.initialise();
        simpleService.start();
        MuleEvent response = dims.process(MuleTestUtils.getTestEvent("hello",
            getAsynchronousTestInboundEndpoint(), muleContext));

        assertEquals("olleh", response.getMessageAsString());
    }

    private InboundEndpoint getAsynchronousTestInboundEndpoint() throws Exception
    {
        return getTestInboundEndpoint(false);
    }

    private InboundEndpoint getSynchronousTestInboundEndpoint() throws Exception
    {
        return getTestInboundEndpoint(true);
    }

    private static class DirectInboundMessageSource implements MessageSource
    {
        private MessageProcessor listener;

        public void setListener(MessageProcessor listener)
        {
            this.listener = listener;
        }

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return listener.process(event);
        }

    }

    private static class StringReverserComponent extends AbstractComponent
    {
        @Override
        protected Object doInvoke(MuleEvent event) throws Exception
        {
            return StringUtils.reverse(event.getMessageAsString());
        }
    }

}
