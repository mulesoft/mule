/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.routing.MessageFilter;
import org.mule.source.StartableCompositeMessageSource;
import org.mule.source.StartableCompositeMessageSourceTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ServiceCompositeMessageSourceTestCase extends StartableCompositeMessageSourceTestCase
{

    protected StartableCompositeMessageSource getCompositeSource()
    {
        return new ServiceCompositeMessageSource();
    }

    @Test
    public void testInboundRouters() throws MuleException
    {
        ServiceCompositeMessageSource serviceCompositeMessageSource = (ServiceCompositeMessageSource) compositeSource;
        serviceCompositeMessageSource.setListener(listener);
        serviceCompositeMessageSource.addMessageProcessor(new AppendingInterceptingMessageProcessor("one"));
        serviceCompositeMessageSource.addMessageProcessor(new AppendingInterceptingMessageProcessor("two"));

        serviceCompositeMessageSource.addSource(source);
        serviceCompositeMessageSource.initialise();
        serviceCompositeMessageSource.start();

        source.triggerSource();

        assertEquals(TEST_MESSAGE + "one" + "two", listener.event.getMessageAsString());
    }

    @Test
    public void testInboundRouterCatchAll() throws MuleException
    {
        ServiceCompositeMessageSource serviceCompositeMessageSource = (ServiceCompositeMessageSource) compositeSource;
        serviceCompositeMessageSource.setListener(listener);
        serviceCompositeMessageSource.setCatchAllStrategy(listener2);
        serviceCompositeMessageSource.addMessageProcessor(new AppendingInterceptingMessageProcessor("one"));
        serviceCompositeMessageSource.addMessageProcessor(new TestMessageFilter(false));

        serviceCompositeMessageSource.addSource(source);
        serviceCompositeMessageSource.initialise();
        serviceCompositeMessageSource.start();

        source.triggerSource();

        assertNull(listener.event);
        assertNotNull(listener2.event);
        assertEquals(TEST_MESSAGE + "one", listener2.event.getMessageAsString());
    }

    class AppendingInterceptingMessageProcessor extends AbstractInterceptingMessageProcessor
    {

        String appendString;

        public AppendingInterceptingMessageProcessor(String appendString)
        {
            this.appendString = appendString;
        }

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return processNext(new DefaultMuleEvent(new DefaultMuleMessage(event.getMessage().getPayload()
                                                                           + appendString, ServiceCompositeMessageSourceTestCase.muleContext),
                event));
        }
    }

    class TestMessageFilter extends MessageFilter
    {
        public TestMessageFilter(final boolean accept)
        {
            super(new Filter()
            {
                public boolean accept(MuleMessage message)
                {
                    return accept;
                }
            });
        }
    }

}
