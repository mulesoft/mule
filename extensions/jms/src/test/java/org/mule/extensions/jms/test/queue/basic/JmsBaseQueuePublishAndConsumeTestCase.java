/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.test.queue.basic;

import static com.google.common.collect.ImmutableList.of;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static javax.jms.DeliveryMode.PERSISTENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mule.extensions.jms.api.destination.DestinationType.QUEUE;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import org.mule.extensions.jms.api.destination.JmsDestination;
import org.mule.extensions.jms.api.message.JmsAttributes;
import org.mule.extensions.jms.test.JmsAbstractTestCase;
import org.mule.runtime.api.message.Message;

import java.util.List;

import org.junit.Test;

public abstract class JmsBaseQueuePublishAndConsumeTestCase extends JmsAbstractTestCase {

  protected static final String DEFAULT_OPERATIONS_FLOW = "operations/jms-default-queue-operations.xml";

  @Test
  public void publishAndConsumeOnce() throws Exception {
    destination = newDestination("publishAndConsumeOnce");
    final String payload = "My Message";
    publish(payload);
    assertThat(consume(), hasPayload(equalTo(payload)));
  }

  @Test
  public void publishAndConsumeManyInOrder() throws Exception {
    destination = newDestination("publishAndConsumeManyInOrder");
    final List<String> messages = of("First", "Second", "Third", "Fourth");

    for (String m : messages) {
      publish(m);
    }

    for (String m : messages) {
      assertThat(consume(), hasPayload(equalTo(m)));
    }
  }

  @Test
  public void publishConsumeInOrderTillNoMoreAndContinueInOrder() throws Exception {
    destination = newDestination("publishConsumeInOrderTillNoMoreAndContinueInOrder");
    final List<String> firstMessages = of("First", "Second");
    final List<String> secondMessages = of("Third", "Fourth");

    for (String m : firstMessages) {
      publish(m);
    }

    for (String m : firstMessages) {
      assertThat(consume(), hasPayload(equalTo(m)));
    }

    Message nullPayload = consume(destination, emptyMap(), 0);
    assertThat(format("Expected no more messages available but consumption did not fail: %s",
                      nullPayload.getPayload().getValue()),
               nullPayload.getPayload().getValue(), nullValue());

    for (String m : secondMessages) {
      publish(m);
    }

    for (String m : secondMessages) {
      assertThat(consume(), hasPayload(equalTo(m)));
    }

    nullPayload = consume(destination, emptyMap(), 0);
    assertThat(format("Expected no more messages available but consumption did not fail: %s",
                      nullPayload.getPayload().getValue()),
               nullPayload.getPayload().getValue(), nullValue());
  }

  @Test
  public void expectedHeadersOnlyDefaultParameters() throws Exception {
    destination = newDestination("expectedHeadersOnlyDefaultParameters");
    final String payload = "My Message";
    publish(payload);
    Message message = consume();
    assertThat(message, hasPayload(equalTo(payload)));
    assertHeaders((JmsAttributes) message.getAttributes(),
                  new JmsDestination(destination, QUEUE), PERSISTENT,
                  4, true, true, null, null, null,
                  false);
  }

}
