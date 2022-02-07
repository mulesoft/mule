/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation.construct;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.internal.event.ParameterizedEventDecorator.parameterized;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Operation;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.event.ParameterizedEventDecorator;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext;

import java.util.Map;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

public class DefaultOperation implements Operation {

  private final static Logger LOGGER = getLogger(DefaultOperation.class);

  public static Builder builder() {
    return new DefaultOperationBuilder();
  }

  private final MessageProcessorChain chain;
  private final Location rootComponentLocation;
  private final ComponentLocation chainLocation;
  private final OperationModel operationModel;
  private final MuleContext muleContext;

  DefaultOperation(MessageProcessorChain chain,
                          Location rootComponentLocation,
                          ComponentLocation chainLocation,
                          OperationModel operationModel,
                          MuleContext muleContext) {
    this.chain = chain;
    this.rootComponentLocation = rootComponentLocation;
    this.chainLocation = chainLocation;
    this.operationModel = operationModel;
    this.muleContext = muleContext;
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return from(publisher)
        .map(event -> {
          SdkInternalContext sdkCtx = SdkInternalContext.from(event);
          Map<String, Object> params = sdkCtx.getOperationExecutionParams(chainLocation, event.getContext().getId()).getParameters();

          return parameterized(event, params);
        })
        .transform(chain)
        //TODO: Discuss with Rodro. What happens if the chain fails? How to deparametrize? Do I even need to?
        .map(ParameterizedEventDecorator::deparameterize);
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return chain.process(event);
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(chain, true, muleContext);
  }

  @Override
  public void start() throws MuleException {
    chain.start();
  }

  @Override
  public void stop() throws MuleException {
    chain.stop();
  }

  @Override
  public void dispose() {
    disposeIfNeeded(chain, LOGGER);
  }
  
  @Override
  public OperationModel getModel() {
    return operationModel;
  }

  @Override
  public ComponentLocation getChainLocation() {
    return chainLocation;
  }

  @Override
  public Location getRootComponentLocation() {
    return rootComponentLocation;
  }
}
