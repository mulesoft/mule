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

import org.mule.api.context.notification.ServerNotification;

/**
 * <code>SecurityNotification</code> is fired when a request for authorisation
 * occurs. The event may denote successful access or denied access depending on the
 * type of event. Subscribing to these notifications developers can maintain an
 * access log, block clients, etc.
 * 
 * @see org.mule.api.MuleContext
 */
public class SecurityNotification extends ServerNotification
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 5951835321289699941L;

    public static final int SECURITY_AUTHENTICATION_FAILED = SECURITY_EVENT_ACTION_START_RANGE + 1;

    static {
        registerAction("authenitication failed", SECURITY_AUTHENTICATION_FAILED);
    }

    public SecurityNotification(org.mule.api.security.SecurityException message, int action)
    {
        super(message, action);
        resourceIdentifier = message.toString();
    }

    protected String getPayloadToString()
    {
        return source.toString();
    }

    public String getType()
    {
        return TYPE_WARNING;
    }
}
