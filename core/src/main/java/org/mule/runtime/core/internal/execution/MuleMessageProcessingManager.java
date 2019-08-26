/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

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

  private ModuleFlowProcessingPhase delegate;
  private SystemExceptionHandler exceptionListener;

  @Override
  public void initialise() throws InitialisationException {
    exceptionListener = muleContext.getExceptionListener();
    delegate = new ModuleFlowProcessingPhase(policyManager);
    delegate.initialise();
  }

  @Override
  public void processMessage(ModuleFlowProcessingPhaseTemplate messageProcessTemplate,
                             MessageProcessContext messageProcessContext) {
    delegate.runPhase(messageProcessTemplate, messageProcessContext, this);
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
