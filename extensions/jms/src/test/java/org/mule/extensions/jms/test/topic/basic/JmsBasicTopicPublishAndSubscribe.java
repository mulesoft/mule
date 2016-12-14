/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.test.topic.basic;

import static com.google.common.collect.ImmutableList.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsIn.isIn;
import static org.hamcrest.core.Is.is;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import org.mule.extensions.jms.test.JmsAbstractTestCase;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public abstract class JmsBasicTopicPublishAndSubscribe extends JmsAbstractTestCase {

  public static final String DEFAULT_OPERATIONS_CONFIG = "operations/jms-default-topic-operations.xml";
  public static final String SUBSCRIBER_CONFIG = "source/jms-default-topic-subscribe.xml";
  public static final String LISTENER_DESTINATION = "topicSubscriberDestinationToOnIncomingConnection";

  @Before
  public void setDestination() {
    destination = LISTENER_DESTINATION;
  }

  @Test
  public void publishOnce() throws Exception {
    final String payload = "My Message";
    publish(payload);
    assertThat(receiveIncomingMessage(), hasPayload(equalTo(payload)));
    assertThat(receivedMessages.isEmpty(), is(true));
  }

  @Test
  public void publishAndListenManyInOrder() throws Exception {
    final List<String> messages = of("First", "Second", "Third", "Fourth");
    messages.forEach(this::publish);
    messages.forEach(payload -> assertThat(receiveIncomingMessage(), hasPayload(isIn(messages))));
    assertThat(receivedMessages.isEmpty(), is(true));
  }

  @Test
  public void publishListenInOrderTillNoMoreAndContinueInOrder() throws Exception {
    final List<String> firstMessages = of("First", "Second");
    final List<String> secondMessages = of("Third", "Fourth");

    firstMessages.forEach(this::publish);
    firstMessages.forEach(payload -> assertThat(receiveIncomingMessage(), hasPayload(isIn(firstMessages))));

    assertThat(receivedMessages.isEmpty(), is(true));

    secondMessages.forEach(this::publish);
    secondMessages.forEach(payload -> assertThat(receiveIncomingMessage(), hasPayload(isIn(secondMessages))));

    assertThat(receivedMessages.isEmpty(), is(true));
  }

}
