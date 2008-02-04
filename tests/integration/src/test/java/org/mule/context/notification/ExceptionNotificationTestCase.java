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

public class ExceptionNotificationTestCase extends AbstractNotificationTestCase
{

    public static final String SERVICE_1 = "the-service";

    protected String getConfigResources()
    {
        return "org/mule/test/integration/notifications/exception-notification-test.xml";
    }

    public void doTest() throws Exception
    {
        MuleClient client = new MuleClient();
        assertNotNull(client.send("vm://in-1", "hello world", null));
    }

    public RestrictedNode getSpecification()
    {
        return new Node(ExceptionNotification.class, ExceptionNotification.EXCEPTION_ACTION);
    }

    public void validateSpecification(RestrictedNode spec) throws Exception
    {
        verifyAllNotifications(spec, ExceptionNotification.class,
                ExceptionNotification.EXCEPTION_ACTION, ExceptionNotification.EXCEPTION_ACTION);
    }

}