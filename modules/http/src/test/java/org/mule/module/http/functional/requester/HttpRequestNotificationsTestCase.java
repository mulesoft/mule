/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mule.api.context.notification.ServerNotification.getActionName;
import static org.mule.context.notification.BaseConnectorMessageNotification.MESSAGE_REQUEST_BEGIN;
import static org.mule.context.notification.BaseConnectorMessageNotification.MESSAGE_REQUEST_END;
import static org.mule.module.http.functional.TestConnectorMessageNotificationListener.register;
import org.mule.api.context.MuleContextBuilder;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.module.http.functional.TestConnectorMessageNotificationListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class HttpRequestNotificationsTestCase extends AbstractHttpRequestTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "http-request-notifications-config.xml";
    }

    @Override
    protected void configureMuleContext(MuleContextBuilder contextBuilder)
    {
        contextBuilder.setNotificationManager(register(DefaultMuleContextBuilder.createDefaultNotificationManager()));
        super.configureMuleContext(contextBuilder);
    }

    @Test
    public void receiveNotification() throws Exception
    {
        CountDownLatch latch = new CountDownLatch(2);
        TestConnectorMessageNotificationListener listener = new TestConnectorMessageNotificationListener(latch, "http://localhost:" + httpPort.getValue() + "/basePath/requestPath");
        muleContext.getNotificationManager().addListener(listener);

        runFlow("requestFlow", TEST_MESSAGE);

        latch.await(1000, TimeUnit.MILLISECONDS);

        assertThat(listener.getNotificationActionNames(), contains(getActionName(MESSAGE_REQUEST_BEGIN), getActionName(MESSAGE_REQUEST_END)));
    }

}