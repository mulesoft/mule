/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.connectivity;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.db.integration.TestDbConfig.getDerbyResource;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.api.message.Message;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class DataSourcePoolingTestCase extends AbstractDbIntegrationTestCase {

  private static final int TIMEOUT = 10;
  private static final TimeUnit TIMEOUT_UNIT = SECONDS;
  private static CountDownLatch connectionLatch;

  @Parameterized.Parameters(name = "{2}")
  public static List<Object[]> parameters() {
    return getDerbyResource();
  }

  @Before
  public void setUp() throws Exception {
    setConcurrentRequests(2);
  }

  private void setConcurrentRequests(int count) {
    connectionLatch = new CountDownLatch(count);
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/config/derby-pooling-db-config.xml",
        "integration/connectivity/connection-pooling-config.xml"};
  }

  @Test
  public void providesMultipleConnections() throws Exception {
    assertThat(countSuccesses(request(2)), is(2));
  }

  @Test
  public void connectionsGoBackToThePool() throws Exception {
    providesMultipleConnections();
    providesMultipleConnections();
  }

  @Test
  public void limitsConnections() throws Exception {
    setConcurrentRequests(3);
    Message[] responses = request(3);
    assertThat(countSuccesses(responses), is(2));
    assertThat(countFailures(responses), is(1));
  }

  private Message[] request(int times) throws Exception {
    Thread[] requests = new Thread[times];
    Message[] responses = new Message[times];

    range(0, times).forEach(i -> {
      requests[i] = new Thread(() -> doRequest(responses, i));
      requests[i].start();
    });

    for (int i = 0; i < times; i++) {
      requests[i].join();
    }

    return responses;
  }

  private void doRequest(Message[] responses, int index) {
    try {
      responses[index] = flowRunner("queryAndJoin").run().getMessage();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private int countSuccesses(Message... messages) {
    return count(message -> message.getPayload().getValue().equals("OK"), messages);
  }

  private int countFailures(Message... messages) {
    return count(message -> message.getPayload().getValue().equals("FAIL"), messages);
  }

  private int count(Predicate<Message> predicate, Message... messages) {
    return new Long(Stream.of(messages).filter(predicate).count()).intValue();
  }

  public static class JoinRequests {

    public static Object process(Object payload) {
      connectionLatch.countDown();

      try {
        connectionLatch.await(TIMEOUT, TIMEOUT_UNIT);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      return payload;
    }
  }
}
