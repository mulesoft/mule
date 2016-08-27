/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.vm.functional.transactions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.runtime.core.message.ExceptionMessage;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.util.concurrent.Latch;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

public class VmExceptionStrategyOneWayTestCase extends FunctionalTestCase {

  public static final int TIMEOUT = 3000;
  public static final String ORIGINAL_MESSAGE = "some message";
  private static Latch outboundComponentLatch;
  private static Latch deadLetterQueueLatch;
  private static boolean outboundComponentReached;

  @Override
  protected String getConfigFile() {
    return "vm/vm-exception-strategy-config-one-way.xml";
  }

  @Before
  public void setUp() throws Exception {
    deadLetterQueueLatch = new Latch();
    outboundComponentLatch = new Latch();
    outboundComponentReached = false;
  }

  @Test
  public void testDeadLetterQueueWithInboundEndpointException() throws Exception {
    MuleClient muleClient = muleContext.getClient();
    MuleMessage response = muleClient.send("vm://in1", ORIGINAL_MESSAGE, null).getRight();
    if (!deadLetterQueueLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("dead letter queue must be reached");
    }
    assertThat(outboundComponentReached, is(false));
    assertThat(response, notNullValue());
    assertThat(response.getPayload(), is(nullValue()));
    assertThat(response.getExceptionPayload(), notNullValue());
    assertThat(response.getExceptionPayload(), instanceOf(DefaultExceptionPayload.class));
  }

  @Test
  public void testDeadLetterQueueWithInboundEndpointResponseException() throws Exception {
    MuleClient muleClient = muleContext.getClient();
    MuleMessage response = muleClient.send("vm://in2", ORIGINAL_MESSAGE, null).getRight();
    // TODO PLG - ES - fix this, DLQ call fails since tx was resolved already
    /*
     * if (!deadLetterQueueLatch.await(TIMEOUT, MILLISECONDS)) { fail("dead letter queue must be reached"); }
     */
    assertThat(response, notNullValue());
    assertThat(response.getPayload(), is(nullValue()));
    assertThat(response.getExceptionPayload(), notNullValue());
    assertThat(response.getExceptionPayload(), instanceOf(DefaultExceptionPayload.class));
    if (!outboundComponentLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("outbound component not reached");
    }
  }

  @Test
  public void testDeadLetterQueueWithComponentException() throws Exception {
    MuleClient muleClient = muleContext.getClient();
    MuleMessage response = muleClient.send("vm://in3", ORIGINAL_MESSAGE, null).getRight();
    if (!deadLetterQueueLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("dead letter queue must be reached");
    }
    assertThat(outboundComponentReached, is(false));
    assertThat(response, notNullValue());
    assertThat(response.getPayload(), is(nullValue()));
    assertThat(response.getExceptionPayload(), notNullValue());
    assertThat(response.getExceptionPayload(), instanceOf(DefaultExceptionPayload.class));
  }

  @Test
  public void testDeadLetterQueueWithOutboundEndpointException() throws Exception {
    MuleClient muleClient = muleContext.getClient();
    MuleMessage response = muleClient.send("vm://in4", ORIGINAL_MESSAGE, null).getRight();
    if (!deadLetterQueueLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("dead letter queue must be reached");
    }
    assertThat(outboundComponentReached, is(false));
    assertThat(response, notNullValue());
    assertThat(response.getPayload(), is(nullValue()));
    assertThat(response.getExceptionPayload(), notNullValue());
    assertThat(response.getExceptionPayload(), instanceOf(DefaultExceptionPayload.class));
  }

  public static class FailingTransformer extends AbstractTransformer {

    @Override
    protected Object doTransform(Object src, Charset enc) throws TransformerException {
      throw new TransformerException(CoreMessages.failedToBuildMessage(), this);
    }
  }

  public static class DeadLetterQueueComponent implements Callable {

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
      deadLetterQueueLatch.release();
      MuleMessage message = eventContext.getMessage();
      assertThat(message, notNullValue());
      assertThat(message.getExceptionPayload(), nullValue());
      assertThat(message.getPayload(), instanceOf(ExceptionMessage.class));
      return eventContext.getMessage();
    }
  }

  public static class OutboundComponent implements Callable {

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
      outboundComponentLatch.release();
      outboundComponentReached = true;
      return eventContext.getMessage();
    }
  }
}
