/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.agent;

import org.mule.agent.AbstractNotificationLoggerAgent;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.management.support.AutoDiscoveryJmxSupportFactory;
import org.mule.module.management.support.JmxSupport;
import org.mule.module.management.support.JmxSupportFactory;

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.ObjectName;

/**
 * An agent that propergates Mule Server notifications to Jmx.
 *
 */
public class JmxServerNotificationAgent extends AbstractNotificationLoggerAgent
{

    public static final String LISTENER_JMX_OBJECT_NAME = "type=org.mule.Notification,name=MuleNotificationListener";
    public static final String BROADCASTER_JMX_OBJECT_NAME = "type=org.mule.Notification,name=MuleNotificationBroadcaster";
    public static final String DEFAULT_AGENT_NAME = "Jmx Notification Agent";

    private MBeanServer mBeanServer;
    private BroadcastNotificationService broadcastNotificationMbean;
    private boolean registerListenerMbean = true;
    private ObjectName listenerObjectName;
    private ObjectName broadcasterObjectName;

    private JmxSupportFactory jmxSupportFactory = AutoDiscoveryJmxSupportFactory.getInstance();
    private JmxSupport jmxSupport;


    public JmxServerNotificationAgent()
    {
        super(DEFAULT_AGENT_NAME);
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        try
        {
            jmxSupport = jmxSupportFactory.getJmxSupport();
            mBeanServer = MBeanServerFactory.findMBeanServer(null).get(0);
            broadcasterObjectName = ObjectName.getInstance(jmxSupport.getDomainName(muleContext) + ":" + BROADCASTER_JMX_OBJECT_NAME);
            broadcastNotificationMbean = new BroadcastNotificationService();
            mBeanServer.registerMBean(broadcastNotificationMbean, broadcasterObjectName);
            if (registerListenerMbean)
            {
                listenerObjectName = ObjectName.getInstance(jmxSupport.getDomainName(muleContext) + ":" + LISTENER_JMX_OBJECT_NAME);
                NotificationListener mbean = new NotificationListener();
                broadcastNotificationMbean.addNotificationListener(mbean, null, null);
                mBeanServer.registerMBean(mbean, listenerObjectName);
            }
        }
        catch (Exception e)
        {
            throw new InitialisationException(CoreMessages.failedToStart("JMX Server Notification Agent"), e, this);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose()
    {
        if (listenerObjectName != null && mBeanServer.isRegistered(listenerObjectName))
        {
            try
            {
                mBeanServer.unregisterMBean(listenerObjectName);
            }
            catch (Exception e)
            {
                logger.warn(e.getMessage(), e);
            }
        }

        if (broadcasterObjectName != null && mBeanServer.isRegistered(broadcasterObjectName))
        {
            try
            {
                mBeanServer.unregisterMBean(broadcasterObjectName);
            }
            catch (Exception e)
            {
                logger.warn(e.getMessage(), e);
            }
        }        super.dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void logEvent(ServerNotification e)
    {
        broadcastNotificationMbean.sendNotification(new Notification(e.getClass().getName(), e, e.getTimestamp(), e.toString()));
    }

    /**
     * Should be a 1 line description of the agent.
     *
     * @return description
     */
    @Override
    public String getDescription()
    {
        return DEFAULT_AGENT_NAME + (registerListenerMbean ? " (Listener MBean registered)" : "");
    }


    /**
     * Getter for property 'jmxSupportFactory'.
     *
     * @return Value for property 'jmxSupportFactory'.
     */
    public JmxSupportFactory getJmxSupportFactory()
    {
        return jmxSupportFactory;
    }

    /**
     * Setter for property 'jmxSupportFactory'.
     *
     * @param jmxSupportFactory Value to set for property 'jmxSupportFactory'.
     */
    public void setJmxSupportFactory(JmxSupportFactory jmxSupportFactory)
    {
        this.jmxSupportFactory = jmxSupportFactory;
    }

    public static interface BroadcastNotificationServiceMBean extends NotificationEmitter
    {
        // no methods
    }

    public static class BroadcastNotificationService extends NotificationBroadcasterSupport implements BroadcastNotificationServiceMBean
    {
        // no methods
    }

    public static interface NotificationListenerMBean
    {
        /**
         * Getter for property 'notificsationList'.
         *
         * @return Value for property 'notificsationList'.
         */
        List getNotificationsList();

        /**
         * Getter for property 'listSize'.
         *
         * @return Value for property 'listSize'.
         */
        int getListSize();

        /**
         * Setter for property 'listSize'.
         *
         * @param listSize Value to set for property 'listSize'.
         */
        void setListSize(int listSize);
    }

    public static class NotificationListener implements NotificationListenerMBean, javax.management.NotificationListener
    {
        private int listSize = 100;

        private List notifs;

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleNotification(Notification notification, Object o)
        {
            if (getList().size() == listSize)
            {
                getList().remove(listSize - 1);
            }
            getList().add(0, notification);
        }

        /**
         * {@inheritDoc}
         */
        public List getNotificationsList()
        {
            return notifs;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getListSize()
        {
            return listSize;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setListSize(int listSize)
        {
            this.listSize = listSize;
        }

        /**
         * Getter for property 'list'.
         *
         * @return Value for property 'list'.
         */
        protected List getList()
        {
            if (notifs == null)
            {
                notifs = new ArrayList(listSize);
            }
            return notifs;
        }

    }

}
