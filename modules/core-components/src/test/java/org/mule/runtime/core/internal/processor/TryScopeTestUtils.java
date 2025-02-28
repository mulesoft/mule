/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.tck.junit4.AbstractMuleTestCase.TEST_CONNECTOR_LOCATION;

import static java.util.Optional.empty;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.internal.exception.OnErrorPropagateHandler;
import org.mule.runtime.core.internal.profiling.DummyComponentTracerFactory;
import org.mule.runtime.core.internal.transaction.MuleTransactionConfig;
import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;
import org.mule.tck.testmodels.mule.TestTransactionFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.transaction.TransactionManager;
import javax.xml.namespace.QName;

/**
 * Utils to create a {@link TryScope} fully configured.
 */
public class TryScopeTestUtils {

  private TryScopeTestUtils() {

  }

  /**
   * Creates a {@link TryScope} that could be transactional (with local or xa transaction), without timeout.
   */
  public static TryScope createTryScope(MuleContext muleContext,
                                        TransactionManager txManager,
                                        ProfilingService profilingService,
                                        Optional<Boolean> txType)
      throws MuleException {
    return createTryScope(muleContext, txManager, profilingService, txType, empty());
  }

  /**
   * Creates a {@link TryScope} that could be transactional (with local or xa transaction), with a given timeout.
   */
  public static TryScope createTryScope(MuleContext muleContext,
                                        TransactionManager txManager,
                                        ProfilingService profilingService,
                                        Optional<Boolean> txType,
                                        Optional<Integer> timeout)
      throws MuleException {
    TryScope scope = new TryScope();
    Map<QName, Object> annotations = new HashMap<>();
    annotations.put(LOCATION_KEY, TEST_CONNECTOR_LOCATION);
    scope.setAnnotations(annotations);
    if (txType.isPresent()) {
      scope.setTransactionConfig(createTransactionConfig("ALWAYS_BEGIN", txType.get(), timeout));
    } else {
      scope.setTransactionConfig(createTransactionConfig("INDIFFERENT", false, timeout));
    }
    scope.setComponentTracerFactory(new DummyComponentTracerFactory());
    scope.setExceptionListener(mock(FlowExceptionHandler.class));
    scope.setProfilingService(profilingService);
    scope.setMuleConfiguration(mock(MuleConfiguration.class));
    scope.setTransactionManager(txManager);
    scope.setMuleContext(muleContext);
    scope.setNotificationDispatcher(mock(NotificationDispatcher.class));
    muleContext.getInjector().inject(scope);
    return scope;
  }

  private static MuleTransactionConfig createTransactionConfig(String action, boolean isXa, Optional<Integer> timeout) {
    MuleTransactionConfig transactionConfig = new MuleTransactionConfig();
    transactionConfig.setActionAsString(action);
    transactionConfig.setFactory(new TestTransactionFactory(isXa));
    transactionConfig.setTimeout(timeout.orElse(0));
    return transactionConfig;
  }

  private static ComponentLocation mockComponentLocation() {
    ComponentLocation cl = mock(ComponentLocation.class);
    when(cl.getLocation()).thenReturn("test/error-handler/0");
    when(cl.getRootContainerName()).thenReturn(TEST_CONNECTOR_LOCATION.getRootContainerName());
    when(cl.getParts()).thenReturn(TEST_CONNECTOR_LOCATION.getParts());
    return cl;
  }

  public static TemplateOnErrorHandler createPropagateErrorHandler() {
    TemplateOnErrorHandler handler = new OnErrorPropagateHandler();
    Map<QName, Object> annotations = new HashMap<>();
    annotations.put(LOCATION_KEY, mockComponentLocation());
    handler.setAnnotations(annotations);
    return handler;
  }

}
