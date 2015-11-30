/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.polling;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.schedule.Scheduler;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.NullPayload;
import org.mule.transport.polling.MessageProcessorPollingOverride.NullOverride;
import org.mule.transport.polling.schedule.FixedFrequencySchedulerFactory;

import java.util.Collection;

import org.junit.Test;

public class PollingMessageSourceTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void nullResponseFromNestedMP() throws Exception
    {
        PollingMessageSource pollingMessageSource = createMessageSource(new MessageProcessor()
        {
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return null;
            }
        });

        SensingNullMessageProcessor flow = getSensingNullMessageProcessor();
        pollingMessageSource.setListener(flow);

        pollingMessageSource.poll();

        assertNull(flow.event);
    }

    @Test
    public void nullPayloadResponseFromNestedMP() throws Exception
    {

        PollingMessageSource pollingMessageSource = createMessageSource(new MessageProcessor()
        {
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return new DefaultMuleEvent(new DefaultMuleMessage(NullPayload.getInstance(), muleContext),
                    event);
            }
        });

        SensingNullMessageProcessor flow = getSensingNullMessageProcessor();
        pollingMessageSource.setListener(flow);

        pollingMessageSource.poll();

        assertNull(flow.event);
    }

    @Test
    public void emptyStringResponseFromNestedMP() throws Exception
    {

        PollingMessageSource pollingMessageSource = createMessageSource(new MessageProcessor()
        {
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return new DefaultMuleEvent(new DefaultMuleMessage("", muleContext), event);
            }
        });

        SensingNullMessageProcessor flow = getSensingNullMessageProcessor();
        pollingMessageSource.setListener(flow);

        pollingMessageSource.poll();

        assertNotNull(flow.event);
    }

    @Test
    public void disposeScheduler() throws Exception
    {

        PollingMessageSource pollinMessageSource = createMessageSource(new MessageProcessor()
        {
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return null;
            }
        });

        Collection<Scheduler> allSchedulers = getAllSchedulers();
        assertThat(allSchedulers.size(), is(1));

        Scheduler scheduler = allSchedulers.iterator().next();

        pollinMessageSource.stop();
        pollinMessageSource.dispose();

        assertThat(getAllSchedulers().size(), is(0));
        verify(scheduler).dispose();
    }

    private Collection<Scheduler> getAllSchedulers()
    {
        return muleContext.getRegistry().lookupObjects(Scheduler.class);
    }

    private PollingMessageSource createMessageSource(MessageProcessor processor)
        throws MuleException
    {
        FlowConstruct flowConstruct = mock(FlowConstruct.class);
        PollingMessageSource pollingMessageSource = new PollingMessageSource(muleContext, processor, new NullOverride(), schedulerFactory());
        pollingMessageSource.setFlowConstruct(flowConstruct);
        pollingMessageSource.initialise();
        return pollingMessageSource;
    }

    private FixedFrequencySchedulerFactory schedulerFactory()
    {
        FixedFrequencySchedulerFactory factory = new FixedFrequencySchedulerFactory(){
            @Override
            protected Scheduler doCreate(String name, final Runnable job)
            {
                return spy(super.doCreate(name,job));
            }
        };
        factory.setFrequency(1000);
        factory.setMuleContext(muleContext);
        return factory;
    }

}
