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

public class MessageNotificationTestCase extends AbstractNotificationTestCase
{

    public static final String SERVICE_1 = "service-1";
    public static final String SERVICE_2 = "service-2";
    public static final String CLIENT = null; // client id is null

    protected String getConfigResources()
    {
        return "org/mule/test/integration/notifications/message-notification-test.xml";
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
        return new Node()
                .parallel(new Node(MessageNotification.class, MessageNotification.MESSAGE_SENT, CLIENT))
                .parallel(new Node(MessageNotification.class, MessageNotification.MESSAGE_RECEIVED, SERVICE_1))
                .parallel(new Node(MessageNotification.class, MessageNotification.MESSAGE_DISPATCHED, CLIENT))
                .parallel(new Node(MessageNotification.class, MessageNotification.MESSAGE_RECEIVED, SERVICE_2))
                .parallel(new Node(MessageNotification.class, MessageNotification.MESSAGE_REQUESTED, CLIENT))
                .parallel(new Node(MessageNotification.class, MessageNotification.MESSAGE_DISPATCHED, SERVICE_2))
                .parallel(new Node(MessageNotification.class, MessageNotification.MESSAGE_RECEIVED, CLIENT));
    }

    public void validateSpecification(RestrictedNode spec) throws Exception
    {
        verifyAllNotifications(spec, MessageNotification.class,
                MessageNotification.MESSAGE_RECEIVED, MessageNotification.MESSAGE_REQUESTED);
    }

}