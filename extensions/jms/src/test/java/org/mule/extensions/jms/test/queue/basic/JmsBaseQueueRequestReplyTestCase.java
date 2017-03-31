/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.test.queue.basic;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import org.mule.extensions.jms.test.JmsAbstractTestCase;
import org.mule.runtime.api.message.Message;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;

public abstract class JmsBaseQueueRequestReplyTestCase extends JmsAbstractTestCase {

  protected static final String DEFAULT_OPERATIONS_CONFIG = "operations/jms-default-queue-operations.xml";
  protected static final String PUBLISH_CONSUME_OPERATIONS_CONFIG = "operations/jms-queue-request-reply.xml";

  private static final String REQUEST_REPLY_EXPLICIT_DEST_FLOW = "request-reply-explicit-destination";
  private static final String REQUEST_REPLY_TEMP_DEST_FLOW = "request-reply-temp-destination";

  private static final String FIRST_MESSAGE = "My First Message";
  private static final String SECOND_MESSAGE = "My Second Message";
  private static final String FIRST_RESPONSE = "First Response";
  private static final String SECOND_RESPONSE = "Second Response";

  private static final String REPLY_TO_DESTINATION_VAR = "replyToDestination";
  private static final String REPLY_TO_DESTINATION = "replyQueue";

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
  @Description("Checks that a message can be sent and then wait for the reply to an explicit replyTo destination")
  public void requestReplyExplicitReplyDestination() throws Exception {
    destination = newDestination("first_requestReplyExplicitReplyDestination");

    // Publish initial requests and wait for responses in the same reply queue
    Future<Message> firstRequestReply = executor
        .submit(() -> flowRunner(REQUEST_REPLY_EXPLICIT_DEST_FLOW)
            .withVariable(DESTINATION_VAR, destination)
            .withVariable(REPLY_TO_DESTINATION_VAR, REPLY_TO_DESTINATION)
            .withPayload(FIRST_MESSAGE)
            .run()
            .getMessage());

    // Consume the published messages
    Message firstMessage = consume(destination, emptyMap(), -1);
    assertThat(firstMessage, hasPayload(equalTo(FIRST_MESSAGE)));
    String firstReplyDestination = getReplyDestination(firstMessage);
    assertThat(firstReplyDestination, is(equalTo(REPLY_TO_DESTINATION)));

    reply(REPLY_TO_DESTINATION, FIRST_RESPONSE);

    // Read the reply result
    Message firstReply = firstRequestReply.get();
    assertThat(firstReply, hasPayload(equalTo(FIRST_RESPONSE)));
    assertThat(firstReply.getAttributes(), not(nullValue()));
  }

  @Test
  @Description("Checks that a message can be sent and then wait for the reply to dynamic temporary destination")
  public void requestReplyTemporaryReplyDestination() throws Exception {
    destination = newDestination("requestReplyTemporaryReplyDestination");

    // Publish initial requests and wait for responses in different queues
    Future<Message> firstRequestReply = executor
        .submit(() -> flowRunner(REQUEST_REPLY_TEMP_DEST_FLOW)
            .withVariable(DESTINATION_VAR, destination)
            .withPayload(FIRST_MESSAGE)
            .run()
            .getMessage());

    // Consume the published messages
    Message firstMessage = consume(destination, emptyMap(), -1);
    String firstReplyDestination = getReplyDestination(firstMessage);

    // Reply to the given destinations
    reply(firstReplyDestination, FIRST_RESPONSE);

    // Read the reply result
    Message firstReply = firstRequestReply.get();
    assertThat(firstReply, hasPayload(equalTo(FIRST_RESPONSE)));
    assertThat(firstReply.getAttributes(), not(nullValue()));
  }

  @Test
  @Description("Checks that a message can be sent and then wait for the reply to an explicit replyTo destination")
  public void requestReplyExplicitPreSentReply() throws Exception {
    final String firstDestination = newDestination("first_requestReplyExplicitPreSentReply");

    reply(REPLY_TO_DESTINATION, FIRST_RESPONSE);
    reply(REPLY_TO_DESTINATION, SECOND_RESPONSE);

    // Publish initial requests and wait for responses in the same reply queue
    Message firstReply = flowRunner(REQUEST_REPLY_EXPLICIT_DEST_FLOW)
        .withVariable(DESTINATION_VAR, firstDestination)
        .withVariable(REPLY_TO_DESTINATION_VAR, REPLY_TO_DESTINATION)
        .withPayload(FIRST_MESSAGE)
        .run()
        .getMessage();

    final String secondDestination = newDestination("second_requestReplyExplicitPreSentReply");
    Message secondReply = flowRunner(REQUEST_REPLY_EXPLICIT_DEST_FLOW)
        .withVariable(DESTINATION_VAR, secondDestination)
        .withVariable(REPLY_TO_DESTINATION_VAR, REPLY_TO_DESTINATION)
        .withPayload(SECOND_MESSAGE)
        .run()
        .getMessage();

    // Consume the published messages
    Message firstMessage = consume(firstDestination, emptyMap(), -1);
    assertThat(firstMessage, hasPayload(equalTo(FIRST_MESSAGE)));
    String firstReplyDestination = getReplyDestination(firstMessage);
    assertThat(firstReplyDestination, is(equalTo(REPLY_TO_DESTINATION)));

    Message secondMessage = consume(secondDestination, emptyMap(), -1);
    assertThat(secondMessage, hasPayload(equalTo(SECOND_MESSAGE)));
    String secondReplyDestination = getReplyDestination(secondMessage);
    assertThat(secondReplyDestination, is(equalTo(REPLY_TO_DESTINATION)));

    assertThat(firstReply, hasPayload(equalTo(FIRST_RESPONSE)));
    assertThat(firstReply.getAttributes(), not(nullValue()));

    assertThat(secondReply, hasPayload(equalTo(SECOND_RESPONSE)));
    assertThat(secondReply.getAttributes(), not(nullValue()));
  }

  private void reply(String replyDestination, String message) throws Exception {
    flowRunner(PUBLISHER_FLOW)
        .withVariable(DESTINATION_VAR, replyDestination)
        .withPayload(message)
        .run();
  }

}
