/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.tck.junit4.AbstractMuleTestCase.TEST_CONNECTOR_LOCATION;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;
import org.mule.runtime.core.internal.profiling.DummyComponentTracerFactory;
import org.mule.tck.testmodels.mule.TestTransactionFactory;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;


public class TryScopeTestUtils {

  private TryScopeTestUtils() {

  }

  public static TryScope createTryScope(boolean begintx, MuleContext muleContext, ProfilingService profilingService)
      throws MuleException {
    return createTryScope(begintx, muleContext, profilingService, true, 0);
  }

  public static TryScope createTryScope(boolean begintx, MuleContext muleContext, ProfilingService profilingService,
                                        boolean isXa, int timeout)
      throws MuleException {
    TryScope scope = new TryScope();
    Map<QName, Object> annotations = new HashMap<>();
    annotations.put(LOCATION_KEY, TEST_CONNECTOR_LOCATION);
    scope.setAnnotations(annotations);
    if (begintx) {
      scope.setTransactionConfig(createTransactionConfig("ALWAYS_BEGIN", timeout, isXa));
    } else {
      scope.setTransactionConfig(createTransactionConfig("INDIFFERENT", timeout, isXa));
    }
    scope.setComponentTracerFactory(new DummyComponentTracerFactory());
    scope.setExceptionListener(mock(FlowExceptionHandler.class));
    scope.setProfilingService(profilingService);
    scope.setMuleConfiguration(mock(MuleConfiguration.class));
    scope.setTransactionManager(muleContext.getTransactionManager());
    scope.setMuleContext(muleContext);
    scope.setNotificationDispatcher(mock(NotificationDispatcher.class));
    muleContext.getInjector().inject(scope);
    return scope;
  }

  private static MuleTransactionConfig createTransactionConfig(String action, int timeout, boolean isXa) {
    MuleTransactionConfig transactionConfig = new MuleTransactionConfig();
    transactionConfig.setActionAsString(action);
    transactionConfig.setFactory(new TestTransactionFactory(isXa));
    transactionConfig.setTimeout(timeout);
    return transactionConfig;
  }

}
