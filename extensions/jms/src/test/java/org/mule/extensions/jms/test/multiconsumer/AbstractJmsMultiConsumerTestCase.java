/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.test.multiconsumer;

import org.mule.extensions.jms.api.destination.DestinationType;
import org.mule.extensions.jms.test.JmsAbstractTestCase;
import org.mule.extensions.jms.test.JmsMessageStorage;
import org.mule.tck.junit4.rule.SystemProperty;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.function.Function;
import java.util.stream.Stream;

public abstract class AbstractJmsMultiConsumerTestCase extends JmsAbstractTestCase {

  static final String NUMBER_OF_CONSUMERS = "4";
  static final int NUMBER_OF_MESSAGES = 12;

  @Rule
  public SystemProperty destination = new SystemProperty("destination", newDestination("destination"));

  @Rule
  public SystemProperty topicDestination = new SystemProperty("topicDestination", newDestination("topicDestination"));

  @Rule
  public SystemProperty numberOfConsumers = new SystemProperty("consumers", NUMBER_OF_CONSUMERS);

  @Before
  public void setUp() {
    JmsMessageStorage.cleanUpQueue();
  }

  void ackMessage(String ackId) throws Exception {
    flowRunner("doManualAck").withPayload(ackId).run();
  }

  void recoverSession(String ackId) throws Exception {
    flowRunner("recoverSession").withPayload(ackId).run();
  }

  void publishTo(int amount, String destination, DestinationType destinationType) throws Exception {
    for (int i = 0; i < amount; i++) {
      publish("message", destination, ImmutableMap.of("destinationType", destinationType));
    }
  }

  <T, S> Stream<T> aFor(S initialState, Function<S, Boolean> condition, Function<S, S> updater, Function<S, T> supplier) {
    S currentState = initialState;
    Stream.Builder<T> builder = Stream.builder();
    while (condition.apply(currentState)) {
      builder.add(supplier.apply(currentState));
      currentState = updater.apply(currentState);
    }
    return builder.build();
  }
}
