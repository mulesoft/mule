/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.test.topic.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mule.extensions.jms.test.JmsMessageStorage.pollMuleMessage;
import org.junit.Rule;
import org.junit.Test;
import org.mule.extensions.jms.api.message.JmsAttributes;
import org.mule.extensions.jms.api.message.JmsMessageProperties;
import org.mule.extensions.jms.test.JmsAbstractTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.tck.junit4.rule.SystemProperty;

public abstract class JmsAbstractTopicBridge extends JmsAbstractTestCase {

  private static final String FIRST_MESSAGE = "My First Message";
  private static final String BRIDGED_PREFIX = "bridged_";
  private static final String SEND_PAYLOAD_FLOW = "send-payload";
  private static final String PROPERTY_KEY_VAR = "initialProperty";
  private static final String PROPERTY_KEY_VALUE = "INIT_PROPERTY";
  private static final String PROPERTY_VALUE_VAR = "propertyValue";
  private static final String PROPERTY_VALUE_VALUE = "Custom Value";

  final String BRIDGE_CONFIG_XML = "operations/jms-topic-bridge.xml";

  @Rule
  public SystemProperty initialDestination = new SystemProperty("initialDestination", newDestination("iniDest"));

  @Rule
  public SystemProperty finalDestination = new SystemProperty("finalDestination", newDestination("finalDest"));

  @Test
  public void bridge() throws Exception {
    sendMessage();
    assertBridgedMessage(pollMuleMessage());
  }

  private void assertBridgedMessage(Message message) {
    assertThat(message, not(nullValue()));
    assertThat(message.getPayload(), not(nullValue()));
    assertThat(message.getPayload().getValue(), is(equalTo(BRIDGED_PREFIX + FIRST_MESSAGE)));
    assertThat(message.getAttributes(), not(nullValue()));

    JmsMessageProperties properties = ((JmsAttributes) message.getAttributes()).getProperties();
    assertThat(properties, not(nullValue()));
    assertThat(properties.getUserProperties().get(PROPERTY_KEY_VALUE), is(equalTo(PROPERTY_VALUE_VALUE)));
  }

  private void sendMessage() throws Exception {
    flowRunner(SEND_PAYLOAD_FLOW)
        .withVariable(PROPERTY_KEY_VAR, PROPERTY_KEY_VALUE)
        .withVariable(PROPERTY_VALUE_VAR, PROPERTY_VALUE_VALUE)
        .withPayload(FIRST_MESSAGE).run();
  }
}
