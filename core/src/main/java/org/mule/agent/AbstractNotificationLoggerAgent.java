/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.agent;

import org.mule.AbstractAgent;
import org.mule.api.MuleException;
import org.mule.api.context.notification.AdminNotificationListener;
import org.mule.api.context.notification.ServiceNotificationListener;
import org.mule.api.context.notification.ConnectionNotificationListener;
import org.mule.api.context.notification.CustomNotificationListener;
import org.mule.api.context.notification.ManagementNotificationListener;
import org.mule.api.context.notification.ManagerNotificationListener;
import org.mule.api.context.notification.MessageNotificationListener;
import org.mule.api.context.notification.ModelNotificationListener;
import org.mule.api.context.notification.SecurityNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.ServerNotificationListener;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.context.notification.NotificationException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractNotificationLoggerAgent</code> Receives Mule server notifications
 * and logs them and can optionally route them to an endpoint
 */
public abstract class AbstractNotificationLoggerAgent extends AbstractAgent
{
    /**
     * The logger used for this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private boolean ignoreManagerNotifications = false;
    private boolean ignoreModelNotifications = false;
    private boolean ignoreComponentNotifications = false;
    private boolean ignoreConnectionNotifications = false;
    private boolean ignoreSecurityNotifications = false;
    private boolean ignoreManagementNotifications = false;
    private boolean ignoreCustomNotifications = false;
    private boolean ignoreAdminNotifications = false;
    private boolean ignoreMessageNotifications = false;

    private Set listeners = new HashSet();


    protected AbstractNotificationLoggerAgent(String name)
    {
        super(name);
    }

    public void start() throws MuleException
    {
        // nothing to do
    }

    public void stop() throws MuleException
    {
        // nothing to do
    }

    public void dispose()
    {
        // nothing to do
    }

    public void registered()
    {
        // nothing to do
    }

    public void unregistered()
    {
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();)
        {
            ServerNotificationListener listener = (ServerNotificationListener) iterator.next();
            muleContext.unregisterListener(listener);
        }
    }

    public boolean isIgnoreManagerNotifications()
    {
        return ignoreManagerNotifications;
    }

    public void setIgnoreManagerNotifications(boolean ignoreManagerNotifications)
    {
        this.ignoreManagerNotifications = ignoreManagerNotifications;
    }

    public boolean isIgnoreMessageNotifications()
    {
        return ignoreMessageNotifications;
    }

    public void setIgnoreMessageNotifications(boolean ignoreMessageNotifications)
    {
        this.ignoreMessageNotifications = ignoreMessageNotifications;
    }

    public boolean isIgnoreModelNotifications()
    {
        return ignoreModelNotifications;
    }

    public void setIgnoreModelNotifications(boolean ignoreModelNotifications)
    {
        this.ignoreModelNotifications = ignoreModelNotifications;
    }

    public boolean isIgnoreComponentNotifications()
    {
        return ignoreComponentNotifications;
    }

    public void setIgnoreComponentNotifications(boolean ignoreComponentNotifications)
    {
        this.ignoreComponentNotifications = ignoreComponentNotifications;
    }

    public boolean isIgnoreSecurityNotifications()
    {
        return ignoreSecurityNotifications;
    }

    public void setIgnoreSecurityNotifications(boolean ignoreSecurityNotifications)
    {
        this.ignoreSecurityNotifications = ignoreSecurityNotifications;
    }

    public boolean isIgnoreManagementNotifications()
    {
        return ignoreManagementNotifications;
    }

    public void setIgnoreManagementNotifications(boolean ignoreManagementNotifications)
    {
        this.ignoreManagementNotifications = ignoreManagementNotifications;
    }

    public boolean isIgnoreCustomNotifications()
    {
        return ignoreCustomNotifications;
    }

    public void setIgnoreCustomNotifications(boolean ignoreCustomNotifications)
    {
        this.ignoreCustomNotifications = ignoreCustomNotifications;
    }

    public boolean isIgnoreAdminNotifications()
    {
        return ignoreAdminNotifications;
    }

    public void setIgnoreAdminNotifications(boolean ignoreAdminNotifications)
    {
        this.ignoreAdminNotifications = ignoreAdminNotifications;
    }

    public boolean isIgnoreConnectionNotifications()
    {
        return ignoreConnectionNotifications;
    }

    public void setIgnoreConnectionNotifications(boolean ignoreConnectionNotifications)
    {
        this.ignoreConnectionNotifications = ignoreConnectionNotifications;
    }

    public final void initialise() throws InitialisationException    
    {
        doInitialise();
        if (!ignoreManagerNotifications)
        {
            ServerNotificationListener l = new ManagerNotificationListener()
            {
                public void onNotification(ServerNotification notification)
                {
                    logEvent(notification);
                }
            };
            try
            {
               muleContext.registerListener(l);
            }
            catch (NotificationException e)
            {
                throw new InitialisationException(e, this);
            }
            listeners.add(l);
        }
        if (!ignoreModelNotifications)
        {
            ServerNotificationListener l = new ModelNotificationListener()
            {
                public void onNotification(ServerNotification notification)
                {
                    logEvent(notification);
                }
            };
            try
            {
               muleContext.registerListener(l);
            }
            catch (NotificationException e)
            {
                throw new InitialisationException(e, this);
            }
            listeners.add(l);
        }
        if (!ignoreComponentNotifications)
        {
            ServerNotificationListener l = new ServiceNotificationListener()
            {
                public void onNotification(ServerNotification notification)
                {
                    logEvent(notification);
                }
            };
            try
            {
               muleContext.registerListener(l);
            }
            catch (NotificationException e)
            {
                throw new InitialisationException(e, this);
            }
            listeners.add(l);
        }
        if (!ignoreSecurityNotifications)
        {
            ServerNotificationListener l = new SecurityNotificationListener()
            {
                public void onNotification(ServerNotification notification)
                {
                    logEvent(notification);
                }
            };
            try
            {
               muleContext.registerListener(l);
            }
            catch (NotificationException e)
            {
                throw new InitialisationException(e, this);
            }
            listeners.add(l);
        }

        if (!ignoreManagementNotifications)
        {
            ServerNotificationListener l = new ManagementNotificationListener()
            {
                public void onNotification(ServerNotification notification)
                {
                    logEvent(notification);
                }
            };
            try
            {
               muleContext.registerListener(l);
            }
            catch (NotificationException e)
            {
                throw new InitialisationException(e, this);
            }
            listeners.add(l);
        }

        if (!ignoreCustomNotifications)
        {
            ServerNotificationListener l = new CustomNotificationListener()
            {
                public void onNotification(ServerNotification notification)
                {
                    logEvent(notification);
                }
            };
            try
            {
               muleContext.registerListener(l);
            }
            catch (NotificationException e)
            {
                throw new InitialisationException(e, this);
            }
            listeners.add(l);
        }

        if (!ignoreConnectionNotifications)
        {
            ServerNotificationListener l = new ConnectionNotificationListener()
            {
                public void onNotification(ServerNotification notification)
                {
                    logEvent(notification);
                }
            };
            try
            {
               muleContext.registerListener(l);
            }
            catch (NotificationException e)
            {
                throw new InitialisationException(e, this);
            }
            listeners.add(l);
        }

        if (!ignoreAdminNotifications)
        {
            ServerNotificationListener l = new AdminNotificationListener()
            {
                public void onNotification(ServerNotification notification)
                {
                    logEvent(notification);
                }
            };
            try
            {
               muleContext.registerListener(l);
            }
            catch (NotificationException e)
            {
                throw new InitialisationException(e, this);
            }
            listeners.add(l);
        }

        if (!ignoreMessageNotifications /** &&  TODO RM* !RegistryContext.getConfiguration().isEnableMessageEvents() **/)
        {
            logger.warn("EventLogger agent has been asked to log message notifications, but the MuleManager is configured not to fire Message notifications");
        }
        else if (!ignoreMessageNotifications)
        {
            ServerNotificationListener l = new MessageNotificationListener()
            {
                public void onNotification(ServerNotification notification)
                {
                    logEvent(notification);
                }
            };
            try
            {
               muleContext.registerListener(l);
            }
            catch (NotificationException e)
            {
                throw new InitialisationException(e, this);
            }
            listeners.add(l);
        }

    }

    protected abstract void doInitialise() throws InitialisationException;

    protected abstract void logEvent(ServerNotification e);
}
