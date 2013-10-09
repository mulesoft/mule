/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.ServerNotification;
import org.mule.api.security.SecurityException;

/**
 * <code>SecurityNotification</code> is fired when a request for authorisation failed.
 */
public class SecurityNotification extends ServerNotification
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 5951835321289699941L;

    public static final int SECURITY_AUTHENTICATION_FAILED = SECURITY_EVENT_ACTION_START_RANGE + 1;

    static
    {
        registerAction("authentication failed", SECURITY_AUTHENTICATION_FAILED);
    }

    public SecurityNotification(SecurityException message, int action)
    {
        super(message.getDetailedMessage(), action);
        resourceIdentifier = message.getClass().getName();
    }

    @Override
    public String getType()
    {
        return TYPE_WARNING;
    }
}
