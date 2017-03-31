/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.functional.exceptions.FunctionalTestException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;
import org.mule.runtime.core.message.ExceptionMessage;
import org.mule.runtime.core.routing.outbound.MulticastingRouter;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class DefaultServiceExceptionStrategyTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/default-service-exception-strategy-config-flow.xml";
  }

  @Test
  public void testDefaultExceptionStrategySingleEndpoint() throws Exception {
    FlowConstruct flowConstruct = muleContext.getRegistry().lookupFlowConstruct("testService1");

    assertNotNull(flowConstruct);
    assertNotNull(flowConstruct.getExceptionListener());
    assertTrue(flowConstruct.getExceptionListener() instanceof DefaultMessagingExceptionStrategy);
    assertEquals(1, ((DefaultMessagingExceptionStrategy) flowConstruct.getExceptionListener()).getMessageProcessors().size());

    flowRunner("testService1").withPayload(TEST_PAYLOAD).dispatch();

    assertExceptionMessage(muleContext.getClient().request("test://out1", RECEIVE_TIMEOUT).getRight().get());
    // request one more time to ensure that only one exception message was sent
    // per exception
    assertThat(muleContext.getClient().request("test://out2", RECEIVE_TIMEOUT).getRight().isPresent(), is(false));
  }

  @Test
  public void testDefaultExceptionStrategyMultipleEndpoints() throws Exception {
    FlowConstruct flowConstruct = muleContext.getRegistry().lookupFlowConstruct("testService2");

    assertNotNull(flowConstruct);
    assertNotNull(flowConstruct.getExceptionListener());
    assertTrue(flowConstruct.getExceptionListener() instanceof DefaultMessagingExceptionStrategy);
    DefaultMessagingExceptionStrategy exceptionListener =
        (DefaultMessagingExceptionStrategy) flowConstruct.getExceptionListener();
    Processor mp = exceptionListener.getMessageProcessors().iterator().next();
    assertTrue(mp.getClass().getName(), mp instanceof MulticastingRouter);
    assertEquals(2, ((MulticastingRouter) mp).getRoutes().size());

    MuleClient client = muleContext.getClient();

    flowRunner("testService2").withPayload(TEST_PAYLOAD).dispatch();

    Message out2 = client.request("test://out2", RECEIVE_TIMEOUT).getRight().get();
    Message out3 = client.request("test://out3", RECEIVE_TIMEOUT).getRight().get();
    assertExceptionMessage(out2);
    assertExceptionMessage(out3);
    assertThat(out2, equalTo(out3));
  }

  @Test
  public void testDefaultExceptionStrategyNonEndpoint() throws Exception {
    MuleClient mc = muleContext.getClient();

    flowRunner("testService3").withPayload(TEST_PAYLOAD).dispatch();

    Message out4 = mc.request("test://out4", RECEIVE_TIMEOUT).getRight().get();
    assertEquals("ERROR!", getPayloadAsString(out4));
  }

  @Test
  public void testSerializablePayload() throws Exception {
    Map<String, String> map = new HashMap<>();
    map.put("key1", "value1");
    map.put("key2", "value2");

    MuleClient client = muleContext.getClient();
    flowRunner("testService6").withPayload(map).dispatch();

    Message message = client.request("test://out6", RECEIVE_TIMEOUT).getRight().get();

    assertTrue(message.getPayload().getValue() instanceof ExceptionMessage);
    Object payload = ((ExceptionMessage) message.getPayload().getValue()).getPayload();
    assertTrue("payload shoud be a Map, but is " + payload.getClass().getName(), payload instanceof Map<?, ?>);
    Map<?, ?> payloadMap = (Map<?, ?>) payload;
    assertEquals("value1", payloadMap.get("key1"));
    assertEquals("value2", payloadMap.get("key2"));
  }

  @Test
  public void testStopsServiceOnException() throws Exception {
    final FlowConstruct flowConstruct = muleContext.getRegistry().lookupFlowConstruct("testService5");

    MuleClient client = muleContext.getClient();
    flowRunner("testService5").withPayload(TEST_PAYLOAD).dispatch();

    assertExceptionMessage(client.request("test://out5", RECEIVE_TIMEOUT).getRight().get());

    Prober prober = new PollingProber(5000, 100);
    prober.check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return !flowConstruct.getLifecycleState().isStarted();
      }

      @Override
      public String describeFailure() {
        return "Service was not stopped after processing the exception";
      }
    });
  }

  private void assertExceptionMessage(Message out) {
    assertThat(out.getPayload().getValue(), is(instanceOf(ExceptionMessage.class)));
    ExceptionMessage exceptionMessage = (ExceptionMessage) out.getPayload().getValue();
    assertThat(exceptionMessage.getException().getCause().getCause(), is(instanceOf(FunctionalTestException.class)));
    assertThat(exceptionMessage.getPayload(), is("test"));
  }
}
