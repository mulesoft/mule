/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.test.topic.basic;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import org.mule.extensions.jms.api.destination.QueueConsumer;
import org.mule.extensions.jms.test.JmsAbstractTestCase;
import org.mule.runtime.core.api.message.InternalMessage;

import com.google.common.collect.ImmutableMap;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;


public abstract class JmsBaseTopicListenResponseTestCase extends JmsAbstractTestCase {

  protected static final String LISTENER_CONFIG = "source/jms-listen-reply.xml";
  protected static final String REQUEST_REPLY_CONFIG = "operations/jms-topic-request-reply.xml";

  private static final String FIRST_MESSAGE = "My First Message";
  private static final String FIRST_RESPONSE = "First Response";

  private static final String REQUEST_REPLY_EXPLICIT_DEST_FLOW = "request-reply-explicit-destination";

  private static final String LISTENER_DESTINATION = "topicListenerDestination";

  private static final String REPLY_TO_DESTINATION_VAR = "replyToDestination";
  private static final String REPLY_TO_DESTINATION = "replyDestination";
  private static final String REPLY_TO_DESTINATION_TYPE_VAR = "replyToDestinationType";
  private static final String REPLY_CONSUMER_TYPE_VAR = "consumerType";

  private static final String READ_MESSAGE_PREFIX = "received_";


  private ExecutorService executor;

  @Before
  public void setup() {
    this.executor = newFixedThreadPool(2);
  }

  @After
  public void cleanup() {
    if (this.executor != null) {
      this.executor.shutdown();
    }
  }

  @Test
  public void listenOnceReplyToQueue() throws Exception {
    final String payload = "My Message";

    publish(payload, LISTENER_DESTINATION,
            ImmutableMap.<String, Object>builder()
                .put(REPLY_TO_DESTINATION_VAR, REPLY_TO_DESTINATION)
                .put(REPLY_TO_DESTINATION_TYPE_VAR, "QUEUE")
                .build());

    InternalMessage reply = consume(REPLY_TO_DESTINATION, of(REPLY_CONSUMER_TYPE_VAR, new QueueConsumer()), -1);
    assertThat(reply, hasPayload(equalTo(READ_MESSAGE_PREFIX + payload)));
  }

  @Test
  @Description("Checks that a message can be sent and then wait for the reply to an explicit replyTo destination")
  public void requestReplyExplicitReplyToTopic() throws Exception {
    destination = newDestination("first_requestReplyExplicitReplyDestination");

    // Publish initial requests and wait for responses in the same reply queue
    Future<InternalMessage> firstRequestReply = executor
        .submit(() -> flowRunner(REQUEST_REPLY_EXPLICIT_DEST_FLOW)
            .withVariable(DESTINATION_VAR, destination)
            .withVariable(REPLY_TO_DESTINATION_VAR, REPLY_TO_DESTINATION)
            .withPayload(FIRST_MESSAGE)
            .run()
            .getMessage());

    // Consume the published messages
    InternalMessage firstMessage = consume(destination, of(REPLY_CONSUMER_TYPE_VAR, new QueueConsumer()), -1);
    assertThat(firstMessage, hasPayload(equalTo(FIRST_MESSAGE)));
    String firstReplyDestination = getReplyDestination(firstMessage);
    assertThat(firstReplyDestination, is(equalTo(REPLY_TO_DESTINATION)));

    flowRunner(PUBLISHER_FLOW)
        .withVariable(DESTINATION_VAR, REPLY_TO_DESTINATION)
        .withVariable(REPLY_TO_DESTINATION_VAR, REPLY_TO_DESTINATION)
        .withVariable(REPLY_TO_DESTINATION_TYPE_VAR, "TOPIC")
        .withPayload(FIRST_RESPONSE)
        .run();

    // Read the reply result
    InternalMessage firstReply = firstRequestReply.get();
    assertThat(firstReply, hasPayload(equalTo(FIRST_RESPONSE)));
    assertThat(firstReply.getAttributes(), not(nullValue()));
  }

}
