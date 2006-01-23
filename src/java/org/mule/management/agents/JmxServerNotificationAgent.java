/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.management.agents;

import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.internal.admin.AbstractNotificationLoggerAgent;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOServerNotification;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.List;

/**
 * An agent that propergates Mule Server notifications to Jmx.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JmxServerNotificationAgent extends AbstractNotificationLoggerAgent {

    public static final String LISTENER_JMX_OBJECT_NAME = "type=org.mule.Notification,name=MuleNotificationListener";
    public static final String BROADCASTER_JMX_OBJECT_NAME = "type=org.mule.Notification,name=MuleNotificationBroadcaster";

    private MBeanServer mBeanServer;
    private BroadcastNotificationService broadcastNotificationMbean = null;
    private boolean useInstanceIdAsDomain = true;
    private boolean registerListenerMbean = true;
    private ObjectName listenerObjectName = null;
    private ObjectName broadcasterObjectName = null;


    protected void doInitialise() throws InitialisationException {
        if (getName() == null) {
            setName("Jmx Notification Agent");
        }
        try {
            mBeanServer = (MBeanServer) MBeanServerFactory.findMBeanServer(null).get(0);
            broadcasterObjectName = ObjectName.getInstance(getDomainName() + ":" + BROADCASTER_JMX_OBJECT_NAME);
             broadcastNotificationMbean = new BroadcastNotificationService();
            mBeanServer.registerMBean(broadcastNotificationMbean, broadcasterObjectName);
            if(registerListenerMbean) {
                listenerObjectName = ObjectName.getInstance(getDomainName() + ":" + LISTENER_JMX_OBJECT_NAME);
                NotificationListener mbean = new NotificationListener();
                broadcastNotificationMbean.addNotificationListener(mbean, null, null);
                mBeanServer.registerMBean(mbean, listenerObjectName);
            }
        } catch (Exception e) {
            throw new InitialisationException(new Message(Messages.FAILED_TO_START_X, "JMX Server Notification Agent"), e, this);
        }
    }


    public void dispose() {
        try {
           if(listenerObjectName!=null) mBeanServer.unregisterMBean(listenerObjectName);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            mBeanServer.unregisterMBean(broadcasterObjectName);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        super.dispose();
    }

    protected void logEvent(UMOServerNotification e) {
        broadcastNotificationMbean.sendNotification(new Notification(e.getClass().getName(), e, e.getTimestamp(), e.toString()));
    }

    /**
     * Should be a 1 line description of the agent
     *
     * @return
     */
    public String getDescription() {
        return "Jmx Notification Agent" + (registerListenerMbean ? "(Listener MBean registered)" : "");
    }

    protected String getDomainName() {
        if (MuleManager.getInstance().getId() != null && isUseInstanceIdAsDomain()) {
            return MuleManager.getInstance().getId();
        } else {
            return "org.mule";
        }
    }

    public boolean isUseInstanceIdAsDomain() {
        return useInstanceIdAsDomain;
    }

    public void setUseInstanceIdAsDomain(boolean useInstanceIdAsDomain) {
        this.useInstanceIdAsDomain = useInstanceIdAsDomain;
    }

    public static interface BroadcastNotificationServiceMBean extends NotificationEmitter {

    }

    public static class BroadcastNotificationService extends NotificationBroadcasterSupport implements BroadcastNotificationServiceMBean {

    }

    public static interface NotificationListenerMBean {
        List getNotificsationList();

        int getListSize();

        void setListSize(int listSize);
    }

    public static class NotificationListener implements NotificationListenerMBean, javax.management.NotificationListener {
        private int listSize = 100;

        private List notifs;

        public void handleNotification(Notification notification, Object o) {
            if (getList().size() == listSize) {
                getList().remove(listSize - 1);
            }
            getList().add(0, notification);
        }

        public List getNotificsationList() {
            return notifs;
        }

        public int getListSize() {
            return listSize;
        }

        public void setListSize(int listSize) {
            this.listSize = listSize;
        }

        protected List getList() {
            if (notifs == null) {
                notifs = new ArrayList(listSize);
            }
            return notifs;
        }

    }

}
