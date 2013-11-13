/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.util.ExceptionUtils;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.AbstractApplicationContext;

public class SpringEventsTestCase extends FunctionalTestCase
{

    protected static final int DEFAULT_LATCH_TIMEOUT = 10000;

    private static final int NUMBER_OF_MESSAGES = 10;
    volatile AtomicInteger eventCounter1;
    volatile AtomicInteger eventCounter2;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        eventCounter1 = new AtomicInteger(0);
        eventCounter2 = new AtomicInteger(0);
    }

    @Override
    protected String getConfigFile()
    {
        return "mule-events-app-context.xml";
    }

    @Test
    public void testManagerIsInstanciated() throws Exception
    {
        assertTrue(muleContext.isInitialised());
        assertTrue(muleContext.isStarted());
        assertNotNull(muleContext.getRegistry().lookupObject(
            AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME));
    }

    @Test
    public void testRemovingListeners() throws Exception
    {
        TestSubscriptionEventBean subscriptionBean = (TestSubscriptionEventBean) muleContext.getRegistry()
            .lookupObject("testSubscribingEventBean1");
        assertNotNull(subscriptionBean);
        MuleEventMulticaster multicaster = (MuleEventMulticaster) muleContext.getRegistry().lookupObject(
            AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME);
        assertNotNull(multicaster);

        Latch whenFinished = new Latch();
        subscriptionBean.setEventCallback(new CountingEventCallback(eventCounter1, 1, whenFinished));

        multicaster.removeApplicationListener(subscriptionBean);

        MuleClient client = muleContext.getClient();
        client.send("vm://event.multicaster", "Test Spring MuleEvent", null);

        assertEquals(0, eventCounter1.get());

        multicaster.addApplicationListener(subscriptionBean);
        client.send("vm://event.multicaster", "Test Spring MuleEvent", null);

        assertTrue(whenFinished.await(DEFAULT_LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(1, eventCounter1.get());
        eventCounter1.set(0);

        multicaster.removeAllListeners();
        client.send("vm://event.multicaster", "Test Spring MuleEvent", null);

        assertEquals(0, eventCounter1.get());
        multicaster.addApplicationListener(subscriptionBean);
        // context.refresh();
        subscriptionBean.setEventCallback(null);
    }

    @Test
    public void testReceivingANonSubscriptionMuleEvent() throws Exception
    {
        TestMuleEventBean bean = (TestMuleEventBean) muleContext.getRegistry().lookupObject(
            "testNonSubscribingMuleEventBean");
        assertNotNull(bean);

        // register a callback
        Latch whenFinished = new Latch();
        bean.setEventCallback(new CountingEventCallback(eventCounter1, 1, whenFinished));

        MuleClient client = muleContext.getClient();
        client.send("vm://event.multicaster", "Test Spring MuleEvent", null);

        whenFinished.await(DEFAULT_LATCH_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(1, eventCounter1.get());
    }

    @Test
    public void testReceivingASpringEvent() throws Exception
    {
        TestApplicationEventBean bean = (TestApplicationEventBean) muleContext.getRegistry().lookupObject(
            "testEventSpringBean");
        assertNotNull(bean);

        final Latch whenFinished = new Latch();
        EventCallback callback = new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object o) throws Exception
            {
                assertNull(context);
                if (o instanceof TestApplicationEvent)
                {
                    if (eventCounter1.incrementAndGet() == 1)
                    {
                        whenFinished.countDown();
                    }
                }
            }
        };

        bean.setEventCallback(callback);

        ApplicationContext context = ((MuleEventMulticaster) muleContext.getRegistry().lookupObject(
            "applicationEventMulticaster")).applicationContext;
        context.publishEvent(new TestApplicationEvent(context));

        whenFinished.await(DEFAULT_LATCH_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(1, eventCounter1.get());
    }

    @Test
    public void testReceivingAllEvents() throws Exception
    {
        TestAllEventBean bean = (TestAllEventBean) muleContext.getRegistry().lookupObject("testAllEventBean");
        assertNotNull(bean);

        Latch whenFinished = new Latch();
        bean.setEventCallback(new CountingEventCallback(eventCounter1, 2, whenFinished));

        MuleClient client = muleContext.getClient();
        client.send("vm://event.multicaster", "Test Spring MuleEvent", null);
        ApplicationContext context = ((MuleEventMulticaster) muleContext.getRegistry().lookupObject(
            "applicationEventMulticaster")).applicationContext;
        context.publishEvent(new TestApplicationEvent(context));

        whenFinished.await(DEFAULT_LATCH_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(2, eventCounter1.get());
    }

    @Test
    public void testReceivingASubscriptionEvent() throws Exception
    {
        TestSubscriptionEventBean subscriptionBean = (TestSubscriptionEventBean) muleContext.getRegistry()
            .lookupObject("testSubscribingEventBean1");
        assertNotNull(subscriptionBean);

        Latch whenFinished = new Latch();
        subscriptionBean.setEventCallback(new CountingEventCallback(eventCounter1, 1, whenFinished));

        MuleClient client = muleContext.getClient();
        client.send("vm://event.multicaster", "Test Spring MuleEvent", null);

        whenFinished.await(DEFAULT_LATCH_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(1, eventCounter1.get());
    }

    @Test
    public void testReceiveAndPublishEvent() throws Exception
    {
        TestSubscriptionEventBean bean1 = (TestSubscriptionEventBean) muleContext.getRegistry().lookupObject(
            "testSubscribingEventBean1");
        assertNotNull(bean1);

        final Latch whenFinished1 = new Latch();
        EventCallback callback = new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object o) throws Exception
            {
                MuleApplicationEvent returnEvent = new MuleApplicationEvent("MuleEvent from a spring bean",
                    "vm://testBean2");
                MuleApplicationEvent e = (MuleApplicationEvent) o;
                e.getApplicationContext().publishEvent(returnEvent);
                if (eventCounter1.incrementAndGet() == NUMBER_OF_MESSAGES)
                {
                    whenFinished1.countDown();
                }
            }
        };
        bean1.setEventCallback(callback);

        TestSubscriptionEventBean bean2 = (TestSubscriptionEventBean) muleContext.getRegistry().lookupObject(
            "testSubscribingEventBean2");
        assertNotNull(bean2);

        Latch whenFinished2 = new Latch();
        bean2.setEventCallback(new CountingEventCallback(eventCounter2, NUMBER_OF_MESSAGES, whenFinished2));

        // send asynchronously
        this.doSend("vm://event.multicaster", "Test Spring MuleEvent", NUMBER_OF_MESSAGES);

        whenFinished1.await(DEFAULT_LATCH_TIMEOUT, TimeUnit.MILLISECONDS);
        whenFinished2.await(DEFAULT_LATCH_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(NUMBER_OF_MESSAGES, eventCounter1.get());
        assertEquals(NUMBER_OF_MESSAGES, eventCounter2.get());
    }

    @Test
    public void testPublishOnly() throws Exception
    {
        final MuleApplicationEvent event = new MuleApplicationEvent("MuleEvent from a spring bean",
            "vm://testBean2");

        TestSubscriptionEventBean bean2 = (TestSubscriptionEventBean) muleContext.getRegistry().lookupObject(
            "testSubscribingEventBean2");
        assertNotNull(bean2);

        Latch whenFinished = new Latch();
        bean2.setEventCallback(new CountingEventCallback(eventCounter1, NUMBER_OF_MESSAGES, whenFinished));

        // publish asynchronously
        this.doPublish(event, NUMBER_OF_MESSAGES);

        whenFinished.await(DEFAULT_LATCH_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(NUMBER_OF_MESSAGES, eventCounter1.get());
    }

    @Test
    public void testPublishWithEventAwareTransformer() throws Exception
    {
        CountDownLatch transformerLatch = new CountDownLatch(1);

        TestEventAwareTransformer trans = new TestEventAwareTransformer();
        trans.setLatch(transformerLatch);
        muleContext.getRegistry().registerTransformer(trans);

        MuleApplicationEvent event = new MuleApplicationEvent("MuleEvent from a spring bean",
            "vm://testBean2?transformers=dummyTransformer");

        TestSubscriptionEventBean bean2 = (TestSubscriptionEventBean) muleContext.getRegistry().lookupObject(
            "testSubscribingEventBean2");
        assertNotNull(bean2);

        Latch whenFinished = new Latch();
        bean2.setEventCallback(new CountingEventCallback(eventCounter1, 1, whenFinished));

        // publish asynchronously
        this.doPublish(event, 1);

        whenFinished.await(DEFAULT_LATCH_TIMEOUT, TimeUnit.MILLISECONDS);
        assertTrue(transformerLatch.await(DEFAULT_LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(1, eventCounter1.get());
    }

    // asynchronously publish the given event to the ApplicationContext for 'count' number of times
    protected void doPublish(final ApplicationEvent event, final int count)
    {
        Runnable publisher = new Runnable()
        {
            @Override
            public void run()
            {
                for (int i = 0; i < count; i++)
                {
                    ApplicationContext context = null;
                    try
                    {
                        context = ((MuleEventMulticaster) muleContext.getRegistry().lookupObject(
                            "applicationEventMulticaster")).applicationContext;
                        context.publishEvent(event);
                    }
                    catch (Exception e)
                    {
                        fail(e.getMessage());
                    }
                }
            }
        };

        Executors.newSingleThreadExecutor().execute(publisher);
    }

    // asynchronously send the payload to the given Mule URL for 'count' number of times
    protected void doSend(final String url, final Object payload, final int count)
    {
        Runnable sender = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    MuleClient client = muleContext.getClient();
                    for (int i = 0; i < count; i++)
                    {
                        client.send(url, payload, null);
                    }
                }
                catch (MuleException ex)
                {
                    fail(ExceptionUtils.getStackTrace(ex));
                }
            }
        };

        // execute in background
        Executors.newSingleThreadExecutor().execute(sender);
    }

    /*
     * This callback counts how many times an MuleEvent was received. If a maximum
     * number has been reached, the given CountDownLatch is counted down. When
     * passing in a Latch (CountDownLatch(1)) this acts just like a sempahore for the
     * caller.
     */
    public static class CountingEventCallback implements EventCallback
    {
        private final AtomicInteger counter;
        private final int maxCount;
        private final CountDownLatch finished;

        public CountingEventCallback(AtomicInteger counter, int maxCount, CountDownLatch whenFinished)
        {
            super();
            this.counter = counter;
            this.maxCount = maxCount;
            this.finished = whenFinished;
        }

        @Override
        public void eventReceived(MuleEventContext context, Object o) throws Exception
        {
            // apparently beans get an extra ContextRefreshedEvent during
            // startup;
            // this messes up our event counts.
            if (!(o instanceof ContextRefreshedEvent))
            {
                if (counter.incrementAndGet() == maxCount && finished != null)
                {
                    finished.countDown();
                }
            }
        }
    }

    /*
     * A simple Transformer that counts down a Latch to indicate that it has been
     * called.
     */
    public static class TestEventAwareTransformer extends AbstractMessageTransformer
    {
        private CountDownLatch latch;

        public TestEventAwareTransformer()
        {
            this.setName("dummyTransformer");
        }

        @Override
        public Object clone() throws CloneNotSupportedException
        {
            TestEventAwareTransformer clone = (TestEventAwareTransformer) super.clone();
            // we MUST share the latch for this test since we obviously want to
            // wait
            // for it.
            clone.setLatch(latch);
            return clone;
        }

        public CountDownLatch getLatch()
        {
            return latch;
        }

        public void setLatch(CountDownLatch latch)
        {
            this.latch = latch;
        }

        @Override
        public Object transformMessage(MuleMessage message, String outputEncoding)
        {
            assertNotNull(message);

            if (latch != null)
            {
                latch.countDown();
            }

            return message;
        }
    }

    /*
     * A simple custom ApplicationEvent for sending
     */
    public static class TestApplicationEvent extends ApplicationEvent
    {
        private static final long serialVersionUID = 1L;

        public TestApplicationEvent(Object source)
        {
            super(source);
        }
    }
}
