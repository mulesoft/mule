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
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.notification.ComponentMessageNotificationListener;
import org.mule.api.context.notification.ConnectionNotificationListener;
import org.mule.api.context.notification.CustomNotificationListener;
import org.mule.api.context.notification.EndpointMessageNotificationListener;
import org.mule.api.context.notification.ManagementNotificationListener;
import org.mule.api.context.notification.ModelNotificationListener;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.api.context.notification.SecurityNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.ServerNotificationListener;
import org.mule.api.context.notification.ServiceNotificationListener;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.context.notification.NotificationException;
import org.mule.context.notification.MuleContextNotification;
import org.mule.context.notification.ModelNotification;
import org.mule.context.notification.ServiceNotification;
import org.mule.context.notification.SecurityNotification;
import org.mule.context.notification.ManagementNotification;
import org.mule.context.notification.ConnectionNotification;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.context.notification.ComponentMessageNotification;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractNotificationLoggerAgent</code> Receives Mule server notifications
 * and logs them and can optionally route them to an endpoint. This agent will only
 * receive notifications for notification events that are enabled. The notifications
 * that are enabled are determined by the {@link MuleContextBuilder} that is used or
 * configuration mechanisms that may override these values.
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
    private boolean ignoreEndpointMessageNotifications = false;
    private boolean ignoreComponentMessageNotifications = false;

    private Set<ServerNotificationListener> listeners = new HashSet<ServerNotificationListener>();

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
        for (ServerNotificationListener listener : listeners)
        {
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
    
    public boolean isIgnoreComponentMessageNotifications()
    {
        return ignoreComponentMessageNotifications;
    }

    public void setIgnoreComponentMessageNotifications(boolean ignoreComponentMessageNotifications)
    {
        this.ignoreComponentMessageNotifications = ignoreComponentMessageNotifications;
    }

    public boolean isIgnoreEndpointMessageNotifications()
    {
        return ignoreEndpointMessageNotifications;
    }

    public void setIgnoreEndpointMessageNotifications(boolean ignoreEndpointMessageNotifications)
    {
        this.ignoreEndpointMessageNotifications = ignoreEndpointMessageNotifications;
    }

    public final void initialise() throws InitialisationException
    {
        doInitialise();
        if (!ignoreManagerNotifications)
        {
            ServerNotificationListener l = new MuleContextNotificationListener<MuleContextNotification>()
            {
                public void onNotification(MuleContextNotification notification)
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
            ServerNotificationListener l = new ModelNotificationListener<ModelNotification>()
            {
                public void onNotification(ModelNotification notification)
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
            ServerNotificationListener l = new ServiceNotificationListener<ServiceNotification>()
            {
                public void onNotification(ServiceNotification notification)
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
            ServerNotificationListener l = new SecurityNotificationListener<SecurityNotification>()
            {
                public void onNotification(SecurityNotification notification)
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
            ServerNotificationListener l = new ManagementNotificationListener<ManagementNotification>()
            {
                public void onNotification(ManagementNotification notification)
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
            ServerNotificationListener l = new ConnectionNotificationListener<ConnectionNotification>()
            {
                public void onNotification(ConnectionNotification notification)
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

        if (!ignoreMessageNotifications && !ignoreEndpointMessageNotifications)
        {
            ServerNotificationListener l = new EndpointMessageNotificationListener<EndpointMessageNotification>()
            {
                public void onNotification(EndpointMessageNotification notification)
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
        
        if (!ignoreMessageNotifications && !ignoreComponentMessageNotifications)
        {
            ServerNotificationListener l = new ComponentMessageNotificationListener<ComponentMessageNotification>()
            {
                public void onNotification(ComponentMessageNotification notification)
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
