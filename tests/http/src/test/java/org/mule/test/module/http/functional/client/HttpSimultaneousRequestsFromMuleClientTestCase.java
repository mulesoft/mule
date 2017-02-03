/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.client;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.service.http.api.HttpConstants.Method.POST;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.connector.ConnectorOperationLocator;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.module.http.functional.AbstractHttpTestCase;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class HttpSimultaneousRequestsFromMuleClientTestCase extends AbstractHttpTestCase {

  @Rule
  public DynamicPort listenPort = new DynamicPort("port");

  @Override
  protected String getConfigFile() {
    return "http-listener-for-client.xml";
  }

  private ExecutorService executor;
  private ConnectorOperationLocator connectorOperationLocator;

  @Before
  public void before() {
    executor = newFixedThreadPool(2);
    connectorOperationLocator = muleContext.getRegistry().get(OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR);
  }

  @After
  public void after() {
    executor.shutdownNow();
  }

  @Test
  public void simultaneousRequests() throws Exception {
    final Future<Event> result1 = executor.submit(new RequestThroughClient("/test1"));
    final Future<Event> result2 = executor.submit(new RequestThroughClient("/test2"));

    assertThat(result1.get(), not(nullValue()));
    assertThat(result2.get(), not(nullValue()));
  }

  private final class RequestThroughClient implements Callable<Event> {

    private final String path;

    private RequestThroughClient(String path) {
      this.path = path;
    }

    @Override
    public Event call() throws Exception {
      Processor messageProcessor =
          connectorOperationLocator.locateConnectorOperation("http://localhost:" + listenPort.getNumber() + path,
                                                             newOptions().method(POST.name()).build(), REQUEST_RESPONSE);
      initialiseIfNeeded(messageProcessor, muleContext, getTestFlow(muleContext));

      return messageProcessor.process(eventBuilder().message(InternalMessage.of(TEST_MESSAGE)).build());
    }
  }
}
