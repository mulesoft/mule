/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.support;

import org.mule.api.MuleContext;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.context.notification.MuleContextNotification;
import org.mule.context.notification.NotificationException;

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
    private static final ThreadLocal<JmxRegistrationContext> contexts = new ThreadLocal<JmxRegistrationContext>();

    private String resolvedDomain;

    /** Do not instantiate JmxRegistrationContext. */
    private JmxRegistrationContext(MuleContext context)
    {
        try
        {
            // register the cleanup hook, otherwise server stop/start cycles may produce
            // Mule JMX domains with ever increasing suffix.
            context.registerListener(new MuleContextNotificationListener<MuleContextNotification>()
            {
                @Override
                public void onNotification(MuleContextNotification notification)
                {
                    MuleContextNotification mn = notification;
                    if (MuleContextNotification.CONTEXT_DISPOSED == mn.getAction())
                    {
                        // just in case someone is holding a ref to the context instance
                        resolvedDomain = null;
                        // disassociate
                        contexts.set(null);
                    }
                }
            });
        } 
        catch (NotificationException e)
        {
            logger.warn("Did not cleanup properly.", e);
        }
    }

    /**
     * Get current context or create one if none exist for the current startup cycle.
     * @return jmx registration context
     */
    public static JmxRegistrationContext getCurrent(MuleContext context)
    {
        JmxRegistrationContext ctx = contexts.get();
        if (ctx == null)
        {
            ctx = new JmxRegistrationContext(context);
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
