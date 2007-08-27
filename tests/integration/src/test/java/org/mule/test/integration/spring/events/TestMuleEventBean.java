/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.spring.events;

import org.mule.extras.spring.events.MuleApplicationEvent;
import org.mule.extras.spring.events.MuleEventListener;
import org.mule.tck.functional.EventCallback;
import org.mule.util.StringMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEvent;

/**
 * <code>TestMuleEventBean</code> is a MuleEventBean for testing with the
 * MuleEventMulticaster.
 */

public class TestMuleEventBean implements MuleEventListener
{
    private static final Log logger = LogFactory.getLog(TestMuleEventBean.class);

    private EventCallback eventCallback;

    public void onApplicationEvent(ApplicationEvent event)
    {
        MuleApplicationEvent e = (MuleApplicationEvent)event;

        logger.debug(StringMessageUtils.getBoilerPlate("Received message on " + e.getEndpoint()));

        if (eventCallback != null)
        {
            try
            {
                eventCallback.eventReceived(e.getMuleEventContext(), event);
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
