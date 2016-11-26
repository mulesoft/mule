/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.test.queue;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import org.mule.extensions.jms.api.message.JmsAttributes;
import org.mule.extensions.jms.test.JmsAbstractTestCase;
import org.mule.runtime.core.api.message.InternalMessage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("JMS Extension")
@Stories("Queue Request Reply")
public class JmsTemporaryQueueRequestReplyTestCase extends JmsAbstractTestCase {

  private static final String FIRST_MESSAGE = "My First Message";
  private static final String SECOND_MESSAGE = "My Second Message";
  private static final String PUBLISHER_FLOW = "publisher";
  private static final String REQUEST_REPLY_FLOW = "request-reply";
  private static final String REQUEST_DESTINATION = "postedQueue";
  private static final String REQUEST_DESTINATION_VAR = "requestDestination";
  private static final String REPLY_TO_DESTINATION = "replyQueue";
  private static final String REPLY_TO_DESTINATION_VAR = "replyToDestination";
  private static final String REQUEST_LISTENER_FLOW = "request-listener";


  @Override
  protected String[] getConfigFiles() {
    return new String[] {
        "config/activemq/activemq-default.xml",
        "operations/jms-queue-temporary-request-reply.xml"
    };
  }

  @Test
  @Description("Checks that a message can be sent and then wait for the reply to dynamic temporary destination")
  public void requestReplyTemporaryReplyDestination() throws Exception {
    ExecutorService executor = newFixedThreadPool(2);

    Future<InternalMessage> requesterTarget = executor
        .submit(() -> flowRunner(REQUEST_LISTENER_FLOW)
            .withVariable(REQUEST_DESTINATION_VAR, REQUEST_DESTINATION)
            .run().getMessage());

    Future<InternalMessage> requestReply = executor
        .submit(() -> flowRunner(REQUEST_REPLY_FLOW)
            .withVariable(REQUEST_DESTINATION_VAR, REQUEST_DESTINATION)
            .withVariable(REPLY_TO_DESTINATION_VAR, REPLY_TO_DESTINATION)
            .withPayload(SECOND_MESSAGE).run()
            .getMessage());

    // Read the message posted by the request flow
    InternalMessage requestedMessage = requesterTarget.get();

    assertThat(requestedMessage, not(nullValue()));
    assertThat(requestedMessage.getPayload(), not(nullValue()));
    assertThat(requestedMessage.getPayload().getValue(), is(equalTo(SECOND_MESSAGE)));
    assertThat(requestedMessage.getAttributes(), not(nullValue()));

    // Post a message to be read when listening for a reply in the "replyTo"
    String replyDestination = ((JmsAttributes) requestedMessage.getAttributes()).getHeaders().getJMSReplyTo().getDestination();
    flowRunner(PUBLISHER_FLOW)
        .withVariable(REPLY_TO_DESTINATION_VAR, replyDestination)
        .withPayload(FIRST_MESSAGE).run();

    InternalMessage replyMessage = requestReply.get();

    assertThat(replyMessage, not(nullValue()));
    assertThat(replyMessage.getPayload(), not(nullValue()));
    assertThat(replyMessage.getPayload().getValue(), is(equalTo(FIRST_MESSAGE)));
    assertThat(replyMessage.getAttributes(), not(nullValue()));

    executor.shutdown();
  }

}
