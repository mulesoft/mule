/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.spring.events;

import org.mule.extras.client.MuleClient;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.umo.UMOEventContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class SpringEventsTestCase extends AbstractMuleTestCase
{
    private static final int NUMBER_OF_MESSAGES = 10;
    private static int eventCount = 0;
    private static int eventCount2 = 0;
    private static ClassPathXmlApplicationContext context;
    public Object lock = new Object();

    protected void doSetUp() throws Exception
    {
        context = new ClassPathXmlApplicationContext(getConfigResources());
        eventCount = 0;
        eventCount2 = 0;
    }

    protected String getConfigResources()
    {
        return "mule-events-app-context.xml";
    }

    public void testRemovingListeners() throws Exception
    {
        TestSubscriptionEventBean subscriptionBean = (TestSubscriptionEventBean) context.getBean("testSubscribingEventBean1");
        assertNotNull(subscriptionBean);
        MuleEventMulticaster multicaster = (MuleEventMulticaster) context.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME);
        assertNotNull(multicaster);
        // when an event is received by 'testEventBean1' this callback will be
        // invoked
        EventCallback callback = new EventCallback() {
            public void eventReceived(UMOEventContext context, Object o) throws Exception
            {
                eventCount++;
            }
        };
        subscriptionBean.setEventCallback(callback);

        multicaster.removeApplicationListener(subscriptionBean);
        MuleClient client = new MuleClient();
        client.send("vm://event.multicaster", "Test Spring Event", null);

        afterPublishEvent();
        assertEquals(0, eventCount);

        multicaster.addApplicationListener(subscriptionBean);
        client.send("vm://event.multicaster", "Test Spring Event", null);

        afterPublishEvent();
        assertEquals(1, eventCount);
        eventCount = 0;

        multicaster.removeAllListeners();
        client.send("vm://event.multicaster", "Test Spring Event", null);

        afterPublishEvent();
        assertEquals(0, eventCount);
        multicaster.addApplicationListener(subscriptionBean);
        context.refresh();
        subscriptionBean.setEventCallback(null);
    }

    public void testReceivingANonSubscriptionMuleEvent() throws Exception
    {
        TestMuleEventBean bean = (TestMuleEventBean) context.getBean("testNonSubscribingMuleEventBean");
        assertNotNull(bean);
        // when an event is received by 'testEventBean1' this callback will be
        // invoked
        EventCallback callback = new EventCallback() {
            public void eventReceived(UMOEventContext context, Object o) throws Exception
            {
                eventCount++;
            }
        };
        bean.setEventCallback(callback);
        MuleClient client = new MuleClient();
        client.send("vm://event.multicaster", "Test Spring Event", null);

        afterPublishEvent();
        assertEquals(1, eventCount);
    }

    public void testReceivingASpringEvent() throws Exception
    {
        TestApplicationEventBean bean = (TestApplicationEventBean) context.getBean("testEventSpringBean");
        assertNotNull(bean);
        // when an event is received by 'testEventBean1' this callback will be
        // invoked
        EventCallback callback = new EventCallback() {
            public void eventReceived(UMOEventContext context, Object o) throws Exception
            {
                eventCount++;
                assertNull(context);
                assertTrue(o instanceof ContextRefreshedEvent);
            }
        };
        bean.setEventCallback(callback);
        context.publishEvent(new ContextRefreshedEvent(context));
        afterPublishEvent();
        assertEquals(1, eventCount);
    }

    public void testReceivingASubscriptionEvent() throws Exception
    {
        TestSubscriptionEventBean subscriptionBean = (TestSubscriptionEventBean) context.getBean("testSubscribingEventBean1");
        assertNotNull(subscriptionBean);
        // when an event is received by 'testEventBean1' this callback will be
        // invoked
        EventCallback callback = new EventCallback() {
            public void eventReceived(UMOEventContext context, Object o) throws Exception
            {
                eventCount++;
            }
        };
        subscriptionBean.setEventCallback(callback);
        MuleClient client = new MuleClient();
        client.send("vm://event.multicaster", "Test Spring Event", null);
        afterPublishEvent();
        assertEquals(1, eventCount);
    }

    public void testReceiveAndPublishEvent() throws Exception
    {
        TestSubscriptionEventBean bean1 = (TestSubscriptionEventBean) context.getBean("testSubscribingEventBean1");
        assertNotNull(bean1);
        EventCallback callback = new EventCallback() {
            public void eventReceived(UMOEventContext context, Object o) throws Exception
            {
                eventCount++;
                MuleApplicationEvent returnEvent = new MuleApplicationEvent("Event from a spring bean",
                                                                            "vm://testBean2");
                MuleApplicationEvent e = (MuleApplicationEvent) o;
                e.getApplicationContext().publishEvent(returnEvent);
            }
        };
        bean1.setEventCallback(callback);

        TestSubscriptionEventBean bean2 = (TestSubscriptionEventBean) context.getBean("testSubscribingEventBean2");
        assertNotNull(bean2);
        EventCallback callback2 = new EventCallback() {
            public void eventReceived(UMOEventContext context, Object o) throws Exception
            {
                eventCount2++;
                if(eventCount2==NUMBER_OF_MESSAGES) {
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
            }
        };
        bean2.setEventCallback(callback2);

        MuleClient client = new MuleClient();
        for(int i = 0;i < NUMBER_OF_MESSAGES;i++) {
            client.send("vm://event.multicaster", "Test Spring Event", null);
        }
        // give it a second for the event to process
        synchronized (lock) {
            lock.wait(3000);
        }
        assertEquals(NUMBER_OF_MESSAGES, eventCount);
        assertEquals(NUMBER_OF_MESSAGES, eventCount2);
    }

    public void testPublishOnly() throws Exception
    {
        MuleApplicationEvent event = new MuleApplicationEvent("Event from a spring bean", "vm://testBean2");

        TestSubscriptionEventBean bean2 = (TestSubscriptionEventBean) context.getBean("testSubscribingEventBean2");
        assertNotNull(bean2);
        EventCallback callback = new EventCallback() {
            public void eventReceived(UMOEventContext context, Object o) throws Exception
            {
                eventCount++;
                if(eventCount==NUMBER_OF_MESSAGES) {
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
            }
        };
        bean2.setEventCallback(callback);

        for(int i = 0;i < NUMBER_OF_MESSAGES;i++) {
            context.publishEvent(event);
        }
        // give it some time for the event to process
        synchronized (lock) {
            lock.wait(5000);
        }
        assertEquals(NUMBER_OF_MESSAGES, eventCount);
    }

    protected void afterPublishEvent() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {

        }
    }
}
