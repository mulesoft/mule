/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.filter;


import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.context.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE;
import static org.mule.context.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE;
import static org.mule.context.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE_ORIGINAL_EVENT;
import org.mule.api.context.notification.MessageProcessorNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.component.DefaultJavaComponent;
import org.mule.context.notification.MessageProcessorNotification;
import org.mule.routing.MessageFilter;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class MessageFilterNotificationsTestCase extends FunctionalTestCase
{

    private List<MessageProcessorNotification> notifications = new ArrayList<>();

    @Before
    public void setUp() throws Exception
    {
        muleContext.getNotificationManager().addInterfaceToType(MessageProcessorNotificationListener.class, MessageProcessorNotification.class);
        muleContext.registerListener(new MessageProcessorNotificationListener()
        {
            @Override
            public void onNotification(ServerNotification notification)
            {
                notifications.add((MessageProcessorNotification) notification);
            }
        });
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/filter/message-filter-notification-config.xml";
    }

    @Test
    public void testNotificationSubFlow() throws Exception
    {
        runFlow("unacceptedCaseSubFlow");
        assertNotifications();
    }

    @Test
    public void testNotificationFlow() throws Exception
    {
        runFlow("unacceptedCaseFlow");
        assertNotifications();
    }

    private void assertNotifications ()
    {
        assertThat(notifications, is(notNullValue()));
        assertThat(notifications.size(), is (6));
        assertNotification(notifications.get(0), MessageFilter.class, MESSAGE_PROCESSOR_PRE_INVOKE);
        assertNotification(notifications.get(1), MessageFilter.class, MESSAGE_PROCESSOR_PRE_INVOKE_ORIGINAL_EVENT);
        assertNotification(notifications.get(2), DefaultJavaComponent.class, MESSAGE_PROCESSOR_PRE_INVOKE);
        assertNotification(notifications.get(3), DefaultJavaComponent.class, MESSAGE_PROCESSOR_PRE_INVOKE_ORIGINAL_EVENT);
        assertNotification(notifications.get(4), DefaultJavaComponent.class, MESSAGE_PROCESSOR_POST_INVOKE);
        assertNotification(notifications.get(5), MessageFilter.class, MESSAGE_PROCESSOR_POST_INVOKE);
    }

    private void assertNotification (MessageProcessorNotification notification, Class processorClass, int action)
    {
        assertThat(notification.getProcessor(), instanceOf(processorClass));
        assertThat(notification.getAction(), is(action));
    }

}
