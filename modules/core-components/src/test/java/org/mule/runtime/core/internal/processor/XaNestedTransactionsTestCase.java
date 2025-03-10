/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_COMMIT;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_CONTINUE;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_START;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.core.internal.processor.TryScopeTestUtils.createPropagateErrorHandler;
import static org.mule.runtime.core.internal.processor.TryScopeTestUtils.createTryScope;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static org.mule.test.allure.AllureConstants.TransactionFeature.TRANSACTION;
import static org.mule.test.allure.AllureConstants.TransactionFeature.XaStory.XA_TRANSACTION;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.exception.OnErrorContinueHandler;
import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(TRANSACTION)
@Story(XA_TRANSACTION)
public class XaNestedTransactionsTestCase extends AbstractMuleContextTestCase {

  private static List<Transaction> transactions;
  private ProfilingService profilingService = mock(ProfilingService.class);
  private Flow flow;

  @Before
  public void before() throws RegistrationException {
    flow = builder("flow", mockContextWithServices()).build();
    flow.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));
  }

  @Before
  public void setup() throws Exception {
    transactions = new ArrayList<>();
    when(profilingService.getProfilingDataProducer(TX_CONTINUE)).thenReturn(mock(ProfilingDataProducer.class));
    when(profilingService.getProfilingDataProducer(TX_START)).thenReturn(mock(ProfilingDataProducer.class));
    when(profilingService.getProfilingDataProducer(TX_COMMIT)).thenReturn(mock(ProfilingDataProducer.class));
  }

  @Test
  public void xaAllowsNestedTx() throws Exception {
    TryScope inner = createTryScope(muleContext, profilingService, of(true), empty());
    inner.setMessageProcessors(singletonList(new TxCaptor()));
    TryScope outer = createTryScope(muleContext, profilingService, of(true), empty());
    outer.setMessageProcessors(asList(new TxCaptor(), inner, new TxCaptor()));

    outer.initialise();
    try {
      outer.process(getNullEvent());
      assertThat(transactions, hasSize(3));
      assertThat(transactions.get(0), is(not(transactions.get(1))));
      assertThat(transactions.get(0), is(transactions.get(2)));
    } finally {
      outer.dispose();
    }
  }

  @Test
  public void xaErrorInNestedTx() throws Exception {
    TryScope inner = createTryScope(muleContext, profilingService, of(true));
    inner.setMessageProcessors(asList(new TxCaptor(), new ErrorProcessor()));
    TemplateOnErrorHandler handler = createPropagateErrorHandler();
    handler.setMessageProcessors(singletonList(new TxCaptor()));
    inner.setExceptionListener(handler);

    TryScope withHandler = createTryScope(muleContext, profilingService, empty());
    withHandler.setMessageProcessors(singletonList(inner));
    withHandler.setExceptionListener(new OnErrorContinueHandler());

    TryScope outer = createTryScope(muleContext, profilingService, of(true));
    outer.setMessageProcessors(asList(new TxCaptor(), withHandler, new TxCaptor()));
    outer.setExceptionListener(new OnErrorContinueHandler());
    outer.initialise();

    try {
      outer.process(getNullEvent());
      assertThat(transactions, hasSize(4));
      assertThat(transactions.get(0).getStatus(), is(Transaction.STATUS_COMMITTED));
      assertThat(transactions.get(1).getStatus(), is(Transaction.STATUS_ROLLEDBACK));
      assertThat(transactions.get(2), is(nullValue()));
      assertThat(transactions.get(3).getStatus(), is(Transaction.STATUS_COMMITTED));
    } finally {
      outer.dispose();
    }
  }

  @Test
  public void xaErrorInOuterTx() throws Exception {
    TryScope inner = createTryScope(muleContext, profilingService, of(true));
    inner.setMessageProcessors(singletonList(new TxCaptor()));

    TryScope outer = createTryScope(muleContext, profilingService, of(true));
    outer.setMessageProcessors(asList(new TxCaptor(), inner, new ErrorProcessor()));
    outer.setExceptionListener(createPropagateErrorHandler());

    TryScope surrounding = createTryScope(muleContext, profilingService, empty());
    surrounding.setMessageProcessors(singletonList(outer));
    surrounding.setExceptionListener(new OnErrorContinueHandler());
    surrounding.initialise();

    try {
      surrounding.process(getNullEvent());
      assertThat(transactions, hasSize(2));
      assertThat(transactions.get(0).getStatus(), is(Transaction.STATUS_ROLLEDBACK));
      assertThat(transactions.get(1).getStatus(), is(Transaction.STATUS_COMMITTED));
    } finally {
      surrounding.dispose();
    }
  }

  public static class TxCaptor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      Transaction tx = TransactionCoordination.getInstance().getTransaction();
      transactions.add(tx);
      return event;
    }
  }

  public static class ErrorProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      throw new ConnectionException("I tried to connect and I couldn't. Boo hoo.");
    }
  }

}
