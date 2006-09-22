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

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

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

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ServerNotificationsTestCase extends AbstractMuleTestCase
        implements ModelNotificationListener, ManagerNotificationListener {


    private boolean managerStopped = false;
    private boolean modelStopped = false;
    private int componentStartedCount = 0;
    private int customNotificationCount = 0;

    public void testStandardNotifications() throws Exception {

        UMOManager m = getManager(true);
        m.start();
        m.registerListener(this);
        m.stop();
        assertTrue(modelStopped);
        assertTrue(managerStopped);
    }

    public void testStandardNotificationsWithSubscription() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);
        UMOManager m = getManager(true);
        m.start();
        m.registerListener(new ComponentNotificationListener() {
            public void onNotification(UMOServerNotification notification) {
                if (notification.getAction() == ComponentNotification.COMPONENT_STARTED) {
                    componentStartedCount++;
                    assertEquals("component1", notification.getResourceIdentifier());
                    latch.countDown();
                }
            }
        }, "component1");

        m.getModel().registerComponent(getTestDescriptor("component2", Apple.class.getName()));
        m.getModel().registerComponent(getTestDescriptor("component1", Apple.class.getName()));

        //Wait for the notifcation event to be fired as they are queue
        latch.await(2000, TimeUnit.MILLISECONDS);
        assertEquals(1, componentStartedCount);
    }

    public void testStandardNotificationsWithWildcardSubscription() throws Exception {

        final CountDownLatch latch = new CountDownLatch(2);

        UMOManager m = getManager(true);
        m.start();
        m.registerListener(new ComponentNotificationListener() {
            public void onNotification(UMOServerNotification notification) {
                if (notification.getAction() == ComponentNotification.COMPONENT_STARTED) {
                    componentStartedCount++;
                    assertFalse("noMatchComponent".equals(notification.getResourceIdentifier()));
                    latch.countDown();
                }
            }
        }, "component*");

        m.getModel().registerComponent(getTestDescriptor("component2", Apple.class.getName()));
        m.getModel().registerComponent(getTestDescriptor("component1", Apple.class.getName()));
        m.getModel().registerComponent(getTestDescriptor("noMatchComponent", Apple.class.getName()));

        //Wait for the notifcation event to be fired as they are queue
        latch.await(2000, TimeUnit.MILLISECONDS);
        assertEquals(2, componentStartedCount);
    }

    public void testCustomNotifications() throws Exception {

        final CountDownLatch latch = new CountDownLatch(2);

        UMOManager m = getManager(true);
        m.start();
        m.registerListener(new DummyNotificationListener() {
            public void onNotification(UMOServerNotification notification) {
                if (notification.getAction() == DummyNotification.EVENT_RECEIVED) {
                    customNotificationCount++;
                    assertEquals("hello", notification.getSource());
                    latch.countDown();
                }
            }
        });

        m.fireNotification(new DummyNotification("hello", DummyNotification.EVENT_RECEIVED));
        m.fireNotification(new DummyNotification("hello", DummyNotification.EVENT_RECEIVED));

        //Wait for the notifcation event to be fired as they are queue
        latch.await(2000, TimeUnit.MILLISECONDS);
        assertEquals(2, customNotificationCount);
    }

    public void testCustomNotificationsWithWildcardSubscription() throws Exception {

        final CountDownLatch latch = new CountDownLatch(2);

        UMOManager m = getManager(true);
        m.start();
        m.registerListener(new DummyNotificationListener() {
            public void onNotification(UMOServerNotification notification) {
                if (notification.getAction() == DummyNotification.EVENT_RECEIVED) {
                    customNotificationCount++;
                    assertFalse("e quick bro".equals(notification.getResourceIdentifier()));
                    latch.countDown();
                }
            }
        }, "* quick brown*");

        m.fireNotification(new DummyNotification("the quick brown fox jumped over the lazy dog", DummyNotification.EVENT_RECEIVED));
        m.fireNotification(new DummyNotification("e quick bro", DummyNotification.EVENT_RECEIVED));
        m.fireNotification(new DummyNotification(" quick brown", DummyNotification.EVENT_RECEIVED));

        //Wait for the notifcation event to be fired as they are queue
        latch.await(2000, TimeUnit.MILLISECONDS);
        assertEquals(2, customNotificationCount);
    }

    public void onNotification(UMOServerNotification notification) {
        if (notification.getAction() == ModelNotification.MODEL_STOPPED) {
            modelStopped = true;
        } else if (notification.getAction() == ManagerNotification.MANAGER_STOPPED) {
            managerStopped = true;
        }
    }

    public static interface DummyNotificationListener extends CustomNotificationListener {
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

