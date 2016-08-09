/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.context.notification.ServerNotification.getActionName;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_REQUEST_BEGIN;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_REQUEST_END;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.test.module.http.functional.TestConnectorMessageNotificationListener.register;
import static org.mule.test.module.http.functional.matcher.HttpMessageAttributesMatchers.hasReasonPhrase;
import static org.mule.test.module.http.functional.matcher.HttpMessageAttributesMatchers.hasStatusCode;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.context.DefaultMuleContextBuilder;
import org.mule.test.module.http.functional.TestConnectorMessageNotificationListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class HttpRequestNotificationsTestCase extends AbstractHttpRequestTestCase {

  @Override
  protected String getConfigFile() {
    return "http-request-notifications-config.xml";
  }

  @Override
  protected void configureMuleContext(MuleContextBuilder contextBuilder) {
    contextBuilder.setNotificationManager(register(DefaultMuleContextBuilder.createDefaultNotificationManager()));
    super.configureMuleContext(contextBuilder);
  }

  @Test
  public void receiveNotification() throws Exception {
    CountDownLatch latch = new CountDownLatch(2);
    TestConnectorMessageNotificationListener listener =
        new TestConnectorMessageNotificationListener(latch, "http://localhost:" + httpPort.getValue() + "/basePath/requestPath");
    muleContext.getNotificationManager().addListener(listener);

    MuleMessage response = flowRunner("requestFlow").withPayload(TEST_MESSAGE).run().getMessage();

    latch.await(1000, TimeUnit.MILLISECONDS);

    assertThat(listener.getNotificationActionNames(),
               contains(getActionName(MESSAGE_REQUEST_BEGIN), getActionName(MESSAGE_REQUEST_END)));

    // End event should have appended http.status and http.reason as inbound properties
    MuleMessage message = listener.getNotifications(getActionName(MESSAGE_REQUEST_END)).get(0).getSource();
    // For now, check the response, since we no longer have control over the MuleEvent generated, only the MuleMessage
    assertThat((HttpResponseAttributes) response.getAttributes(), hasStatusCode(OK.getStatusCode()));
    assertThat((HttpResponseAttributes) response.getAttributes(), hasReasonPhrase(OK.getReasonPhrase()));

    MuleMessage requestMessage = listener.getNotifications(getActionName(MESSAGE_REQUEST_BEGIN)).get(0).getSource();
    assertThat(requestMessage.getUniqueId(), equalTo(message.getUniqueId()));
    assertThat(requestMessage.getMessageRootId(), equalTo(message.getMessageRootId()));
  }

}
