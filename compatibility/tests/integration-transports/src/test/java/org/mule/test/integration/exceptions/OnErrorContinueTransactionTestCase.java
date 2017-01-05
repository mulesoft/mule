/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.functional.functional.EventCallback;
import org.mule.functional.listener.ExceptionListener;
import org.mule.functional.listener.SystemExceptionListener;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.transaction.TransactionCoordination;

import org.junit.Test;

public class OnErrorContinueTransactionTestCase extends CompatibilityFunctionalTestCase {

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
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/on-error-continue-transaction-flow.xml";
  }

  @Test
  public void singleTransactionIsCommittedOnFailure() throws Exception {
    getFunctionalTestComponent(SINGLE_TRANSACTION_BEHAVIOR_FLOW).setEventCallback(getFailureCallback());
    MuleClient client = muleContext.getClient();
    ExceptionListener exceptionListener = new ExceptionListener(muleContext);
    exceptionListener.setTimeoutInMillis(TIMEOUT);
    client.dispatch(IN_1_JMS_ENDPOINT, MESSAGE, null);
    exceptionListener.waitUntilAllNotificationsAreReceived();
    stopFlowConstruct(SINGLE_TRANSACTION_BEHAVIOR_FLOW);
    assertThat(client.request(IN_1_JMS_ENDPOINT, SHORT_TIMEOUT).getRight().isPresent(), is(false));
  }

  @Test
  public void singleTransactionIsCommittedOnFailureButCommitFails() throws Exception {
    getFunctionalTestComponent(SINGLE_TRANSACTION_BEHAVIOR_FLOW).setEventCallback(replaceTransactionWithMockAndFailComponent());
    MuleClient client = muleContext.getClient();
    ExceptionListener exceptionListener = new ExceptionListener(muleContext);
    exceptionListener.setTimeoutInMillis(TIMEOUT);
    SystemExceptionListener systemExceptionListener = new SystemExceptionListener(muleContext).setTimeoutInMillis(TIMEOUT);
    client.dispatch(IN_1_JMS_ENDPOINT, MESSAGE, null);
    exceptionListener.waitUntilAllNotificationsAreReceived();
    stopFlowConstruct(SINGLE_TRANSACTION_BEHAVIOR_FLOW);
    systemExceptionListener.waitUntilAllNotificationsAreReceived();
    InternalMessage request = client.request(IN_1_JMS_ENDPOINT, SHORT_TIMEOUT).getRight().get();
    assertThat(request, notNullValue());
  }

  @Test
  public void xaTransactionIsCommittedOnFailure() throws Exception {
    MuleClient client = muleContext.getClient();
    ExceptionListener exceptionListener = new ExceptionListener(muleContext);
    exceptionListener.setTimeoutInMillis(TIMEOUT);
    client.dispatch(IN_2_JMS_ENDPOINT, MESSAGE, null);
    exceptionListener.waitUntilAllNotificationsAreReceived();
    stopFlowConstruct(XA_TRANSACTION_BEHAVIOR_FLOW);
    InternalMessage outMessage = client.request(OUT_2_JMS_ENDPOINT, TIMEOUT).getRight().get();
    assertThat(outMessage, notNullValue());
    assertThat(getPayloadAsString(outMessage), is(MESSAGE));
    assertThat(client.request(IN_2_JMS_ENDPOINT, SHORT_TIMEOUT).getRight().isPresent(), is(false));
    InternalMessage inVmMessage = client.request(IN_2_VM_ENDPOINT, TIMEOUT).getRight().get();
    assertThat(inVmMessage, notNullValue());
    assertThat(getPayloadAsString(inVmMessage), is(MESSAGE));
  }

  @Test
  public void transactionCommitFailureTriggersExceptionStrategy() throws Exception {
    transactionCommitFailureExecutesExceptionStrategy(InternalMessage.of(TEST_PAYLOAD));
  }

  @Test
  public void transactionCommitFailureTriggersExceptionStrategyUsingFilter() throws Exception {
    final InternalMessage muleMessage =
        InternalMessage.builder().payload(TEST_PAYLOAD).addOutboundProperty("filterMessage", true).build();
    transactionCommitFailureExecutesExceptionStrategy(muleMessage);
  }

  private void transactionCommitFailureExecutesExceptionStrategy(InternalMessage muleMessage) throws Exception {
    getFunctionalTestComponent(TRANSACTION_COMMIT_FAILS_FLOW).setEventCallback(replaceTransactionWithMock());
    ExceptionListener exceptionListener = new ExceptionListener(muleContext);
    muleContext.getClient().dispatch(IN_3_VM_ENDPOINT, muleMessage);
    exceptionListener.waitUntilAllNotificationsAreReceived();
    stopFlowConstruct(TRANSACTION_COMMIT_FAILS_FLOW);
    exceptionListener.assertExpectedException(MessagingException.class);
  }

  @Test
  public void transactionCommitFailureWithinCatchExceptionStrategy() throws Exception {
    SystemExceptionListener systemExceptionListener = new SystemExceptionListener(muleContext);
    getFunctionalTestComponent(TRANSACTION_COMMIT_FAILS_FLOW).setEventCallback(replaceTransactionWithMockAndFailComponent());
    ExceptionListener exceptionListener = new ExceptionListener(muleContext);
    muleContext.getClient().dispatch(IN_3_VM_ENDPOINT, InternalMessage.of(TEST_PAYLOAD));
    exceptionListener.waitUntilAllNotificationsAreReceived();
    stopFlowConstruct(TRANSACTION_COMMIT_FAILS_FLOW);
    systemExceptionListener.waitUntilAllNotificationsAreReceived();
  }

  private EventCallback replaceTransactionWithMock(final EventCallback processEventCallback) throws Exception {
    when(mockTransaction.supports(anyObject(), anyObject())).thenReturn(true);
    doAnswer(invocationOnMock -> {
      TransactionCoordination.getInstance().unbindTransaction(mockTransaction);
      throw new RuntimeException();
    }).when(mockTransaction).commit();
    return (context, component, muleContext) -> {
      context.getCurrentTransaction().rollback();
      TransactionCoordination.getInstance().bindTransaction(mockTransaction);
      processEventCallback.eventReceived(context, component, muleContext);
    };
  }

  private EventCallback replaceTransactionWithMock() throws Exception {
    return replaceTransactionWithMock((context, component, muleContext) -> {
      // Do nothing
    });
  }

  private EventCallback replaceTransactionWithMockAndFailComponent() throws Exception {
    return replaceTransactionWithMock((context, component, muleContext) -> {
      throw new RuntimeException();
    });
  }

  private EventCallback getFailureCallback() {
    return (context, component, muleContext) -> {
      throw new RuntimeException();
    };
  }

}
