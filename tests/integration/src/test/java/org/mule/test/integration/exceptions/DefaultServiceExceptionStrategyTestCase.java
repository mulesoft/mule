/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.functional.exceptions.FunctionalTestException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.MessageProcessor;
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

    flowRunner("testService1").withPayload(getTestMuleMessage()).asynchronously().run();

    assertExceptionMessage(muleContext.getClient().request("test://out1", RECEIVE_TIMEOUT));
    // request one more time to ensure that only one exception message was sent
    // per exception
    assertNull(muleContext.getClient().request("test://out2", RECEIVE_TIMEOUT));
  }

  @Test
  public void testDefaultExceptionStrategyMultipleEndpoints() throws Exception {
    FlowConstruct flowConstruct = muleContext.getRegistry().lookupFlowConstruct("testService2");

    assertNotNull(flowConstruct);
    assertNotNull(flowConstruct.getExceptionListener());
    assertTrue(flowConstruct.getExceptionListener() instanceof DefaultMessagingExceptionStrategy);
    DefaultMessagingExceptionStrategy exceptionListener =
        (DefaultMessagingExceptionStrategy) flowConstruct.getExceptionListener();
    MessageProcessor mp = exceptionListener.getMessageProcessors().iterator().next();
    assertTrue(mp.getClass().getName(), mp instanceof MulticastingRouter);
    assertEquals(2, ((MulticastingRouter) mp).getRoutes().size());

    MuleClient client = muleContext.getClient();

    flowRunner("testService2").withPayload(getTestMuleMessage()).asynchronously().run();

    MuleMessage out2 = client.request("test://out2", RECEIVE_TIMEOUT);
    MuleMessage out3 = client.request("test://out3", RECEIVE_TIMEOUT);
    assertExceptionMessage(out2);
    assertExceptionMessage(out3);
    assertNotSame(out2, out3);
    assertThat(out3.getPayload(), equalTo(out2.getPayload()));
  }

  @Test
  public void testDefaultExceptionStrategyNonEndpoint() throws Exception {
    MuleClient mc = muleContext.getClient();

    flowRunner("testService3").withPayload(getTestMuleMessage()).asynchronously().run();

    MuleMessage out4 = mc.request("test://out4", RECEIVE_TIMEOUT);
    assertEquals("ERROR!", getPayloadAsString(out4));
  }

  @Test
  public void testSerializablePayload() throws Exception {
    Map<String, String> map = new HashMap<String, String>();
    map.put("key1", "value1");
    map.put("key2", "value2");

    MuleClient client = muleContext.getClient();
    flowRunner("testService6").withPayload(getTestMuleMessage(map)).asynchronously().run();

    MuleMessage message = client.request("test://out6", RECEIVE_TIMEOUT);

    assertTrue(message.getPayload() instanceof ExceptionMessage);
    Object payload = ((ExceptionMessage) message.getPayload()).getPayload();
    assertTrue("payload shoud be a Map, but is " + payload.getClass().getName(), payload instanceof Map<?, ?>);
    Map<?, ?> payloadMap = (Map<?, ?>) payload;
    assertEquals("value1", payloadMap.get("key1"));
    assertEquals("value2", payloadMap.get("key2"));
  }

  @Test
  public void testStopsServiceOnException() throws Exception {
    final FlowConstruct flowConstruct = muleContext.getRegistry().lookupFlowConstruct("testService5");

    MuleClient client = muleContext.getClient();
    flowRunner("testService5").withPayload(getTestMuleMessage()).asynchronously().run();

    assertExceptionMessage(client.request("test://out5", RECEIVE_TIMEOUT));

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

  private void assertExceptionMessage(MuleMessage out) {
    assertTrue(out.getPayload() instanceof ExceptionMessage);
    ExceptionMessage exceptionMessage = (ExceptionMessage) out.getPayload();
    assertEquals(FunctionalTestException.class, exceptionMessage.getException().getCause().getClass());
    assertEquals("test", exceptionMessage.getPayload());
  }
}
