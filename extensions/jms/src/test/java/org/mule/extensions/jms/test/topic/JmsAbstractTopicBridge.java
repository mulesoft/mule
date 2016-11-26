/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.test.topic;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import org.mule.extensions.jms.api.message.JmsAttributes;
import org.mule.extensions.jms.api.message.JmsMessageProperties;
import org.mule.extensions.jms.test.JmsAbstractTestCase;
import org.mule.runtime.core.api.message.InternalMessage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class JmsAbstractTopicBridge extends JmsAbstractTestCase {

  private static final String FIRST_MESSAGE = "My First Message";
  private static final String BRIDGED_PREFIX = "bridged_";
  private static final String BRIDGED_PREFIX_VAR = "bridgePrefix";
  private static final String BRIDGE_FLOW = "bridge";
  private static final String SEND_PAYLOAD_FLOW = "send-payload";
  private static final String BRIDGE_RECEIVER_FLOW = "bridge-receiver";
  private static final String INITIAL_DESTINATION = "initialTopic";
  private static final String INITIAL_DESTINATION_VAR = "initialDestination";
  private static final String FINAL_DESTINATION = "finalTopic";
  private static final String FINAL_DESTINATION_VAR = "finalDestination";
  private static final String PROPERTY_KEY_VAR = "initialProperty";
  private static final String PROPERTY_KEY_VALUE = "INIT_PROPERTY";
  private static final String PROPERTY_VALUE_VAR = "propertyValue";
  private static final String PROPERTY_VALUE_VALUE = "Custom Value";

  protected final String BRIDGE_CONFIG_XML = "operations/jms-topic-bridge.xml";
  private ExecutorService executor;

  @Before
  public void setup() {
    this.executor = newFixedThreadPool(2);
  }

  @After
  public void cleanup() {
    this.executor.shutdown();
  }

  @Test
  public void bridge() throws Exception {

    //Subscribe to source topic
    initBridgeListener();

    // Listen for bridged message
    Future<InternalMessage> bridgeTarget = initBridgeTargetListener();

    sendMessage();

    InternalMessage message = bridgeTarget.get();

    assertBridgedMessage(message);
  }

  protected void assertBridgedMessage(InternalMessage message) {
    assertThat(message, not(nullValue()));
    assertThat(message.getPayload(), not(nullValue()));
    assertThat(message.getPayload().getValue(), is(equalTo(BRIDGED_PREFIX + FIRST_MESSAGE)));
    assertThat(message.getAttributes(), not(nullValue()));

    JmsMessageProperties properties = ((JmsAttributes) message.getAttributes()).getProperties();
    assertThat(properties, not(nullValue()));
    assertThat(properties.getUserProperties().get(PROPERTY_KEY_VALUE), is(equalTo(PROPERTY_VALUE_VALUE)));
  }

  protected void sendMessage() throws Exception {
    flowRunner(SEND_PAYLOAD_FLOW)
        .withVariable(INITIAL_DESTINATION_VAR, INITIAL_DESTINATION)
        .withVariable(PROPERTY_KEY_VAR, PROPERTY_KEY_VALUE)
        .withVariable(PROPERTY_VALUE_VAR, PROPERTY_VALUE_VALUE)
        .withPayload(FIRST_MESSAGE).run();
  }

  protected Future<InternalMessage> initBridgeTargetListener() {
    return executor.submit(
                           () -> flowRunner(BRIDGE_RECEIVER_FLOW)
                               .withVariable(FINAL_DESTINATION_VAR, FINAL_DESTINATION)
                               .run().getMessage());
  }

  protected void initBridgeListener() {
    executor.submit(() -> {
      try {
        flowRunner(BRIDGE_FLOW)
            .withVariable(INITIAL_DESTINATION_VAR, INITIAL_DESTINATION)
            .withVariable(FINAL_DESTINATION_VAR, FINAL_DESTINATION)
            .withVariable(BRIDGED_PREFIX_VAR, BRIDGED_PREFIX)
            .run();
      } catch (Exception e) {
        fail(e.getMessage());
      }
    });
  }
}
