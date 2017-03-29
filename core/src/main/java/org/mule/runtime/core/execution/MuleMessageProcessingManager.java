/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.policy.PolicyManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

/**
 * Default implementation for {@link MessageProcessingManager}.
 */
public class MuleMessageProcessingManager implements MessageProcessingManager, MuleContextAware, Initialisable {

  private final EndProcessPhase endProcessPhase = new EndProcessPhase();
  private MuleContext muleContext;
  private PhaseExecutionEngine phaseExecutionEngine;

  @Inject
  private PolicyManager policyManager;

  @Override
  public void processMessage(MessageProcessTemplate messageProcessTemplate, MessageProcessContext messageProcessContext) {
    phaseExecutionEngine.process(messageProcessTemplate, messageProcessContext);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public void initialise() throws InitialisationException {
    Collection<MessageProcessPhase> registryMessageProcessPhases =
        muleContext.getRegistry().lookupObjects(MessageProcessPhase.class);
    List<MessageProcessPhase> messageProcessPhaseList = new ArrayList<MessageProcessPhase>();
    if (registryMessageProcessPhases != null) {
      messageProcessPhaseList.addAll(registryMessageProcessPhases);
    }
    messageProcessPhaseList.add(new ValidationPhase());
    messageProcessPhaseList.add(new FlowProcessingPhase());
    messageProcessPhaseList.add(new AsyncResponseFlowProcessingPhase());
    messageProcessPhaseList.add(new ModuleFlowProcessingPhase(policyManager));
    Collections.sort(messageProcessPhaseList, (messageProcessPhase, messageProcessPhase2) -> {
      int compareValue = 0;
      if (messageProcessPhase instanceof Comparable) {
        compareValue = ((Comparable) messageProcessPhase).compareTo(messageProcessPhase2);
      }
      if (compareValue == 0 && messageProcessPhase2 instanceof Comparable) {
        compareValue = ((Comparable) messageProcessPhase2).compareTo(messageProcessPhase) * -1;
      }
      return compareValue;
    });

    for (MessageProcessPhase messageProcessPhase : messageProcessPhaseList) {
      if (messageProcessPhase instanceof MuleContextAware) {
        ((MuleContextAware) messageProcessPhase).setMuleContext(muleContext);
      }
      if (messageProcessPhase instanceof Initialisable) {
        ((Initialisable) messageProcessPhase).initialise();
      }
    }

    phaseExecutionEngine = new PhaseExecutionEngine(messageProcessPhaseList, muleContext.getExceptionListener(), endProcessPhase);
  }
}
