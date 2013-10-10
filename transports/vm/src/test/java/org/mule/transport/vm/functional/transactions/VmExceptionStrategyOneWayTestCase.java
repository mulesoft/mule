/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm.functional.transactions;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.message.DefaultExceptionPayload;
import org.mule.message.ExceptionMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transformer.AbstractTransformer;
import org.mule.transport.NullPayload;
import org.mule.util.concurrent.Latch;

import static edu.emory.mathcs.backport.java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class VmExceptionStrategyOneWayTestCase extends FunctionalTestCase
{

    public static final int TIMEOUT = 3000;
    public static final int TINY_TIMEOUT = 300;
    public static final String ORIGINAL_MESSAGE = "some message";
    private static Latch outboundComponentLatch;
    private static Latch deadLetterQueueLatch;
    private static boolean outboundComponentReached;

    @Override
    protected String getConfigResources()
    {
        return "vm/vm-exception-strategy-config-one-way.xml";
    }

    @Before
    public void setUp() throws Exception
    {
        deadLetterQueueLatch = new Latch();
        outboundComponentLatch = new Latch();
        outboundComponentReached = false;
    }

    @Test
    public void testDeadLetterQueueWithInboundEndpointException() throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);
        MuleMessage response = muleClient.send("vm://in1", ORIGINAL_MESSAGE, null);
        if (!deadLetterQueueLatch.await(TIMEOUT, MILLISECONDS)) {
            fail("dead letter queue must be reached");
        }
        assertThat(outboundComponentReached, Is.is(false));
        assertThat(response, IsNull.<Object>notNullValue());
        assertThat(response.getPayload(),IsInstanceOf.instanceOf(NullPayload.class));
        assertThat(response.getExceptionPayload(), IsNull.<Object>notNullValue());
        assertThat(response.getExceptionPayload(), IsInstanceOf.instanceOf(DefaultExceptionPayload.class));
    }

    @Test
    public void testDeadLetterQueueWithInboundEndpointResponseException() throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);
        MuleMessage response = muleClient.send("vm://in2", ORIGINAL_MESSAGE, null);
        //TODO PLG - ES - fix this, DLQ call fails since tx was resolved already
        /*if (!deadLetterQueueLatch.await(TIMEOUT, MILLISECONDS)) {
            fail("dead letter queue must be reached");
        }*/
        assertThat(response, IsNull.<Object>notNullValue());
        assertThat(response.getPayload(),IsInstanceOf.instanceOf(NullPayload.class));
        assertThat(response.getExceptionPayload(), IsNull.<Object>notNullValue());
        assertThat(response.getExceptionPayload(), IsInstanceOf.instanceOf(DefaultExceptionPayload.class));
        if (!outboundComponentLatch.await(TINY_TIMEOUT, MILLISECONDS))
        {
            fail("outbound component not reached");
        }
    }

    @Test
    public void testDeadLetterQueueWithComponentException() throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);
        MuleMessage response = muleClient.send("vm://in3", ORIGINAL_MESSAGE, null);
        if (!deadLetterQueueLatch.await(TIMEOUT, MILLISECONDS)) {
            fail("dead letter queue must be reached");
        }
        assertThat(outboundComponentReached, Is.is(false));
        assertThat(response, IsNull.<Object>notNullValue());
        assertThat(response.getPayload(),IsInstanceOf.instanceOf(NullPayload.class));
        assertThat(response.getExceptionPayload(), IsNull.<Object>notNullValue());
        assertThat(response.getExceptionPayload(), IsInstanceOf.instanceOf(DefaultExceptionPayload.class));
    }

    @Test
    public void testDeadLetterQueueWithOutboundEndpointException() throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);
        MuleMessage response = muleClient.send("vm://in4", ORIGINAL_MESSAGE, null);
        if (!deadLetterQueueLatch.await(TIMEOUT, MILLISECONDS)) {
            fail("dead letter queue must be reached");
        }
        assertThat(outboundComponentReached, Is.is(false));
        assertThat(response, IsNull.<Object>notNullValue());
        assertThat(response.getPayload(),IsInstanceOf.instanceOf(NullPayload.class));
        assertThat(response.getExceptionPayload(), IsNull.<Object>notNullValue());
        assertThat(response.getExceptionPayload(), IsInstanceOf.instanceOf(DefaultExceptionPayload.class));
    }

    public static class FailingTransformer extends AbstractTransformer
    {

        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            throw new TransformerException(CoreMessages.failedToBuildMessage(), this);
        }
    }

    public static class DeadLetterQueueComponent implements Callable
    {

        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            deadLetterQueueLatch.release();
            MuleMessage message = eventContext.getMessage();
            assertThat(message, IsNull.<Object>notNullValue());
            assertThat(message.getExceptionPayload(), IsNull.<Object>nullValue());
            assertThat(message.getPayload(), IsInstanceOf.instanceOf(ExceptionMessage.class));
            return eventContext.getMessage();
        }
    }

    public static class OutboundComponent implements Callable
    {

        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            outboundComponentLatch.release();
            outboundComponentReached = true;
            return eventContext.getMessage();
        }
    }
}
