/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.api;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutorFactory;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.tests.internal.BaseLifecycleTracker;

import java.util.Optional;

import org.slf4j.Logger;

public class LifecycleTrackerEnricher implements DeclarationEnricher {

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    extensionLoadingContext.getExtensionDeclarer().getDeclaration().getOperations().forEach(operation -> {
      if (operation.getName().contains("lifecycleTracker")) {
        boolean shouldCheckPhase = operation.getName().contains("Check");
        CompletableComponentExecutorFactory executorFactory = getExecutorFactory(operation, shouldCheckPhase);
        operation.addModelProperty(new CompletableComponentExecutorModelProperty(executorFactory));
      }
    });
  }

  private CompletableComponentExecutorFactory getExecutorFactory(OperationDeclaration operation, boolean shouldCheckPhase) {
    Optional<CompletableComponentExecutorModelProperty> executorModelProperty =
        operation.getModelProperty(CompletableComponentExecutorModelProperty.class);
    CompletableComponentExecutorFactory<ComponentModel> oldFactory =
        executorModelProperty.map(CompletableComponentExecutorModelProperty::getExecutorFactory).orElse(null);
    return (componentModel, map) -> {
      CompletableComponentExecutor<ComponentModel> delegateExecutor = null;
      if (oldFactory != null) {
        delegateExecutor = oldFactory.createExecutor(componentModel, map);
      }
      return new LifecycleTrackerComponentExecutorDecorator(delegateExecutor, shouldCheckPhase);
    };
  }

  private static class LifecycleTrackerComponentExecutorDecorator extends BaseLifecycleTracker
      implements CompletableComponentExecutor<ComponentModel> {

    private static final Logger LOGGER = getLogger(LifecycleTrackerComponentExecutorDecorator.class);

    private final CompletableComponentExecutor<ComponentModel> delegate;

    LifecycleTrackerComponentExecutorDecorator(CompletableComponentExecutor<ComponentModel> delegate, boolean shouldCheck) {
      super(shouldCheck);
      this.delegate = delegate;
    }

    @Override
    public void execute(ExecutionContext<ComponentModel> executionContext, ExecutorCallback executorCallback) {
      addTrackingDataToRegistry(executionContext.getParameter("name"));
      delegate.execute(executionContext, executorCallback);
    }

    @Override
    protected void onInit(MuleContext muleContext) throws InitialisationException {
      initialiseIfNeeded(delegate, muleContext);
    }

    @Override
    protected void onStart() throws MuleException {
      startIfNeeded(delegate);
    }

    @Override
    protected void onStop() throws MuleException {
      stopIfNeeded(delegate);
    }

    @Override
    protected void onDispose() {
      disposeIfNeeded(delegate, LOGGER);
    }
  }
}
