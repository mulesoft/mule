/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.processor.MessageProcessor;
import org.mule.exception.AbstractMessagingExceptionStrategy;
import org.mule.message.ExceptionMessage;
import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hamcrest.core.IsNull;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class ExceptionStrategyCommonScenariosTestCase extends FunctionalTestCase
{
    public static final String MESSAGE_TO_SEND = "A message";
    public static final String MESSAGE_MODIFIED = "A message with some text added";
    public static final int TIMEOUT = 5000;
    private final static Latch endMessageProcessorExecuted = new Latch();
    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Rule
    public DynamicPort dynamicPort3 = new DynamicPort("port3");

    @Rule
    public DynamicPort dynamicPort4 = new DynamicPort("port4");

    @Rule
    public DynamicPort dynamicPort5 = new DynamicPort("port5");

    @Rule
    public DynamicPort dynamicPort6 = new DynamicPort("port6");


    @Rule
    public DynamicPort dynamicPort7 = new DynamicPort("port7");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/exceptions/exception-strategy-common-scenarios-flow.xml";
    }

    @Test
    public void testPreservePayloadExceptionStrategy() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://in4", MESSAGE_TO_SEND, null, TIMEOUT);
        assertThat(response, IsNull.<Object>notNullValue());
        assertThat(getPayloadAsString(response), is(MESSAGE_MODIFIED));
    }


    @Ignore("See MULE-9201")
    @Test(expected = org.mule.api.transport.NoReceiverForEndpointException.class)
    public void testStopFlowBasedOnExceptionType() throws Throwable
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://in5", MESSAGE_TO_SEND, null, TIMEOUT);
        assertThat((NullPayload) response.getPayload(), is(NullPayload.getInstance()));
        assertThat(response.getExceptionPayload(), IsNull.<Object>notNullValue());
        try
        {
            client.send("vm://in5", MESSAGE_TO_SEND, null, TIMEOUT);
        }
        catch (Exception e)
        {
            throw e.getCause();
        }
    }

    public static class PreservePayloadExceptionStrategy extends AbstractMessagingExceptionStrategy
    {
        public PreservePayloadExceptionStrategy(MuleContext muleContext)
        {
            super(muleContext);
        }

        @Override
        public MuleEvent handleException(Exception e, MuleEvent event)
        {
            Object payloadBeforeException = event.getMessage().getPayload();
            MuleEvent resultEvent = super.handleException(e, event);
            resultEvent.getMessage().setPayload(payloadBeforeException);
            return resultEvent;
        }
    }
}
