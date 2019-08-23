/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static java.util.Collections.emptySet;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.privileged.execution.MessageProcessContext;
import org.mule.runtime.core.privileged.execution.MessageProcessTemplate;
import org.mule.runtime.core.privileged.execution.MessageProcessingManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

/**
 * Default implementation for {@link MessageProcessingManager}.
 */
public class MuleMessageProcessingManager implements MessageProcessingManager, Initialisable, Startable, Stoppable {

  private final EndProcessPhase endProcessPhase = new EndProcessPhase();
  private MuleContext muleContext;
  private PhaseExecutionEngine phaseExecutionEngine;

  @Inject
  private PolicyManager policyManager;

  private Registry registry;

  private List<MessageProcessPhase> messageProcessPhaseList;
  private Collection<MessageProcessPhase> registryMessageProcessPhases;

  @Override
  public void processMessage(MessageProcessTemplate messageProcessTemplate, MessageProcessContext messageProcessContext) {
    phaseExecutionEngine.process(messageProcessTemplate, messageProcessContext);
  }

  @Inject
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Inject
  public void setRegistryMessageProcessPhases(Optional<Collection<MessageProcessPhase>> registryMessageProcessPhases) {
    // This needs to be an Optional due to https://jira.spring.io/browse/SPR-15338
    this.registryMessageProcessPhases = registryMessageProcessPhases.orElse(emptySet());
  }

  @Override
  public void initialise() throws InitialisationException {
    messageProcessPhaseList = new ArrayList<>();
    if (registryMessageProcessPhases != null) {
      messageProcessPhaseList.addAll(registryMessageProcessPhases);
    }
    messageProcessPhaseList.add(new ValidationPhase());
    messageProcessPhaseList.add(new FlowProcessingPhase(registry));
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
      initialiseIfNeeded(messageProcessPhase, muleContext);
    }

    phaseExecutionEngine = new PhaseExecutionEngine(messageProcessPhaseList, muleContext.getExceptionListener(), endProcessPhase);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(messageProcessPhaseList);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(messageProcessPhaseList);
  }

  @Inject
  public void setRegistry(Registry registry) {
    this.registry = registry;
  }
}
