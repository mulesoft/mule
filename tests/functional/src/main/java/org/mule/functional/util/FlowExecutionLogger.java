/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.util;


import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static org.mule.tck.probe.PollingProber.check;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Saves the payload that goes through a processor for accessing it later
 */
public class FlowExecutionLogger extends AbstractComponent implements Processor {

  private static final int POLLING_TIMEOUT = 5000;
  private static final int POLLING_DELAY = 500;

  private static final String EXECUTION_ROUTE_KEY = "executionRoute";

  private static Map<String, ExecutionLog> executionLogsMap = new ConcurrentHashMap<>();

  public static void resetLogsMap() {
    executionLogsMap = new ConcurrentHashMap<>();
  }

  private static void waitUntilNthExecution(String routeKey, int n) {
    try {
      check(POLLING_TIMEOUT, POLLING_DELAY, () -> executionLogsMap.get(routeKey).getExecutionCount() >= n);
    } catch (AssertionError e) {
      fail(format("Route: %s not executed %d times", routeKey, n));
    }
  }

  public static void assertRouteNeverExecuted(String routeKey) {
    assertThat(executionLogsMap.get(routeKey), is(nullValue()));
  }

  public static void assertRouteNthExecution(String routeKey, int n, Object... values) {
    waitUntilNthExecution(routeKey, n);
    ExecutionLog executionLog = executionLogsMap.get(routeKey);
    Message message = executionLog.getCollectedMessages().get(n - 1);
    if (message.getPayload().getValue() instanceof List) {
      List<TypedValue> aggregatedElements = (List<TypedValue>) message.getPayload().getValue();
      assertThat(aggregatedElements.stream().map(element -> element.getValue()).collect(toList()), contains(values));
    } else {
      assertThat(values, arrayWithSize(1));
      assertThat(message.getPayload().getValue(), is(equalTo(values[0])));
    }
  }

  public static void assertRouteExecutedNTimes(String routeKey, int n) {
    try {
      check(POLLING_TIMEOUT, POLLING_DELAY, () -> executionLogsMap.containsKey(routeKey));
    } catch (AssertionError e) {
      fail(format("%s has never been executed", routeKey));
    }
    try {
      check(POLLING_TIMEOUT, POLLING_DELAY, () -> executionLogsMap.get(routeKey).getExecutionCount() == n);
    } catch (AssertionError e) {
      fail(format("The number of executions of route: %s is not the expected one, got: %d, expected: %d", routeKey,
                  executionLogsMap.get(routeKey).getExecutionCount(), n));
    }
  }

  @Override
  public CoreEvent process(CoreEvent coreEvent) throws MuleException {
    String routeName = coreEvent.getVariables().get(EXECUTION_ROUTE_KEY).getValue().toString();
    Message message = coreEvent.getMessage();
    if (executionLogsMap.containsKey(routeName)) {
      executionLogsMap.get(routeName).logExecution(message);
    } else {
      ExecutionLog newLog = new ExecutionLog(message);
      executionLogsMap.put(routeName, newLog);
    }
    return coreEvent;
  }

  private static class ExecutionLog {

    private List<Message> collectedMessages;

    public ExecutionLog(Message message) {
      this.collectedMessages = new ArrayList<>();
      logExecution(message);
    }

    public void logExecution(Message newMessage) {
      collectedMessages.add(newMessage);
    }

    public int getExecutionCount() {
      return collectedMessages.size();
    }

    public List<Message> getCollectedMessages() {
      return collectedMessages;
    }
  }

}
