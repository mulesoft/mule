/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getDefaultProcessingStrategyFactory;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.policy.PolicyChain;
import org.mule.runtime.core.api.policy.PolicyInstance;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.core.internal.lifecycle.DefaultLifecycleManager;

import java.util.Optional;

import org.slf4j.Logger;

@NoExtend
public class DefaultPolicyInstance extends AbstractComponent
    implements PolicyInstance, MuleContextAware, Lifecycle {

  private static final Logger LOGGER = getLogger(DefaultPolicyInstance.class);

  private ProcessingStrategy processingStrategy;

  private String name;

  private PolicyChain operationPolicyChain;
  private PolicyChain sourcePolicyChain;

  private MuleContext muleContext;
  private final DefaultLifecycleManager<DefaultPolicyInstance> lifecycleStateManager =
      new DefaultLifecycleManager<>("proxy-policy-" + UUID.getUUID(), this);

  @Override
  public void initialise() throws InitialisationException {
    processingStrategy = defaultProcessingStrategy().create(muleContext, getName());

    if (operationPolicyChain != null) {
      operationPolicyChain.setProcessingStrategy(processingStrategy);
    }
    if (sourcePolicyChain != null) {
      sourcePolicyChain.setProcessingStrategy(processingStrategy);
    }

    initialiseIfNeeded(operationPolicyChain, muleContext);
    initialiseIfNeeded(sourcePolicyChain, muleContext);
    lifecycleStateManager.fireInitialisePhase((phaseNam, object) -> {
    });
  }

  private ProcessingStrategyFactory defaultProcessingStrategy() {
    return getDefaultProcessingStrategyFactory(muleContext);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(processingStrategy);
    startIfNeeded(operationPolicyChain);
    startIfNeeded(sourcePolicyChain);
    lifecycleStateManager.fireStartPhase((phaseNam, object) -> {
    });

  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public void dispose() {
    disposeIfNeeded(operationPolicyChain, LOGGER);
    disposeIfNeeded(sourcePolicyChain, LOGGER);
    disposeIfNeeded(processingStrategy, LOGGER);
    lifecycleStateManager.fireDisposePhase((phaseNam, object) -> {
    });
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(operationPolicyChain);
    stopIfNeeded(sourcePolicyChain);
    stopIfNeeded(processingStrategy);
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

  @Override
  public ProcessingStrategy getProcessingStrategy() {
    return processingStrategy;
  }
}
