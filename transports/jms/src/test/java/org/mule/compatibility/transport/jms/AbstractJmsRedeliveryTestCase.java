/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.functional.exceptions.FunctionalTestException;
import org.mule.functional.functional.CounterCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.context.notification.ExceptionNotificationListener;
import org.mule.runtime.core.exception.MessageRedeliveredException;
import org.mule.runtime.core.context.notification.ExceptionNotification;
import org.mule.runtime.core.context.notification.NotificationException;
import org.mule.runtime.core.message.ExceptionMessage;
import org.mule.runtime.core.util.concurrent.Latch;

import org.junit.After;
import org.junit.Before;

public abstract class AbstractJmsRedeliveryTestCase extends FunctionalTestCase {

  protected static final String JMS_INPUT_QUEUE = "jms://in?connector=jmsConnectorLimitedRedelivery";
  protected static final String JMS_INPUT_QUEUE2 = "jms://in2?connector=jmsConnectorNoRedelivery";
  protected static final String JMS_DEAD_LETTER = "jms://dead.letter?connector=jmsConnectorNoRedelivery";
  protected final int timeout = getTestTimeoutSecs() * 1000 / 4;

  protected MuleClient client;
  protected Latch messageRedeliveryExceptionFired;
  protected CounterCallback callback;

  public AbstractJmsRedeliveryTestCase() {
    System.setProperty("maxRedelivery", String.valueOf(getMaxRedelivery()));
    System.setProperty("maxRedeliveryAttempts", String.valueOf(getMaxRedeliveryAttempts()));
  }

  @Override
  protected String getConfigFile() {
    return "jms-redelivery-flow.xml";
  }

  @Before
  public void setUp() throws Exception {
    client = muleContext.getClient();
    messageRedeliveryExceptionFired = new Latch();
    registerEventListener(messageRedeliveryExceptionFired);
    purgeQueue();
    setupCallback();
  }

  protected void assertMessageInDlq() throws MuleException {
    InternalMessage dl = client.request(JMS_DEAD_LETTER, 1000).getRight().get();
    assertNotNull(dl);
    assertTrue(dl.getPayload().getValue() instanceof ExceptionMessage);
    ExceptionMessage em = (ExceptionMessage) dl.getPayload().getValue();
    assertNotNull(em.getException());
    assertTrue(em.getException() instanceof MessageRedeliveredException);
  }

  protected void assertMessageInDlqRollbackEs() throws Exception {
    InternalMessage dl = client.request(JMS_DEAD_LETTER, 1000).getRight().get();
    assertNotNull(dl);
    assertTrue(getPayloadAsString(dl).equals(TEST_MESSAGE));
  }

  protected void purgeQueue() throws MuleException {
    // required if broker is not restarted with the test - it tries to deliver those messages to the client
    // purge the queue
    while (client.request(JMS_INPUT_QUEUE, 1000).getRight().isPresent()) {
      logger.warn("Destination " + JMS_INPUT_QUEUE + " isn't empty, draining it");
    }
  }

  protected void setupCallback() throws Exception {
    callback = createCallback();
    FunctionalTestComponent ftc = getFunctionalTestComponent("Bouncer");
    FunctionalTestComponent ftc2 = getFunctionalTestComponent("Bouncer2");
    ftc.setEventCallback(callback);
    ftc2.setEventCallback(callback);
  }

  private CounterCallback createCallback() {
    // enhance the counter callback to count, then throw an exception
    return new CounterCallback() {

      @Override
      public void eventReceived(MuleEventContext context, Object Component, MuleContext muleContext) throws Exception {
        final int count = incCallbackCount();
        logger.info("Message Delivery Count is: " + count);
        throw new FunctionalTestException();
      }
    };
  }

  private void registerEventListener(final Latch messageRedeliveryExceptionFired) throws NotificationException {
    final ExceptionNotificationListener<ExceptionNotification> listener = notification -> {
      if (notification.getException() instanceof MessageRedeliveredException) {
        messageRedeliveryExceptionFired.countDown();
      }
    };
    muleContext.registerListener(listener);
  }

  protected void assertNoMessageInDlq(String location) throws MuleException {
    assertThat(client.request(location, 1000).getRight().isPresent(), is(false));
  }

  @After
  public void cleanUpMaxRedelivery() {
    System.clearProperty("maxRedelivery");
    System.clearProperty("maxRedeliveryAttempts");
  }

  protected abstract int getMaxRedelivery();

  protected abstract int getMaxRedeliveryAttempts();
}
