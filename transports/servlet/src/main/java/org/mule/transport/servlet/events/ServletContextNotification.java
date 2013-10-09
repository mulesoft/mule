/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
