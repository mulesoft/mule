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

import org.mule.module.client.MuleClient;

/**
 * Test ComponentNotifications/Listeners by sending events to a component. A pre and
 * post notification should be received by listeners.
 */
public class ComponentMessageNotificationTestCase extends AbstractNotificationTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/notifications/component-message-notification-test.xml";
    }

    public void doTest() throws Exception
    {
        MuleClient client = new MuleClient();
        assertNotNull(client.send("vm://in-1?connector=direct", "hello sweet world", null));
        client.dispatch("vm://in-2?connector=direct", "goodbye cruel world", null);
        assertNotNull(client.request("vm://out-2?connector=queue", 5000));
    }

    public RestrictedNode getSpecification()
    {
        return new Node().parallel(
            new Node(ComponentMessageNotification.class, ComponentMessageNotification.COMPONENT_PRE_INVOKE))
            .parallel(
                new Node(ComponentMessageNotification.class,
                    ComponentMessageNotification.COMPONENT_POST_INVOKE))
            .parallel(
                new Node(ComponentMessageNotification.class,
                    ComponentMessageNotification.COMPONENT_PRE_INVOKE))
            .parallel(
                new Node(ComponentMessageNotification.class,
                    ComponentMessageNotification.COMPONENT_POST_INVOKE));
    }

    public void validateSpecification(RestrictedNode spec) throws Exception
    {
        verifyAllNotifications(spec, ComponentMessageNotification.class,
            ComponentMessageNotification.COMPONENT_PRE_INVOKE,
            ComponentMessageNotification.COMPONENT_POST_INVOKE);
    }
}
