/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.AbstractComponent.ROOT_CONTAINER_NAME_KEY;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.*;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.exception.OnErrorContinueHandler;
import org.mule.runtime.core.internal.exception.OnErrorPropagateHandler;
import org.mule.runtime.core.internal.profiling.DummyComponentTracerFactory;
import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.core.privileged.transaction.XaTransaction;
import org.mule.runtime.core.privileged.transaction.xa.XaTransactionFactory;
import org.mule.runtime.tracer.api.component.ComponentTracer;
import org.mule.runtime.tracer.api.component.ComponentTracerFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import javax.transaction.TransactionManager;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XaNestedTransactionTestCase extends AbstractMuleContextTestCase {

  private static List<Transaction> transactions;
  private static List<javax.transaction.Transaction> innerTransactions;

  private FlowExceptionHandler exceptionHandler = mock(FlowExceptionHandler.class);
  private ProfilingService profilingService = mock(ProfilingService.class);
  private TransactionManager manager = mock(TransactionManager.class);

  private MuleConfiguration configuration = mock(MuleConfiguration.class);

  private Flow flow;

  @Before
  public void before() throws RegistrationException {
    flow = builder("flow", mockContextWithServices()).build();
    flow.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));
  }

  @Before
  public void setup() throws Exception {
    transactions = new ArrayList<>();
    innerTransactions = new ArrayList<>();
    when(profilingService.getProfilingDataProducer(TX_CONTINUE)).thenReturn(mock(ProfilingDataProducer.class));
    when(profilingService.getProfilingDataProducer(TX_START)).thenReturn(mock(ProfilingDataProducer.class));
    when(profilingService.getProfilingDataProducer(TX_COMMIT)).thenReturn(mock(ProfilingDataProducer.class));
    muleContext.setTransactionManager(manager);
  }

  private TryScope createTryScope(boolean begintx) {
    TryScope scope = new TryScope();
    Map<QName, Object> annotations = new HashMap<>();
    annotations.put(LOCATION_KEY, TEST_CONNECTOR_LOCATION);
    scope.setAnnotations(annotations);
    if (begintx) {
      scope.setTransactionConfig(createTransactionConfig("ALWAYS_BEGIN"));
    } else {
      scope.setTransactionConfig(createTransactionConfig("INDIFFERENT"));
    }
    scope.setComponentTracerFactory(new DummyComponentTracerFactory());
    scope.setExceptionListener(exceptionHandler);
    scope.setProfilingService(profilingService);
    scope.setMuleConfiguration(configuration);
    scope.setTransactionManager(manager);
    scope.setMuleContext(muleContext);
    scope.setNotificationDispatcher(mock(NotificationDispatcher.class));
    return scope;
  }

  @Test
  public void xaAllowsNestedTx() throws Exception {
    TryScope inner = createTryScope(true);
    inner.setMessageProcessors(singletonList(new TxCaptor()));
    TryScope outer = createTryScope(true);
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

  private MuleTransactionConfig createTransactionConfig(String action) {
    MuleTransactionConfig transactionConfig = new MuleTransactionConfig();
    transactionConfig.setActionAsString(action);
    transactionConfig.setFactory(new XaTransactionFactory());
    return transactionConfig;
  }

  @Test
  public void xaErrorInNestedTx() throws Exception {
    TryScope inner = createTryScope(true);
    inner.setMessageProcessors(asList(new TxCaptor(), new ErrorProcessor()));
    TemplateOnErrorHandler handler = new OnErrorPropagateHandler();
    handler.setMessageProcessors(singletonList(new TxCaptor()));
    inner.setExceptionListener(handler);

    TryScope withHandler = createTryScope(false);
    withHandler.setMessageProcessors(singletonList(inner));
    withHandler.setExceptionListener(new OnErrorContinueHandler());

    TryScope outer = createTryScope(true);
    outer.setMessageProcessors(asList(new TxCaptor(), withHandler, new TxCaptor()));
    outer.setExceptionListener(new OnErrorContinueHandler());
    outer.initialise();

    try {
      outer.process(getNullEvent());
      assertThat(innerTransactions, hasSize(4));
      assertThat(innerTransactions.get(0).getStatus(), is(Transaction.STATUS_COMMITTED));
      assertThat(innerTransactions.get(1).getStatus(), is(Transaction.STATUS_ROLLEDBACK));
      assertThat(innerTransactions.get(2), is(nullValue()));
      assertThat(innerTransactions.get(3).getStatus(), is(Transaction.STATUS_COMMITTED));
    } finally {
      outer.dispose();
    }
  }

  @Test
  public void xaErrorInOuterTx() throws Exception {
    TryScope inner = createTryScope(true);
    inner.setMessageProcessors(singletonList(new TxCaptor()));

    TryScope outer = createTryScope(true);
    outer.setMessageProcessors(asList(new TxCaptor(), inner, new ErrorProcessor()));

    TryScope surrounding = createTryScope(false);
    surrounding.setMessageProcessors(singletonList(outer));
    surrounding.setExceptionListener(new OnErrorContinueHandler());
    surrounding.initialise();

    try {
      surrounding.process(getNullEvent());
      assertThat(transactions, hasSize(2));
      assertThat(innerTransactions.get(0).getStatus(), is(Transaction.STATUS_ROLLEDBACK));
      assertThat(innerTransactions.get(1).getStatus(), is(Transaction.STATUS_COMMITTED));
    } finally {
      surrounding.dispose();
    }
  }

  public static class TxCaptor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      Transaction tx = TransactionCoordination.getInstance().getTransaction();
      transactions.add(tx);
      innerTransactions.add(tx != null ? ((XaTransaction) tx).getTransaction() : null);
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
