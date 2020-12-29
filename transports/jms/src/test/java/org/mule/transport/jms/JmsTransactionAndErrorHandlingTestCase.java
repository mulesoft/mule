/*
 * (c) 2003-2019 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms;

import org.junit.Before;
import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JmsTransactionAndErrorHandlingTestCase extends FunctionalTestCase {

  public static final int SHORT_TIMEOUT = 1000;
  public static final int MID_TIMEOUT = 2000;
  public static final int LONG_TIMEOUT = 35000;
  private MuleClient client;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"jms-exception-test.xml"};
  }

  @Override
  public int getTestTimeoutSecs() {
    return 3600;
  }

  @Before
  public void before() throws Exception {
    client =  new MuleClient(muleContext);
  }

  @Test
  public void testEverythingWorks() throws Exception {
    FlowTransformer.crash = false;
    MuleMessage message = new DefaultMuleMessage("Hello", muleContext);
    client.dispatch("vm://in", message);
    MuleMessage result = client.request("vm://out", MID_TIMEOUT);
    assertNull(client.request("vm://error", 1000));
    String payload = result.getPayloadAsString();
    assertEquals("Hello, world!", payload);
  }

  @Test
  public void testEverythingWorksSimple() throws Exception {
    FlowTransformer.crash = false;
    MuleMessage message = new DefaultMuleMessage("Hello", muleContext);
    client.dispatch("vm://in-simple", message);
    MuleMessage result = client.request("vm://out-simple", MID_TIMEOUT);
    assertNull(client.request("vm://error", LONG_TIMEOUT));
    String payload = result.getPayloadAsString();
    assertEquals("Hello, world!", payload);
  }

  @Test
  public void testErrorInUseCase() throws Exception {
    FlowTransformer.crash = true;
    ErrorTransformer.crash = false;
    MuleMessage message = new DefaultMuleMessage("Hello", muleContext);
    client.dispatch("vm://in", message);
    assertNull(client.request("vm://out", MID_TIMEOUT));
    MuleMessage result = client.request("vm://error", SHORT_TIMEOUT);
    String payload = result.getPayloadAsString();
    assertEquals("An error occurred!", payload);
  }

  /**
   * There should be one redelivery attempt, then the original message should
   * be routed to the ActiveMQ dead letter queue.
   */
  @Test
  public void testErrorInUseCaseSimple() throws Exception {
    FlowTransformer.crash = true;
    MuleMessage message = new DefaultMuleMessage("Hello", muleContext);
    client.dispatch("vm://in-simple", message);
    MuleMessage dl = client.request("vm://dead-letter", LONG_TIMEOUT);
    assertNotNull(dl);
    String payload = dl.getPayloadAsString();
    assertEquals("Hello", payload);
    assertNull(client.request("vm://out-simple", MID_TIMEOUT));
  }

  /**
   * There should be one redelivery attempt, then the original message should
   * be routed to the ActiveMQ dead letter queue.
   */
  @Test
  public void testErrorInUseCaseAndInExceptionHandler() throws Exception {
    FlowTransformer.crash = true;
    ErrorTransformer.crash = true;
    MuleMessage message = new DefaultMuleMessage("Hello", muleContext);
    client.dispatch("vm://in", message);
    MuleMessage dl = client.request("vm://dead-letter", LONG_TIMEOUT);

    assertNotNull(dl); // <---- here it fails!

    String payload = dl.getPayloadAsString();
    assertEquals("Hello", payload);
    assertNull(client.request("vm://out", SHORT_TIMEOUT));
  }

  /**
   * There should be one redelivery attempt, then the original message should
   * be routed to the ActiveMQ dead letter queue.
   */
  @Test
  public void testErrorInUseCaseThroughFlowrefAndInExceptionHandler() throws Exception {
    FlowTransformer.crash = true;
    ErrorTransformer.crash = true;
    MuleMessage message = new DefaultMuleMessage("Hello", muleContext);
    client.dispatch("vm://in-flowref", message);
    MuleMessage dl = client.request("vm://dead-letter", LONG_TIMEOUT);

    assertNotNull(dl); // <---- here it fails!

    String payload = dl.getPayloadAsString();
    assertEquals("Hello", payload);
    assertNull(client.request("vm://out", SHORT_TIMEOUT));
  }

}
