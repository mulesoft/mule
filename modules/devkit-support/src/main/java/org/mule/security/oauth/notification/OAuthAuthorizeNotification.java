/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.notification;

import org.mule.api.MuleEvent;
import org.mule.api.context.notification.BlockingServerEvent;
import org.mule.context.notification.CustomNotification;

public class OAuthAuthorizeNotification extends CustomNotification implements BlockingServerEvent
{

    private static final long serialVersionUID = 1496754927011811649L;

    private static final int OAUTH_ACTION_BASE = CUSTOM_EVENT_ACTION_START_RANGE * 4;

    public static final int OAUTH_AUTHORIZATION_BEGIN = OAUTH_ACTION_BASE + 1;
    public static final int OAUTH_AUTHORIZATION_END = OAUTH_ACTION_BASE + 2;

    static
    {
        registerAction("OAuth authorization dance has been started", OAUTH_AUTHORIZATION_BEGIN);
        registerAction("OAuth authorization callback was received", OAUTH_AUTHORIZATION_END);
    }

    private MuleEvent muleEvent;

    public OAuthAuthorizeNotification(MuleEvent event, int action)
    {
        super(event, action);
        this.muleEvent = event;
    }

    public MuleEvent getMuleEvent()
    {
        return this.muleEvent;
    }

    @Override
    public String toString()
    {
        return EVENT_NAME + "{action = " + getActionName(action) + ", MuleEvent.id = "
               + this.muleEvent.getId() + "}";
    }

}
