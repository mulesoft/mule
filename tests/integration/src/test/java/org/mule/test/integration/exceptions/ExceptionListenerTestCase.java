/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.context.notification.ExceptionStrategyNotificationListener;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.context.notification.ExceptionStrategyNotification;
import org.mule.runtime.core.message.ExceptionMessage;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class ExceptionListenerTestCase extends AbstractIntegrationTestCase {

  private static final int TIMEOUT_MILLIS = 5000;
  private static final int POLL_DELAY_MILLIS = 100;

  private MuleClient client;
  private ServerNotification exceptionStrategyStartNotification;
  private ServerNotification exceptionStrategyEndNotification;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-listener-config-flow.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    client = muleContext.getClient();

    exceptionStrategyStartNotification = null;
    exceptionStrategyEndNotification = null;
    final ExceptionStrategyNotificationListener listener = notification -> {
      if (notification.getAction() == ExceptionStrategyNotification.PROCESS_START) {
        exceptionStrategyStartNotification = notification;
      } else if (notification.getAction() == ExceptionStrategyNotification.PROCESS_END) {
        exceptionStrategyEndNotification = notification;
      }
    };
    muleContext.getNotificationManager().addListener(listener);
  }

  @Test
  public void testExceptionStrategyFromComponent() throws Exception {
    assertQueueIsEmpty("test://error.queue");

    flowRunner("mycomponent").withPayload("test").asynchronously().run();

    assertQueueIsEmpty("test://component.out");

    InternalMessage message = client.request("test://error.queue", 2000).getRight().get();
    assertNotNull(message);
    Object payload = message.getPayload().getValue();
    assertTrue(payload instanceof ExceptionMessage);

    assertNotificationsArrived();
    assertNotificationsHaveMatchingResourceIds();
  }

  private void assertNotificationsHaveMatchingResourceIds() {
    assertThat(exceptionStrategyStartNotification.getResourceIdentifier(), is(not(nullValue())));
    assertThat(exceptionStrategyStartNotification.getResourceIdentifier(), is("mycomponent"));
    assertThat(exceptionStrategyStartNotification.getResourceIdentifier(),
               is(exceptionStrategyEndNotification.getResourceIdentifier()));
  }

  private void assertNotificationsArrived() {
    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        assertThat(exceptionStrategyStartNotification, is(not(nullValue())));
        assertThat(exceptionStrategyEndNotification, is(not(nullValue())));
        return true;
      }

      @Override
      public String describeFailure() {
        return "Did not get exception strategy notifications";
      }
    });
  }

  private void assertQueueIsEmpty(String queueName) throws MuleException {
    assertThat(client.request(queueName, 2000).getRight().isPresent(), is(false));
  }
}
