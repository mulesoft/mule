/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management;

import org.mule.impl.internal.notifications.ComponentNotification;
import org.mule.impl.internal.notifications.ComponentNotificationListener;
import org.mule.impl.internal.notifications.CustomNotification;
import org.mule.impl.internal.notifications.CustomNotificationListener;
import org.mule.impl.internal.notifications.ManagerNotification;
import org.mule.impl.internal.notifications.ManagerNotificationListener;
import org.mule.impl.internal.notifications.ModelNotification;
import org.mule.impl.internal.notifications.ModelNotificationListener;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.manager.UMOServerNotification;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

public class ServerNotificationsTestCase extends AbstractMuleTestCase
    implements ModelNotificationListener, ManagerNotificationListener
{

    private final AtomicBoolean managerStopped = new AtomicBoolean(false);
    private final AtomicBoolean modelStopped = new AtomicBoolean(false);
    private final AtomicInteger componentStartedCount = new AtomicInteger(0);
    private final AtomicInteger customNotificationCount = new AtomicInteger(0);

    public void testStandardNotifications() throws Exception
    {
        UMOManager m = getManager(true);
        m.start();
        m.registerListener(this);
        m.stop();
        assertTrue(modelStopped.get());
        assertTrue(managerStopped.get());
    }

    public void testUnregistering() throws Exception
    {
        UMOManager m = getManager(true);
        m.start();
        m.registerListener(this);
        m.unregisterListener(this);
        m.stop();
        // these should still be false because we unregistered ourselves
        assertFalse(modelStopped.get());
        assertFalse(managerStopped.get());
    }

    public void testStandardNotificationsWithSubscription() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);
        UMOManager m = getManager(true);
        m.start();
        m.registerListener(new ComponentNotificationListener()
        {
            public void onNotification(UMOServerNotification notification)
            {
                if (notification.getAction() == ComponentNotification.COMPONENT_STARTED)
                {
                    componentStartedCount.incrementAndGet();
                    assertEquals("component1", notification.getResourceIdentifier());
                    latch.countDown();
                }
            }
        }, "component1");

        m.getModel().registerComponent(getTestDescriptor("component2", Apple.class.getName()));
        m.getModel().registerComponent(getTestDescriptor("component1", Apple.class.getName()));

        // Wait for the notifcation event to be fired as they are queue
        latch.await(2000, TimeUnit.MILLISECONDS);
        assertEquals(1, componentStartedCount.get());
    }

    public void testStandardNotificationsWithWildcardSubscription() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(2);

        UMOManager m = getManager(true);
        m.start();
        m.registerListener(new ComponentNotificationListener()
        {
            public void onNotification(UMOServerNotification notification)
            {
                if (notification.getAction() == ComponentNotification.COMPONENT_STARTED)
                {
                    componentStartedCount.incrementAndGet();
                    assertFalse("noMatchComponent".equals(notification.getResourceIdentifier()));
                    latch.countDown();
                }
            }
        }, "component*");

        m.getModel().registerComponent(getTestDescriptor("component2", Apple.class.getName()));
        m.getModel().registerComponent(getTestDescriptor("component1", Apple.class.getName()));
        m.getModel().registerComponent(getTestDescriptor("noMatchComponent", Apple.class.getName()));

        // Wait for the notifcation event to be fired as they are queue
        latch.await(2000, TimeUnit.MILLISECONDS);
        assertEquals(2, componentStartedCount.get());
    }

    public void testCustomNotifications() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(2);

        UMOManager m = getManager(true);
        m.start();
        m.registerListener(new DummyNotificationListener()
        {
            public void onNotification(UMOServerNotification notification)
            {
                if (notification.getAction() == DummyNotification.EVENT_RECEIVED)
                {
                    customNotificationCount.incrementAndGet();
                    assertEquals("hello", notification.getSource());
                    latch.countDown();
                }
            }
        });

        m.fireNotification(new DummyNotification("hello", DummyNotification.EVENT_RECEIVED));
        m.fireNotification(new DummyNotification("hello", DummyNotification.EVENT_RECEIVED));

        // Wait for the notifcation event to be fired as they are queue
        latch.await(2000, TimeUnit.MILLISECONDS);
        assertEquals(2, customNotificationCount.get());
    }

    public void testCustomNotificationsWithWildcardSubscription() throws Exception
    {

        final CountDownLatch latch = new CountDownLatch(2);

        UMOManager m = getManager(true);
        m.start();
        m.registerListener(new DummyNotificationListener()
        {
            public void onNotification(UMOServerNotification notification)
            {
                if (notification.getAction() == DummyNotification.EVENT_RECEIVED)
                {
                    customNotificationCount.incrementAndGet();
                    assertFalse("e quick bro".equals(notification.getResourceIdentifier()));
                    latch.countDown();
                }
            }
        }, "* quick brown*");

        m.fireNotification(new DummyNotification("the quick brown fox jumped over the lazy dog",
            DummyNotification.EVENT_RECEIVED));
        m.fireNotification(new DummyNotification("e quick bro", DummyNotification.EVENT_RECEIVED));
        m.fireNotification(new DummyNotification(" quick brown", DummyNotification.EVENT_RECEIVED));

        // Wait for the notifcation event to be fired as they are queue
        latch.await(2000, TimeUnit.MILLISECONDS);
        assertEquals(2, customNotificationCount.get());
    }

    public void onNotification(UMOServerNotification notification)
    {
        if (notification.getAction() == ModelNotification.MODEL_STOPPED)
        {
            modelStopped.set(true);
        }
        else if (notification.getAction() == ManagerNotification.MANAGER_STOPPED)
        {
            managerStopped.set(true);
        }
    }

    public static interface DummyNotificationListener extends CustomNotificationListener
    {
        // no methods
    }

    public class DummyNotification extends CustomNotification
    {
        /**
         * Serial version
         */
        private static final long serialVersionUID = -1117307108932589331L;

        public static final int EVENT_RECEIVED = -999999;

        public DummyNotification(String message, int action)
        {
            super(message, action);
            resourceIdentifier = message;
        }
    }
}
