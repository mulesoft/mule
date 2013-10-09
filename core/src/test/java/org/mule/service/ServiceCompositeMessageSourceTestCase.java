/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
