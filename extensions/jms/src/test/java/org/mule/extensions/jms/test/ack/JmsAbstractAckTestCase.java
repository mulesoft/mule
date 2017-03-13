/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.test.ack;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.After;
import org.junit.Rule;
import org.mule.extensions.jms.api.config.AckMode;
import org.mule.extensions.jms.api.message.JmsAttributes;
import org.mule.extensions.jms.test.JmsAbstractTestCase;
import org.mule.extensions.jms.test.JmsMessageStorage;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.function.BooleanSupplier;

public abstract class JmsAbstractAckTestCase extends JmsAbstractTestCase {

  @Rule
  public SystemProperty destination = new SystemProperty("destination", newDestination("destination"));

  @Rule
  public SystemProperty ackMode = new SystemProperty("ack.mode", getAckMode().toString());

  @Rule
  public SystemProperty maxRedelivery = new SystemProperty(MAX_REDELIVERY, "1");

  @After
  public void cleanUpQueues() {
    JmsMessageStorage.cleanUpQueue();
  }

  public abstract AckMode getAckMode();

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"ack/jms-ack.xml", "config/activemq/activemq-default.xml"};
  }

  void assertQueueIsEmpty() throws Exception {
    try {
      JmsMessageStorage.pollMuleMessage();
      throw new RuntimeException();
    } catch (AssertionError error) {
      //
    }
  }

  void recoverSession(String ackId) throws Exception {
    flowRunner("recoverSession").withPayload(ackId).run();
  }

  void ackMessage(String ackId) throws Exception {
    flowRunner("doManualAck").withPayload(ackId).run();
  }

  void assertJmsMessage(Result<TypedValue<Object>, JmsAttributes> message, String jmsMessage, boolean isRedelivered) {
    Object value = message.getOutput().getValue();
    assertThat(value, is(jmsMessage));

    JmsAttributes attributes = message.getAttributes().get();
    assertThat(attributes.getHeaders().getJMSRedelivered(), is(isRedelivered));
  }

  String buildMessage(String message, Actions action) {
    return "{\"message\" : \"" + message + "\", \"action\" : \"" + action + "\"}";
  }

  public enum Actions {
    ACK, RECOVER, EXPLODE, NOTHING
  }

  void validate(BooleanSupplier validation, long validationTimeout, long validationDelay) {
    new PollingProber(validationTimeout, validationDelay).check(new JUnitLambdaProbe(validation));
  }
}
