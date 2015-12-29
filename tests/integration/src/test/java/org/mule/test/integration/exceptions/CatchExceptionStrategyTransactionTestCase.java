/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MessagingException;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.transaction.Transaction;
import org.mule.functional.functional.EventCallback;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.functional.listener.ExceptionListener;
import org.mule.functional.listener.SystemExceptionListener;
import org.mule.transaction.TransactionCoordination;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class CatchExceptionStrategyTransactionTestCase extends FunctionalTestCase
{

    private static final int TIMEOUT = 5000;
    private static final int SHORT_TIMEOUT = 100;
    private static final String MESSAGE = "any message";
    private static final String SINGLE_TRANSACTION_BEHAVIOR_FLOW = "singleTransactionBehavior";
    private static final String XA_TRANSACTION_BEHAVIOR_FLOW = "xaTransactionBehavior";
    private static final String TRANSACTION_COMMIT_FAILS_FLOW = "transactionCommitFails";
    private static final String IN_2_VM_ENDPOINT = "vm://vmIn2";
    private static final String IN_3_VM_ENDPOINT = "vm://in3";
    private static final String IN_1_JMS_ENDPOINT = "jms://in1?connector=activeMq";
    private static final String IN_2_JMS_ENDPOINT = "jms://in2?connector=activeMq";
    private static final String OUT_2_JMS_ENDPOINT = "jms://out2?connector=activeMq";

    private Transaction mockTransaction = mock(Transaction.class);
    
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/exceptions/catch-exception-strategy-transaction-flow.xml";
    }

    @Test
    public void singleTransactionIsCommittedOnFailure() throws Exception
    {
        getFunctionalTestComponent(SINGLE_TRANSACTION_BEHAVIOR_FLOW).setEventCallback(getFailureCallback());
        LocalMuleClient client = muleContext.getClient();
        ExceptionListener exceptionListener = new ExceptionListener(muleContext);
        exceptionListener.setTimeoutInMillis(TIMEOUT);
        client.dispatch(IN_1_JMS_ENDPOINT, MESSAGE, null);
        exceptionListener.waitUntilAllNotificationsAreReceived();
        stopFlowConstruct(SINGLE_TRANSACTION_BEHAVIOR_FLOW);
        MuleMessage request = client.request(IN_1_JMS_ENDPOINT, SHORT_TIMEOUT);
        assertThat(request, nullValue());
    }

    @Test
    public void singleTransactionIsCommittedOnFailureButCommitFails() throws Exception
    {
        getFunctionalTestComponent(SINGLE_TRANSACTION_BEHAVIOR_FLOW).setEventCallback(replaceTransactionWithMockAndFailComponent());
        LocalMuleClient client = muleContext.getClient();
        ExceptionListener exceptionListener = new ExceptionListener(muleContext);
        exceptionListener.setTimeoutInMillis(TIMEOUT);
        SystemExceptionListener systemExceptionListener = new SystemExceptionListener(muleContext).setTimeoutInMillis(TIMEOUT);
        client.dispatch(IN_1_JMS_ENDPOINT, MESSAGE, null);
        exceptionListener.waitUntilAllNotificationsAreReceived();
        stopFlowConstruct(SINGLE_TRANSACTION_BEHAVIOR_FLOW);
        systemExceptionListener.waitUntilAllNotificationsAreReceived();
        MuleMessage request = client.request(IN_1_JMS_ENDPOINT, SHORT_TIMEOUT);
        assertThat(request, notNullValue());
    }

    @Test
    public void xaTransactionIsCommittedOnFailure() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        ExceptionListener exceptionListener = new ExceptionListener(muleContext);
        exceptionListener.setTimeoutInMillis(TIMEOUT);
        client.dispatch(IN_2_JMS_ENDPOINT, MESSAGE, null);
        exceptionListener.waitUntilAllNotificationsAreReceived();
        stopFlowConstruct(XA_TRANSACTION_BEHAVIOR_FLOW);
        MuleMessage outMessage = client.request(OUT_2_JMS_ENDPOINT, TIMEOUT);
        assertThat(outMessage, notNullValue());
        assertThat(getPayloadAsString(outMessage), is(MESSAGE));
        MuleMessage inMessage = client.request(IN_2_JMS_ENDPOINT, SHORT_TIMEOUT);
        assertThat(inMessage, nullValue());
        MuleMessage inVmMessage = client.request(IN_2_VM_ENDPOINT, TIMEOUT);
        assertThat(inVmMessage, notNullValue());
        assertThat(getPayloadAsString(inVmMessage), is(MESSAGE));
    }

    @Test
    public void transactionCommitFailureTriggersExceptionStrategy() throws Exception
    {
        transactionCommitFailureExecutesExceptionStrategy(getTestMuleMessage());
    }

    @Test
    public void transactionCommitFailureTriggersExceptionStrategyUsingFilter() throws Exception
    {
        MuleMessage muleMessage = getTestMuleMessage();
        muleMessage.setOutboundProperty("filterMessage", true);
        transactionCommitFailureExecutesExceptionStrategy(muleMessage);
    }

    private void transactionCommitFailureExecutesExceptionStrategy(MuleMessage muleMessage) throws Exception
    {
        getFunctionalTestComponent(TRANSACTION_COMMIT_FAILS_FLOW).setEventCallback(replaceTransactionWithMock());
        ExceptionListener exceptionListener = new ExceptionListener(muleContext);
        muleContext.getClient().dispatch(IN_3_VM_ENDPOINT, muleMessage);
        exceptionListener.waitUntilAllNotificationsAreReceived();
        stopFlowConstruct(TRANSACTION_COMMIT_FAILS_FLOW);
        exceptionListener.assertExpectedException(MessagingException.class);
    }

    @Test
    public void transactionCommitFailureWithinCatchExceptionStrategy() throws Exception
    {
        SystemExceptionListener systemExceptionListener = new SystemExceptionListener(muleContext);
        getFunctionalTestComponent(TRANSACTION_COMMIT_FAILS_FLOW).setEventCallback(replaceTransactionWithMockAndFailComponent());
        ExceptionListener exceptionListener = new ExceptionListener(muleContext);
        muleContext.getClient().dispatch(IN_3_VM_ENDPOINT, getTestMuleMessage());
        exceptionListener.waitUntilAllNotificationsAreReceived();
        stopFlowConstruct(TRANSACTION_COMMIT_FAILS_FLOW);
        systemExceptionListener.waitUntilAllNotificationsAreReceived();
    }

    private EventCallback replaceTransactionWithMock(final EventCallback processEventCallback) throws Exception
    {
        when(mockTransaction.supports(anyObject(), anyObject())).thenReturn(true);
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                TransactionCoordination.getInstance().unbindTransaction(mockTransaction);
                throw new RuntimeException();
            }
        }).when(mockTransaction).commit();
        return new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                context.getCurrentTransaction().rollback();
                TransactionCoordination.getInstance().bindTransaction(mockTransaction);
                processEventCallback.eventReceived(context, component);
            }
        };
    }

    private EventCallback replaceTransactionWithMock() throws Exception
    {
        return replaceTransactionWithMock(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                //Do nothing
            }
        });
    }

    private EventCallback replaceTransactionWithMockAndFailComponent() throws Exception
    {
        return replaceTransactionWithMock(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                throw new RuntimeException();
            }
        });
    }

    private EventCallback getFailureCallback()
    {
        return new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                throw new RuntimeException();
            }
        };
    }

}
