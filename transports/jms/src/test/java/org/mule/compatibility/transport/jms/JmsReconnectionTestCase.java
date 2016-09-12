/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.compatibility.core.api.transport.MessageReceiver;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.util.Collection;

import javax.jms.Connection;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Ignore;
import org.junit.Test;

public class JmsReconnectionTestCase extends AbstractBrokerFunctionalTestCase {

  private static final int CONSUMER_COUNT = 4;
  private static final int TIMEOUT_MILLIS = 5000;
  private static final String PAYLOAD = "HELLO";

  private MultiConsumerJmsMessageReceiver receiver;
  private Connection connection;

  @Override
  protected String getConfigFile() {
    return "jms-reconnection-config.xml";
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();

    // this is needed because for some reason the broker will reject any connections
    // otherwise
    connection = new ActiveMQConnectionFactory(getConnectorUrl()).createQueueConnection();
  }

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    connection.close();
    super.doTearDownAfterMuleContextDispose();
  }

  @Test
  @Ignore("MULE-9628")
  public void reconnectAllConsumers() throws Exception {
    this.assertMessageRouted();

    final JmsConnector connector = muleContext.getRegistry().lookupObject("activemqconnector");

    Collection<MessageReceiver> receivers = connector.getReceivers().values();
    assertTrue(receivers != null && receivers.size() == 1);
    this.receiver = (MultiConsumerJmsMessageReceiver) receivers.iterator().next();
    assertConsumersCount();

    amqBroker.stop();

    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, 500);
    prober.check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return receiver.consumers.isEmpty();
      }

      @Override
      public String describeFailure() {
        return "consumers were never released";
      }
    });

    amqBroker.start();

    prober.check(new Probe() {

      @Override
      public boolean isSatisfied() {
        try {
          assertConsumersCount();
          return true;
        } catch (AssertionError e) {
          return false;
        }
      }

      @Override
      public String describeFailure() {
        return "receivers never came back";
      }
    });

    this.assertMessageRouted();
  }

  private void assertConsumersCount() {
    assertEquals(CONSUMER_COUNT, this.receiver.consumers.size());

    for (MultiConsumerJmsMessageReceiver.SubReceiver consumer : this.receiver.consumers) {
      assertTrue(consumer.connected);
    }
  }

  private void assertMessageRouted() throws Exception {
    flowRunner("put").withPayload(PAYLOAD).run();
    InternalMessage message = muleContext.getClient().request("vm://out", TIMEOUT_MILLIS).getRight().get();
    assertNotNull(message);
    assertEquals(PAYLOAD, message.getPayload().getValue());
  }
}
