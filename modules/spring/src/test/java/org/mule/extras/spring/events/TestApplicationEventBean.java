/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring.events;

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
