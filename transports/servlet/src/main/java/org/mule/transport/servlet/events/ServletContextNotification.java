/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.events;

import org.mule.api.context.notification.BlockingServerEvent;
import org.mule.context.notification.CustomNotification;

import javax.servlet.ServletContext;

/**
 * TODO
 */
public class ServletContextNotification extends CustomNotification implements BlockingServerEvent
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3246036188011581121L;

    public static final int SERVLET_CONTEXT_START_RANGE = CUSTOM_EVENT_ACTION_START_RANGE + 1200;
    public static final int SERVLET_CONTEXT_INITIALISED = SERVLET_CONTEXT_START_RANGE + 1;
    public static final int SERVLET_CONTEXT_DESTROYED = SERVLET_CONTEXT_START_RANGE + 2;

    static {
        registerAction("servlet context initialising", SERVLET_CONTEXT_INITIALISED);
        registerAction("servlet context destroyed", SERVLET_CONTEXT_DESTROYED);
    }

    public ServletContextNotification(ServletContext context, int action)
    {
        super(context, action, context.getServletContextName());
    }

    public String toString()
    {
        return EVENT_NAME + "{" + "action=" + getActionName(action) + ", resourceId=" + resourceIdentifier
                + ", timestamp=" + timestamp + "}";
    }

}
