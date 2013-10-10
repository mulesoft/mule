/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.ServerNotification;

public class Event2 extends ServerNotification
{

    public Event2()
    {
        super("", 0);
    }

    public Event2(String id)
    {
        super("", 0, id);
    }

}
