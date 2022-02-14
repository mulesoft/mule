/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation.construct;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.execution.ExecutionResult;
import org.mule.runtime.api.component.execution.InputEvent;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Operation;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

public class MuleOperation extends AbstractComponent implements Operation {

  private final static Logger LOGGER = getLogger(MuleOperation.class);

  public static Builder builder() {
    return new DefaultOperationBuilder();
  }

  private final MessageProcessorChain chain;
  private final OperationModel operationModel;
  private final MuleContext muleContext;

  MuleOperation(MessageProcessorChain chain,
                OperationModel operationModel,
                MuleContext muleContext) {
    this.chain = chain;
    this.operationModel = operationModel;
    this.muleContext = muleContext;
  }

  @Override
  public CompletableFuture<ExecutionResult> execute(InputEvent inputEvent) {
    return chain.execute(inputEvent);
  }

  @Override
  public CompletableFuture<Event> execute(Event event) {
    return chain.execute(event);
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
}
