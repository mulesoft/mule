/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.events;

import org.mule.tck.functional.EventCallback;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * <code>TestApplicationEventBean</code> is a bean for testing Spring
 * ApplicationListeners with the MuleEventMulticaster
 */

public class TestApplicationEventBean implements ApplicationListener
{
    private final Log logger = LogFactory.getLog(this.getClass());
    private EventCallback eventCallback;

    public void onApplicationEvent(ApplicationEvent event)
    {
        logger.debug("Received Spring event: " + event.getClass().getName());

        if (eventCallback != null)
        {
            try
            {
                eventCallback.eventReceived(null, event);
            }
            catch (Exception e1)
            {
                throw new RuntimeException("Callback failed: " + e1.getMessage(), e1);
            }
        }
    }

    public EventCallback getEventCallback()
    {
        return eventCallback;
    }

    public void setEventCallback(EventCallback eventCallback)
    {
        this.eventCallback = eventCallback;
    }

}
