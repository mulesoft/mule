/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.agents;

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.ObjectName;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.internal.admin.AbstractNotificationLoggerAgent;
import org.mule.management.support.AutoDiscoveryJmxSupportFactory;
import org.mule.management.support.JmxSupport;
import org.mule.management.support.JmxSupportFactory;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOServerNotification;

/**
 * An agent that propergates Mule Server notifications to Jmx.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JmxServerNotificationAgent extends AbstractNotificationLoggerAgent
{

    public static final String LISTENER_JMX_OBJECT_NAME = "type=org.mule.Notification,name=MuleNotificationListener";
    public static final String BROADCASTER_JMX_OBJECT_NAME = "type=org.mule.Notification,name=MuleNotificationBroadcaster";

    private MBeanServer mBeanServer;
    private BroadcastNotificationService broadcastNotificationMbean;
    private boolean registerListenerMbean = true;
    private ObjectName listenerObjectName;
    private ObjectName broadcasterObjectName;

    private JmxSupportFactory jmxSupportFactory = new AutoDiscoveryJmxSupportFactory();
    private JmxSupport jmxSupport;


    /**
     * {@inheritDoc}
     */
    protected void doInitialise() throws InitialisationException
    {
        if (getName() == null)
        {
            setName("Jmx Notification Agent");
        }
        try
        {
            jmxSupport = jmxSupportFactory.newJmxSupport();
            mBeanServer = (MBeanServer) MBeanServerFactory.findMBeanServer(null).get(0);
            broadcasterObjectName = ObjectName.getInstance(jmxSupport.getDomainName() + ":" + BROADCASTER_JMX_OBJECT_NAME);
            broadcastNotificationMbean = new BroadcastNotificationService();
            mBeanServer.registerMBean(broadcastNotificationMbean, broadcasterObjectName);
            if (registerListenerMbean)
            {
                listenerObjectName = ObjectName.getInstance(jmxSupport.getDomainName() + ":" + LISTENER_JMX_OBJECT_NAME);
                NotificationListener mbean = new NotificationListener();
                broadcastNotificationMbean.addNotificationListener(mbean, null, null);
                mBeanServer.registerMBean(mbean, listenerObjectName);
            }
        } catch (Exception e)
        {
            throw new InitialisationException(new Message(Messages.FAILED_TO_START_X, "JMX Server Notification Agent"), e, this);
        }
    }


    /**
     * {@inheritDoc}
     */
    public void dispose()
    {
        try
        {
            if (listenerObjectName != null)
            {
                mBeanServer.unregisterMBean(listenerObjectName);
            }
        } catch (Exception e)
        {
            logger.warn(e.getMessage(), e);
        }
        try
        {
            mBeanServer.unregisterMBean(broadcasterObjectName);
        } catch (Exception e)
        {
            logger.warn(e.getMessage(), e);
        }
        super.dispose();
    }

    /**
     * {@inheritDoc}
     */
    protected void logEvent(UMOServerNotification e)
    {
        broadcastNotificationMbean.sendNotification(new Notification(e.getClass().getName(), e, e.getTimestamp(), e.toString()));
    }

    /**
     * Should be a 1 line description of the agent.
     *
     * @return description
     */
    public String getDescription()
    {
        return "Jmx Notification Agent" + (registerListenerMbean ? "(Listener MBean registered)" : "");
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
        public int getListSize()
        {
            return listSize;
        }

        /**
         * {@inheritDoc}
         */
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
