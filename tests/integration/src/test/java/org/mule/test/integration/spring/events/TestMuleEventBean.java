/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.spring.events;

import org.mule.extras.spring.events.MuleApplicationEvent;
import org.mule.extras.spring.events.MuleEventListener;
import org.mule.tck.functional.EventCallback;
import org.mule.util.StringMessageUtils;
import org.springframework.context.ApplicationEvent;

/**
 * <code>TestMuleEventBean</code> is a MuleEventBean for testing with the
 * MuleEventMulticaster.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class TestMuleEventBean implements MuleEventListener {
    private EventCallback eventCallback;

    public void onApplicationEvent(ApplicationEvent event) {
        MuleApplicationEvent e = (MuleApplicationEvent) event;

        System.out.println(StringMessageUtils.getBoilerPlate("Received message on " + e.getEndpoint()));

        if (eventCallback != null) {
            try {
                eventCallback.eventReceived(e.getMuleEventContext(), event);
            } catch (Exception e1) {
                throw new RuntimeException("Callback failed: " + e1.getMessage(), e1);
            }
        }
    }

    public EventCallback getEventCallback() {
        return eventCallback;
    }

    public void setEventCallback(EventCallback eventCallback) {
        this.eventCallback = eventCallback;
    }
}
