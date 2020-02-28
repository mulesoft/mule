/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static java.lang.Thread.currentThread;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.internal.policy.PolicyManager;

import javax.inject.Inject;

/**
 * Default implementation for {@link MessageProcessingManager}.
 */
public class MuleMessageProcessingManager implements MessageProcessingManager, Initialisable, PhaseResultNotifier {

  private PolicyManager policyManager;
  private MuleContext muleContext;

  private FlowProcessMediator mediator;
  private SystemExceptionHandler exceptionListener;

  @Override
  public void initialise() throws InitialisationException {
    exceptionListener = muleContext.getExceptionListener();
    mediator = new FlowProcessMediator(policyManager);
    initialiseIfNeeded(mediator, true, muleContext);
  }

  @Override
  public void processMessage(FlowProcessTemplate messageProcessTemplate,
                             MessageProcessContext messageProcessContext) {
    Thread currentThread = currentThread();
    ClassLoader originalTCCL = currentThread.getContextClassLoader();
    ClassLoader executionClassLoader = messageProcessContext.getExecutionClassLoader();
    setContextClassLoader(currentThread, originalTCCL, executionClassLoader);
    try {
      mediator.process(messageProcessTemplate, messageProcessContext, this);
    } finally {
      setContextClassLoader(currentThread, executionClassLoader, originalTCCL);
    }
  }

  @Override
  public void phaseSuccessfully() {

  }

  @Override
  public void phaseFailure(Exception reason) {
    exceptionListener.handleException(reason);
  }

  @Inject
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Inject
  public void setPolicyManager(PolicyManager policyManager) {
    this.policyManager = policyManager;
  }
}
