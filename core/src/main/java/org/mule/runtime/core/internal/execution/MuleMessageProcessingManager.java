/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;
import static org.mule.runtime.core.internal.policy.PolicyManager.NOOP_POLICY_MANAGER;

import static java.lang.Thread.currentThread;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

import java.util.Optional;

import jakarta.inject.Inject;

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
    mediator = new FlowProcessMediator(policyManager, this);
    initialiseIfNeeded(mediator, muleContext);
  }

  @Override
  public void processMessage(FlowProcessTemplate messageProcessTemplate,
                             MessageProcessContext messageProcessContext) {
    processMessage(messageProcessTemplate, messageProcessContext, null);
  }

  @Override
  public void processMessage(FlowProcessTemplate messageProcessTemplate, MessageProcessContext messageProcessContext,
                             DistributedTraceContextManager sourceDistributedTraceContextManager) {
    Thread currentThread = currentThread();
    ClassLoader originalTCCL = currentThread.getContextClassLoader();
    ClassLoader executionClassLoader = messageProcessContext.getExecutionClassLoader();
    setContextClassLoader(currentThread, originalTCCL, executionClassLoader);
    try {
      mediator.process(messageProcessTemplate, messageProcessContext, ofNullable(sourceDistributedTraceContextManager));
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
  public void setPolicyManager(Optional<PolicyManager> policyManager) {
    this.policyManager = policyManager.orElse(NOOP_POLICY_MANAGER);
  }
}
