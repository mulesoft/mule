/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.context.notification;

import org.mule.tck.FunctionalTestCase;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.MuleException;
import org.mule.api.service.Service;
import org.mule.extras.client.MuleClient;

import java.util.Iterator;

public class ServerNotificationManagerTestCase extends FunctionalTestCase
{

    public static final String MULE_SYSTEM_MODEL = "_muleSystemModel";
    public static final String MODEL = "the-model";
    public static final String SERVICE = "the-service";
    private NotificationLogger notifications;

    protected String getConfigResources()
    {
        return "org/mule/test/integration/notifications/server-notification-manager-test.xml";
    }

    public void testNotifications() throws MuleException
    {
        MuleClient client = new MuleClient();
        assertNotNull(client.send("vm://in", "hello world", null));
        Service service = muleContext.getRegistry().lookupService("the-service");
        service.pause();
        service.resume();

        notifications = (NotificationLogger) muleContext.getRegistry().lookupObject("notificationLogger");
        // currently the main testing is done during shutdown
    }

    protected void suitePostTearDown() throws Exception
    {
        // allow shutdown to complete (or get concurrent mod errors and/or miss notifications)
        Thread.sleep(2000L);
        logNotifications();

        // we miss everything before model init as cannot register listener before that point
        // (if we use xml config)
        RestrictedNode spec = new Node()
                .parallel(new Node(ModelNotification.class, ModelNotification.MODEL_INITIALISING, MULE_SYSTEM_MODEL)
                        .serial(new Node(ModelNotification.class, ModelNotification.MODEL_INITIALISED, MULE_SYSTEM_MODEL)))
                .parallel(new Node(ModelNotification.class, ModelNotification.MODEL_INITIALISING, MODEL)
                        .serial(new Node(ModelNotification.class, ModelNotification.MODEL_INITIALISED, MODEL)))
                // no service initialising?
                .parallel(new Node(ServiceNotification.class, ServiceNotification.SERVICE_INITIALISED, SERVICE))
                .serial(new Node(ManagerNotification.class, ManagerNotification.MANAGER_STARTING))
                .serial(new Node(ManagerNotification.class, ManagerNotification.MANAGER_STARTING_MODELS)
                        .parallel(new Node(ModelNotification.class, ModelNotification.MODEL_STARTING, MODEL)
                            .serial(new Node(ModelNotification.class, ModelNotification.MODEL_STARTED, MODEL)))
                        .parallel(new Node(ModelNotification.class, ModelNotification.MODEL_STARTING, MULE_SYSTEM_MODEL)
                            .serial(new Node(ModelNotification.class, ModelNotification.MODEL_STARTED, MULE_SYSTEM_MODEL))))
                .serial(new Node(ManagerNotification.class, ManagerNotification.MANAGER_STARTED_MODELS))
                .serial(new Node(ServiceNotification.class, ServiceNotification.SERVICE_STARTED, SERVICE))
                .serial(new Node(ManagerNotification.class, ManagerNotification.MANAGER_STARTED))
                .serial(new Node(ServiceNotification.class, ServiceNotification.SERVICE_PAUSED, SERVICE))
                .serial(new Node(ServiceNotification.class, ServiceNotification.SERVICE_RESUMED, SERVICE))
                .serial(new Node(ManagerNotification.class, ManagerNotification.MANAGER_DISPOSING))
                .serial(new Node(ManagerNotification.class, ManagerNotification.MANAGER_STOPPING))
                .serial(new Node(ManagerNotification.class, ManagerNotification.MANAGER_STOPPING_MODELS)
                        .parallel(new Node(ModelNotification.class, ModelNotification.MODEL_STOPPING, MODEL)
                            .serial(new Node(ModelNotification.class, ModelNotification.MODEL_STOPPED, MODEL)))
                        .parallel(new Node(ModelNotification.class, ModelNotification.MODEL_STOPPING, MULE_SYSTEM_MODEL)
                            .serial(new Node(ModelNotification.class, ModelNotification.MODEL_STOPPED, MULE_SYSTEM_MODEL))))
                .serial(new Node(ManagerNotification.class, ManagerNotification.MANAGER_STOPPED_MODELS))
                // what about manager stopping services?
                .serial(new Node(ServiceNotification.class, ServiceNotification.SERVICE_STOPPING, SERVICE))
                .serial(new Node(ServiceNotification.class, ServiceNotification.SERVICE_STOPPED, SERVICE))
                .serial(new Node(ManagerNotification.class, ManagerNotification.MANAGER_STOPPED))
                // disposing is probably in parallel at this point
                .serial(new Node()
                        .parallel(new Node(ManagerNotification.class, ManagerNotification.MANAGER_DISPOSING_CONNECTORS)
                            .serial(new Node(ManagerNotification.class, ManagerNotification.MANAGER_DISPOSED_CONNECTORS))
                            .serial(new Node(ManagerNotification.class, ManagerNotification.MANAGER_DISPOSED)))
                        // no service disposing?
                        .parallel(new Node(ServiceNotification.class, ServiceNotification.SERVICE_DISPOSED, SERVICE))
                        .parallel(new Node(ModelNotification.class, ModelNotification.MODEL_DISPOSING, MODEL)
                            .serial(new Node(ModelNotification.class, ModelNotification.MODEL_DISPOSED, MODEL)))
                        .parallel(new Node(ModelNotification.class, ModelNotification.MODEL_DISPOSING, MULE_SYSTEM_MODEL)
                            .serial(new Node(ModelNotification.class, ModelNotification.MODEL_DISPOSED, MULE_SYSTEM_MODEL))));

        verifyAllNotifications(spec, ModelNotification.class,
                ModelNotification.MODEL_INITIALISING, ModelNotification.MODEL_INITIALISED);
        // no model/listener notifications
        verifyAllNotifications(spec, ModelNotification.class,
                ModelNotification.MODEL_STARTING, ModelNotification.MODEL_DISPOSED);
        verifyAllNotifications(spec, ServiceNotification.class,
                ServiceNotification.SERVICE_INITIALISED, ServiceNotification.SERVICE_STOPPING);
        // no manager initialising or initialised
        verifyAllNotifications(spec, ManagerNotification.class,
                ManagerNotification.MANAGER_STARTING, ManagerNotification.MANAGER_STOPPED_MODELS);

        assertExpectedNotifications(spec);
    }

    protected void logNotifications()
    {
        logger.info("Number of notifications: " + notifications.getNotifications().size());
        for (Iterator iterator = notifications.getNotifications().iterator(); iterator.hasNext();)
        {
            ServerNotification notification = (ServerNotification) iterator.next();
            logger.info(notification);
        }
    }

    /**
     * This is destructive - do not use spec after calling this routine
     */
    protected void assertExpectedNotifications(RestrictedNode spec)
    {
        for (Iterator iterator = notifications.getNotifications().iterator(); iterator.hasNext();)
        {
            ServerNotification notification = (ServerNotification) iterator.next();
            switch (spec.match(notification))
            {
            case Node.SUCCESS:
                break;
            case Node.FAILURE:
                fail("Could not match " + notification);
                break;
            case Node.EMPTY:
                fail("Extra notification: " + notification);
            }
        }
    }

    protected void verifyAllNotifications(RestrictedNode spec, Class clazz, int from, int to)
    {
        for (int action = from; action <= to; ++action)
        {
            if (!spec.contains(clazz, action))
            {
                fail("Specification missed action " + action + " for class " + clazz);
            }
        }
    }

}
