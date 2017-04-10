/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static org.mule.runtime.core.api.context.notification.ServerNotification.getActionName;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RESPONSE;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static org.mule.test.module.http.functional.TestConnectorMessageNotificationListener.register;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.context.DefaultMuleContextBuilder;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.http.functional.AbstractHttpTestCase;
import org.mule.test.module.http.functional.TestConnectorMessageNotificationListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.fluent.Request;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;

@Features(HTTP_EXTENSION)
public class HttpListenerNotificationsTestCase extends AbstractHttpTestCase {

  @Rule
  public DynamicPort listenPort = new DynamicPort("port");
  @Rule
  public SystemProperty path = new SystemProperty("path", "path");

  @Override
  protected void configureMuleContext(MuleContextBuilder contextBuilder) {
    contextBuilder.setNotificationManager(register(DefaultMuleContextBuilder.createDefaultNotificationManager()));
    super.configureMuleContext(contextBuilder);
  }

  @Override
  protected String getConfigFile() {
    return "http-listener-notifications-config.xml";
  }

  @Test
  public void receiveNotification() throws Exception {
    String listenerUrl = String.format("http://localhost:%s/%s", listenPort.getNumber(), path.getValue());

    CountDownLatch latch = new CountDownLatch(2);
    // for now use none since we have no way of sending the endpoint
    TestConnectorMessageNotificationListener listener = new TestConnectorMessageNotificationListener(latch, "http");
    muleContext.getNotificationManager().addListener(listener);

    Request.Post(listenerUrl).execute();

    latch.await(1000, TimeUnit.MILLISECONDS);

    assertThat(listener.getNotificationActionNames(), contains(getActionName(MESSAGE_RECEIVED), getActionName(MESSAGE_RESPONSE)));
  }

}
