/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.test;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mule.extensions.jms.test.JmsMessageStorage.cleanUpQueue;
import static org.mule.test.allure.AllureConstants.JmsFeature.JMS_EXTENSION;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.api.destination.JmsDestination;
import org.mule.extensions.jms.api.message.JmsAttributes;
import org.mule.extensions.jms.api.message.JmsHeaders;
import org.mule.functional.junit4.FlowRunner;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.slf4j.Logger;
import ru.yandex.qatools.allure.annotations.Features;

@Features(JMS_EXTENSION)
@ArtifactClassLoaderRunnerConfig(testInclusions = {"org.apache.activemq:artemis-jms-client"})
public abstract class JmsAbstractTestCase extends MuleArtifactFunctionalTestCase {

  private static final Logger LOGGER = getLogger(JmsAbstractTestCase.class);

  protected static final String NAMESPACE = "JMS";
  protected static final String DESTINATION_VAR = "destination";
  protected static final String MAXIMUM_WAIT_VAR = "maximumWait";

  protected static final String PUBLISHER_FLOW = "publisher";
  protected static final String CONSUMER_FLOW = "consumer";
  protected static final String LISTENER_FLOW = "listener";

  protected static final int TIMEOUT_MILLIS = 10000;
  protected static final int POLL_DELAY_MILLIS = 100;
  protected static final String MAX_REDELIVERY = "max.redelivery";
  protected String destination;
  protected long maximumWait = 10000;

  @Rule
  public SystemProperty maxRedelivery = new SystemProperty(MAX_REDELIVERY, "0");

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();
  }

  @Before
  public void setUp() {
    cleanUpQueue();
  }

  @Override
  protected void doTearDown() throws Exception {
    cleanUpQueue();
  }

  protected String newDestination(String name) {
    return name + currentTimeMillis();
  }

  protected void publish(Object message) throws Exception {
    publish(message, destination);
  }

  protected void publish(Object message, String destination) throws Exception {
    publish(message, destination, emptyMap());
  }

  protected void publish(Object message, String destination, Map<String, Object> flowVars) throws Exception {
    FlowRunner publisher = flowRunner(PUBLISHER_FLOW)
        .withPayload(message)
        .withVariable(DESTINATION_VAR, destination);
    flowVars.forEach(publisher::withVariable);
    publisher.run();
  }

  protected Message consume() throws Exception {
    return consume(destination, emptyMap(), maximumWait);
  }

  protected Message consume(String destination) throws Exception {
    return consume(destination, emptyMap(), maximumWait);
  }

  protected Message consume(String destination, Map<String, Object> flowVars) throws Exception {
    return consume(destination, flowVars, maximumWait);
  }

  protected Message consume(String destination, Map<String, Object> flowVars, long maximumWait) throws Exception {
    FlowRunner consumer = flowRunner(CONSUMER_FLOW)
        .withVariable(DESTINATION_VAR, destination)
        .withVariable(MAXIMUM_WAIT_VAR, maximumWait);
    flowVars.forEach(consumer::withVariable);
    return consumer.run().getMessage();
  }

  protected void assertHeaders(JmsAttributes attributes, JmsDestination destination, Integer deliveryMode,
                               Integer priority, boolean hasMessageId, boolean hasTimestamp, String correlactionId,
                               JmsDestination replyTo, String type, Boolean redelivered) {

    JmsHeaders headers = attributes.getHeaders();
    assertThat(headers, notNullValue());
    assertThat(headers.getJMSMessageID(), hasMessageId ? not(isEmptyOrNullString()) : nullValue());
    assertThat(headers.getJMSTimestamp(), hasTimestamp ? not(nullValue()) : nullValue());
    assertThat(headers.getJMSCorrelationID(), equalTo(correlactionId));
    assertThat(headers.getJMSDeliveryMode(), equalTo(deliveryMode));
    assertThat(headers.getJMSPriority(), equalTo(priority));
    assertThat(headers.getJMSRedelivered(), equalTo(redelivered));
    assertThat(headers.getJMSType(), equalTo(type));

    assertDestination(headers.getJMSDestination(), destination);

    if (replyTo == null) {
      assertThat(headers.getJMSReplyTo(), nullValue());
    } else {
      assertDestination(headers.getJMSReplyTo(), destination);
    }
  }

  private void assertDestination(JmsDestination actual, JmsDestination expected) {
    assertThat(actual.getDestination(), equalTo(expected.getDestination()));
    assertThat(actual.getDestinationType(), equalTo(expected.getDestinationType()));
  }

  protected String getReplyDestination(Message firstMessage) {
    return ((JmsAttributes) firstMessage.getAttributes().getValue()).getHeaders().getJMSReplyTo().getDestination();
  }
}
