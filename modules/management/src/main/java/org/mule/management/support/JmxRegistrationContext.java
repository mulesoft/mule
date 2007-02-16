/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.support;

import org.mule.MuleManager;
import org.mule.impl.internal.notifications.ManagerNotification;
import org.mule.impl.internal.notifications.ManagerNotificationListener;
import org.mule.impl.internal.notifications.NotificationException;
import org.mule.umo.manager.UMOServerNotification;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Stores JMX info pertinent to the currently intialising Mule manager with
 * JMX management enabled. The info is being kept for the duration of Mule server life,
 * and cleared on manager disposal.
 * <p/>
 * The main reason for that class is that JmxAgent prepares only the JMX foundation, while
 * the agents following it may require some contextual information about Mule's JMX, such as
 * a currently resolved Mule domain name (if non-clashing JMX domains support is enabled, which
 * is by default). Otherwise, they are left unaware of the previous work, and a random number
 * of JMX domains might be created for Mule.
 */
public class JmxRegistrationContext
{
    /**
     * The logger used for this class
     */
    private final transient Log logger = LogFactory.getLog(getClass());

    /**
     * Normally ThreadLocal is fine, as Mule is being initialised and destroyed
     * by a single thread. We only need to share this info between random agents
     * during startup.
     */
    private static final ThreadLocal contexts = new ThreadLocal();

    private String resolvedDomain;

    /** Do not instantiate JmxRegistrationContext. */
    private JmxRegistrationContext()
    {
        // no manager available, bail out
        if (!MuleManager.isInstanciated())
        {
            return;
        }

        try
        {
            // register the cleanup hook, otherwise server stop/start cycles may produce
            // Mule JMX domains with ever increasing suffix.
            MuleManager.getInstance().registerListener(new ManagerNotificationListener()
            {
                public void onNotification(UMOServerNotification notification)
                {
                    ManagerNotification mn = (ManagerNotification) notification;
                    if (ManagerNotification.MANAGER_DISPOSED == mn.getAction())
                    {
                        // just in case someone is holding a ref to the context instance
                        resolvedDomain = null;
                        // disassociate
                        contexts.set(null);
                    }
                }
            });
        } catch (NotificationException e)
        {
            logger.warn("Did not cleanup properly.", e);
        }
    }

    /**
     * Get current context or create one if none exist for the current startup cycle.
     * @return jmx registration context
     */
    public static JmxRegistrationContext getCurrent()
    {
        JmxRegistrationContext ctx = (JmxRegistrationContext) contexts.get();
        if (ctx == null)
        {
            ctx = new JmxRegistrationContext();
        }
        contexts.set(ctx);
        return ctx;
    }

    /**
     * Getter for property 'resolvedDomain'.
     *
     * @return Value for property 'resolvedDomain'.
     */
    public String getResolvedDomain()
    {
        return resolvedDomain;
    }

    /**
     * Setter for property 'resolvedDomain'.
     *
     * @param resolvedDomain Value to set for property 'resolvedDomain'.
     */
    public void setResolvedDomain(String resolvedDomain)
    {
        this.resolvedDomain = resolvedDomain;
    }
}
