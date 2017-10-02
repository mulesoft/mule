/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.policy;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.error;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.core.internal.lifecycle.DefaultLifecycleManager;
import org.mule.runtime.core.internal.management.stats.DefaultFlowConstructStatistics;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import java.util.Optional;


//TODO MULE-10963 - Remove FlowConstruct implementation once MPs don't depend on FlowConstructAware anymore.
public class DefaultPolicyInstance extends AbstractComponent
    implements PolicyInstance, FlowConstruct, MuleContextAware, Lifecycle {

  private final static Logger logger = getLogger(DefaultPolicyInstance.class);

  private PolicyChain operationPolicyChain;
  private PolicyChain sourcePolicyChain;

  private FlowConstructStatistics flowConstructStatistics = new DefaultFlowConstructStatistics("policy", getName());
  private String name = "proxy-policy-" + UUID.getUUID();
  private MuleContext muleContext;
  private DefaultLifecycleManager lifecycleStateManager = new DefaultLifecycleManager(this.name, this);

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(operationPolicyChain, muleContext);
    initialiseIfNeeded(sourcePolicyChain, muleContext);
    lifecycleStateManager.fireInitialisePhase((phaseNam, object) -> {
    });
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(operationPolicyChain);
    startIfNeeded(sourcePolicyChain);
    lifecycleStateManager.fireStartPhase((phaseNam, object) -> {
    });

  }

  @Override
  public FlowExceptionHandler getExceptionListener() {
    return new FlowExceptionHandler() {

      @Override
      public CoreEvent handleException(Exception exception, CoreEvent event) {
        return null;
      }

      @Override
      public Publisher<CoreEvent> apply(Exception exception) {
        return error(exception);
      }
    };
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
  public String getUniqueIdString() {
    return muleContext.getUniqueIdString();
  }

  @Override
  public String getServerId() {
    return muleContext.getId();
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
    disposeIfNeeded(operationPolicyChain, logger);
    disposeIfNeeded(sourcePolicyChain, logger);
    lifecycleStateManager.fireDisposePhase((phaseNam, object) -> {
    });
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(operationPolicyChain);
    stopIfNeeded(sourcePolicyChain);
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
