/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import static java.util.Optional.ofNullable;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.lifecycle.DefaultLifecycleManager;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.util.UUID;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


//TODO MULE-10963 - Remove FlowConstruct implementation once MPs don't depend on FlowConstructAware anymore.
public class DefaultPolicyInstance implements PolicyInstance, FlowConstruct, MuleContextAware, Lifecycle {

  private final static Logger logger = LoggerFactory.getLogger(DefaultPolicyInstance.class);

  private PolicyChain operationPolicyChain;
  private PolicyChain sourcePolicyChain;

  private FlowConstructStatistics flowConstructStatistics = new FlowConstructStatistics("policy", getName());
  private String name = "proxy-policy-" + UUID.getUUID();
  private MuleContext muleContext;
  private DefaultLifecycleManager lifecycleStateManager = new DefaultLifecycleManager(this.name, this);

  @Override
  public void initialise() throws InitialisationException {
    LifecycleUtils.initialiseIfNeeded(operationPolicyChain, muleContext, this);
    LifecycleUtils.initialiseIfNeeded(sourcePolicyChain, muleContext, this);
    lifecycleStateManager.fireInitialisePhase((phaseNam, object) -> {
    });
  }

  @Override
  public void start() throws MuleException {
    LifecycleUtils.startIfNeeded(operationPolicyChain);
    LifecycleUtils.startIfNeeded(sourcePolicyChain);
    lifecycleStateManager.fireStartPhase((phaseNam, object) -> {
    });

  }

  @Override
  public MessagingExceptionHandler getExceptionListener() {
    return null;
  }

  @Override
  public FlowConstructStatistics getStatistics() {
    return this.flowConstructStatistics;
  }

  @Override
  public MuleContext getMuleContext() {
    return muleContext;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public LifecycleState getLifecycleState() {
    return lifecycleStateManager.getState();
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public void dispose() {
    LifecycleUtils.disposeIfNeeded(operationPolicyChain, logger);
    LifecycleUtils.disposeIfNeeded(sourcePolicyChain, logger);
    lifecycleStateManager.fireDisposePhase((phaseNam, object) -> {
    });
  }

  @Override
  public void stop() throws MuleException {
    LifecycleUtils.stopIfNeeded(operationPolicyChain);
    LifecycleUtils.stopIfNeeded(sourcePolicyChain);
    lifecycleStateManager.fireStopPhase((phaseNam, object) -> {
    });
  }

  public void setOperationPolicyChain(PolicyChain request) {
    this.operationPolicyChain = request;
  }

  public void setSourcePolicyChain(PolicyChain source) {
    this.sourcePolicyChain = source;
  }

  @Override
  public Optional<PolicyChain> getSourcePolicyChain() {
    return ofNullable(sourcePolicyChain);
  }

  @Override
  public Optional<PolicyChain> getOperationPolicyChain() {
    return ofNullable(operationPolicyChain);
  }
}
