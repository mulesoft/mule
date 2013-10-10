/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing;

import static org.junit.Assert.assertEquals;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class RoundRobinTestCase extends AbstractMuleContextTestCase
{
    private final static int NUMBER_OF_ROUTES = 10;
    private final static int NUMBER_OF_MESSAGES = 10;
    private final AtomicInteger messageNumber = new AtomicInteger(0);

    public RoundRobinTestCase()
    {
        setStartContext(true);
    }

    @Test
    public void testRoundRobin() throws Exception
    {
        RoundRobin rr = new RoundRobin();
        MuleSession session = getTestSession(null, muleContext);
        List<TestProcessor> routes = new ArrayList<TestProcessor>(NUMBER_OF_ROUTES);
        for (int i = 0; i < NUMBER_OF_ROUTES; i++)
        {
            routes.add(new TestProcessor());
        }
        rr.setRoutes(new ArrayList<MessageProcessor>(routes));
        List<Thread> threads = new ArrayList<Thread>(NUMBER_OF_ROUTES);
        for (int i = 0; i < NUMBER_OF_ROUTES; i++)
        {
            threads.add(new Thread(new TestDriver(session, rr, NUMBER_OF_MESSAGES, getTestService())));
        }
        for (Thread t : threads)
        {
            t.start();
        }
        for (Thread t : threads)
        {
            t.join();
        }
        for (TestProcessor route : routes)
        {
            assertEquals(NUMBER_OF_MESSAGES, route.getCount());
        }
    }

    class TestDriver implements Runnable
    {
        private MessageProcessor target;
        private int numMessages;
        private MuleSession session;
        private FlowConstruct flowConstruct;

        TestDriver(MuleSession session, MessageProcessor target, int numMessages, FlowConstruct flowConstruct)
        {
            this.target = target;
            this.numMessages = numMessages;
            this.session = session;
            this.flowConstruct = flowConstruct;
        }

        @Override
        public void run()
        {
            for (int i = 0; i < numMessages; i++)
            {
                MuleMessage msg = new DefaultMuleMessage(TEST_MESSAGE + messageNumber.getAndIncrement(),
                    muleContext);
                MuleEvent event = new DefaultMuleEvent(msg, MessageExchangePattern.REQUEST_RESPONSE,
                    flowConstruct, session);
                try
                {
                    target.process(event);
                }
                catch (MuleException e)
                {
                    // this is expected
                }
            }
        }
    }

    static class TestProcessor implements MessageProcessor
    {
        private int count;
        private List<Object> payloads = new ArrayList<Object>();

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            payloads.add(event.getMessage().getPayload());
            count++;
            if (count % 3 == 0)
            {
                throw new DefaultMuleException("Mule Exception!");
            }
            return null;
        }

        public int getCount()
        {
            return count;
        }
    }
}
