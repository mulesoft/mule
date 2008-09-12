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

import org.mule.api.service.Service;
import org.mule.module.client.MuleClient;

public class ServerNotificationManagerTestCase extends AbstractNotificationTestCase
{

    public static final String MULE_SYSTEM_MODEL = "_muleSystemModel";
    public static final String MODEL = "the-model";
    public static final String SERVICE = "the-service";

    protected String getConfigResources()
    {
        return "org/mule/test/integration/notifications/server-notification-manager-test.xml";
    }

    public void doTest() throws Exception
    {
        MuleClient client = new MuleClient();
        assertNotNull(client.send("vm://in", "hello world", null));
        Service service = muleContext.getRegistry().lookupService(SERVICE);
        service.pause();
        service.resume();
    }

    public RestrictedNode getSpecification()
    {
        return new Node()
                // all service events are asynchronous, so place parallel to rest
                .parallel(new Node(ServiceNotification.class, ServiceNotification.SERVICE_INITIALISED, SERVICE))
                .parallel(new Node(ServiceNotification.class, ServiceNotification.SERVICE_STARTED, SERVICE))
                .parallel(new Node(ServiceNotification.class, ServiceNotification.SERVICE_PAUSED, SERVICE))
                .parallel(new Node(ServiceNotification.class, ServiceNotification.SERVICE_RESUMED, SERVICE))
                .parallel(new Node(ServiceNotification.class, ServiceNotification.SERVICE_STOPPING, SERVICE))
                .parallel(new Node(ServiceNotification.class, ServiceNotification.SERVICE_STOPPED, SERVICE))
                .parallel(new Node(ServiceNotification.class, ServiceNotification.SERVICE_DISPOSED, SERVICE))
                // synchronous events start here
                .parallel(new Node()
                        // parallel because we don't know which model
                        .parallelSynch(new Node(ModelNotification.class, ModelNotification.MODEL_INITIALISING, MULE_SYSTEM_MODEL)
                                .serial(new Node(ModelNotification.class, ModelNotification.MODEL_INITIALISED, MULE_SYSTEM_MODEL)))
                        .parallelSynch(new Node(ModelNotification.class, ModelNotification.MODEL_INITIALISING, MODEL)
                                .serial(new Node(ModelNotification.class, ModelNotification.MODEL_INITIALISED, MODEL)))
                        .serial(new Node(MuleContextNotification.class, MuleContextNotification.CONTEXT_STARTING))
                        .serial(new Node(MuleContextNotification.class, MuleContextNotification.CONTEXT_STARTING_MODELS)
                                // parallel because we don't know which model
                                .parallelSynch(new Node(ModelNotification.class, ModelNotification.MODEL_STARTING, MODEL)
                                        .serial(new Node(ModelNotification.class, ModelNotification.MODEL_STARTED, MODEL)))
                                .parallelSynch(new Node(ModelNotification.class, ModelNotification.MODEL_STARTING, MULE_SYSTEM_MODEL)
                                        .serial(new Node(ModelNotification.class, ModelNotification.MODEL_STARTED, MULE_SYSTEM_MODEL))))
                        .serial(new Node(MuleContextNotification.class, MuleContextNotification.CONTEXT_STARTED_MODELS))
                        .serial(new Node(MuleContextNotification.class, MuleContextNotification.CONTEXT_STARTED))
                        .serial(new Node(MuleContextNotification.class, MuleContextNotification.CONTEXT_DISPOSING))
                        .serial(new Node(MuleContextNotification.class, MuleContextNotification.CONTEXT_STOPPING))
                        .serial(new Node(MuleContextNotification.class, MuleContextNotification.CONTEXT_STOPPING_MODELS)
                                // parallel because we don't know which model
                                .parallelSynch(new Node(ModelNotification.class, ModelNotification.MODEL_STOPPING, MODEL)
                                        .serial(new Node(ModelNotification.class, ModelNotification.MODEL_STOPPED, MODEL)))
                                .parallelSynch(new Node(ModelNotification.class, ModelNotification.MODEL_STOPPING, MULE_SYSTEM_MODEL)
                                        .serial(new Node(ModelNotification.class, ModelNotification.MODEL_STOPPED, MULE_SYSTEM_MODEL))))
                        .serial(new Node(MuleContextNotification.class, MuleContextNotification.CONTEXT_STOPPED_MODELS))
                        .serial(new Node(MuleContextNotification.class, MuleContextNotification.CONTEXT_STOPPED)
                                // parallel because we don't know which model
                                .parallelSynch(new Node(ModelNotification.class, ModelNotification.MODEL_DISPOSING, MODEL)
                                        .serial(new Node(ModelNotification.class, ModelNotification.MODEL_DISPOSED, MODEL)))
                                .parallelSynch(new Node(ModelNotification.class, ModelNotification.MODEL_DISPOSING, MULE_SYSTEM_MODEL)
                                        .serial(new Node(ModelNotification.class, ModelNotification.MODEL_DISPOSED, MULE_SYSTEM_MODEL))))
                          .serial(new Node(MuleContextNotification.class, MuleContextNotification.CONTEXT_DISPOSED)));
    }

    public void validateSpecification(RestrictedNode spec) throws Exception
    {
        verifyAllNotifications(spec, ModelNotification.class,
                ModelNotification.MODEL_INITIALISING, ModelNotification.MODEL_INITIALISED);
        // no model/listener notifications
        verifyAllNotifications(spec, ModelNotification.class,
                ModelNotification.MODEL_STARTING, ModelNotification.MODEL_DISPOSED);
        verifyAllNotifications(spec, ServiceNotification.class,
                ServiceNotification.SERVICE_INITIALISED, ServiceNotification.SERVICE_STOPPING);
        // no manager initialising or initialised
        verifyAllNotifications(spec, MuleContextNotification.class,
                MuleContextNotification.CONTEXT_STARTING, MuleContextNotification.CONTEXT_STOPPED_MODELS);
    }

}
