/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.processor.MessageProcessor;
import org.mule.exception.AbstractMessagingExceptionStrategy;
import org.mule.message.ExceptionMessage;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;
import org.mule.transport.email.FixedPortGreenMailSupport;
import org.mule.transport.email.functional.AbstractEmailFunctionalTestCase;
import org.mule.util.concurrent.Latch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.mail.internet.MimeMessage;

import org.hamcrest.core.IsNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class ExceptionStrategyCommonScenariosTestCase extends AbstractServiceAndFlowTestCase
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

    public ExceptionStrategyCommonScenariosTestCase(AbstractServiceAndFlowTestCase.ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{AbstractServiceAndFlowTestCase.ConfigVariant.SERVICE, "org/mule/test/integration/exceptions/exception-strategy-common-scenarios-service.xml"},
                {AbstractServiceAndFlowTestCase.ConfigVariant.FLOW, "org/mule/test/integration/exceptions/exception-strategy-common-scenarios-flow.xml"}});
    }

    @Test
    public void testRoutePayloadBeforeException() throws Exception
    {
        final Latch messageProcessedLatch = new Latch();
        FunctionalTestComponent ftc = getFunctionalTestComponent("LastMessageStateRouting");
        ftc.setEventCallback(new EventCallback()
        {
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                messageProcessedLatch.release();
                throw new RuntimeException();
            }
        });
        LocalMuleClient client = muleContext.getClient();
        client.dispatch("jms://in1?connector=jmsConnector", MESSAGE_TO_SEND, null);
        if (!messageProcessedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS))
        {
            fail("Message never received by mule");
        }
        MuleMessage response = client.request("jms://dead.letter1?connector=jmsConnector", TIMEOUT);
        assertThat(response, IsNull.<Object>notNullValue());
        assertThat(response.getPayloadAsString(), is(MESSAGE_MODIFIED));
    }

    @Test
    public void testRouteOriginalPayload() throws Exception
    {
        final Latch messageProcessedLatch = new Latch();
        FunctionalTestComponent ftc = getFunctionalTestComponent("OriginalMessageRouting");
        ftc.setEventCallback(new EventCallback()
        {
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                messageProcessedLatch.release();
                throw new RuntimeException();
            }
        });
        LocalMuleClient client = muleContext.getClient();
        client.dispatch("jms://in2?connector=jmsConnector", MESSAGE_TO_SEND, null);
        if (!messageProcessedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS))
        {
            fail("Message never received by mule");
        }
        MuleMessage response = client.request("jms://dead.letter2?connector=jmsConnector", TIMEOUT);
        assertThat(response, IsNull.<Object>notNullValue());
        assertThat(response.getPayloadAsString(), is(MESSAGE_TO_SEND));
    }

    @Test
    public void testRouteByExceptionType() throws Exception
    {
        final CountDownLatch messageProcessedLatch = new CountDownLatch(3);
        FunctionalTestComponent ftc = getFunctionalTestComponent("RouteByExceptionType");
        ftc.setEventCallback(new EventCallback()
        {
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                messageProcessedLatch.countDown();
                throw new RuntimeException();
            }
        });
        LocalMuleClient client = muleContext.getClient();
        client.dispatch("jms://in3?connector=jmsConnector", MESSAGE_TO_SEND, null);
        if (!messageProcessedLatch.await(TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS))
        {
            fail("Message never received by mule");
        }
        MuleMessage response = client.request("jms://dead.letter3?connector=jmsConnector", TIMEOUT);
        assertThat(response, IsNull.<Object>notNullValue());
        assertThat(response.getPayloadAsString(), is(MESSAGE_TO_SEND));
        assertThat(client.request("jms://exceptions?connector=jmsConnector", TIMEOUT), IsNull.<Object>notNullValue());
        assertThat(client.request("jms://exceptions?connector=jmsConnector", TIMEOUT), IsNull.notNullValue());
        MuleMessage exceptionResponse = client.request("jms://exceptions?connector=jmsConnector", TIMEOUT);
        assertThat(exceptionResponse, IsNull.<Object>notNullValue());
        assertThat(exceptionResponse.getPayload(), instanceOf(ExceptionMessage.class));
    }

    @Test
    public void testPreservePayloadExceptionStrategy() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://in4", MESSAGE_TO_SEND, null, TIMEOUT);
        assertThat(response, IsNull.<Object>notNullValue());
        assertThat(response.getPayloadAsString(), is(MESSAGE_MODIFIED));
    }


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

    @Test
    public void testRollbackTransactionAndSendAnEmail() throws Exception
    {
        if (variant.equals(ConfigVariant.SERVICE))
        {
            //((Lifecycle)getFlowConstruct("RollbackTransactionAndSendEmail")).stop(); is not working as expected
            return;
        }
        FixedPortGreenMailSupport greenMailSupport = new FixedPortGreenMailSupport(dynamicPort2.getNumber());

        List<Integer> ports = new ArrayList<Integer>(6);
        ports.add(dynamicPort1.getNumber());
        ports.add(dynamicPort2.getNumber());
        ports.add(dynamicPort3.getNumber());
        ports.add(dynamicPort4.getNumber());
        ports.add(dynamicPort5.getNumber());
        ports.add(dynamicPort6.getNumber());

        greenMailSupport.startServers(ports);
        LocalMuleClient client = muleContext.getClient();
        client.dispatch("jms://in6?connector=jmsConnectorNoRedelivery", MESSAGE_TO_SEND, null);
        endMessageProcessorExecuted.await(TIMEOUT, TimeUnit.MILLISECONDS);
        ((Lifecycle) getFlowConstruct("RollbackTransactionAndSendEmail")).stop();
        MuleMessage response = client.request("jms://in6?connector=jmsConnectorNoRedelivery", TIMEOUT);
        assertThat(response, IsNull.<Object>notNullValue());
        assertThat(response.getPayloadAsString(), is(MESSAGE_TO_SEND));
        greenMailSupport.getServers().waitForIncomingEmail(AbstractEmailFunctionalTestCase.DELIVERY_DELAY_MS, 1);
        MimeMessage[] messages = greenMailSupport.getServers().getReceivedMessages();
        assertNotNull("did not receive any messages", messages);
        assertEquals("did not receive 1 mail", 1, messages.length);
    }

    public static class EndMessageProcessor implements MessageProcessor
    {

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            endMessageProcessorExecuted.release();
            return event;
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
