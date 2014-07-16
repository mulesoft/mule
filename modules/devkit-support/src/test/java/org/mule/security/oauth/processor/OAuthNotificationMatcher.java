/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.processor;

import org.mule.api.MuleEvent;
import org.mule.api.context.notification.ServerNotification;
import org.mule.security.oauth.notification.OAuthAuthorizeNotification;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class OAuthNotificationMatcher extends BaseMatcher<ServerNotification>
{

    private int action;
    private MuleEvent event;

    public OAuthNotificationMatcher(int action, MuleEvent event)
    {
        this.action = action;
        this.event = event;
    }

    @Override
    public boolean matches(Object item)
    {
        if (item instanceof OAuthAuthorizeNotification)
        {
            OAuthAuthorizeNotification notification = (OAuthAuthorizeNotification) item;
            return notification.getMuleEvent() == this.event && notification.getAction() == this.action;
        }
        return false;
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText("test matcher");
    }
}
