/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.construct;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getDefaultProcessingStrategyFactory;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.execution.ExecutionResult;
import org.mule.runtime.api.component.execution.InputEvent;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

/**
 * Default {@link Operation} implementation
 *
 * @since 4.5.0
 */
public class MuleOperation extends AbstractComponent implements Operation {

  private final static Logger LOGGER = getLogger(MuleOperation.class);

  public static Builder builder() {
    return new DefaultOperationBuilder();
  }

  private final List<Processor> processors;
  private final OperationModel operationModel;
  private final MuleContext muleContext;

  private MessageProcessorChain chain;
  private ProcessingStrategy processingStrategy;

  MuleOperation(List<Processor> processors,
                OperationModel operationModel,
                MuleContext muleContext) {
    this.processors = processors;
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
    processingStrategy = getDefaultProcessingStrategyFactory(muleContext).create(muleContext, operationModel.getName());
    initialiseIfNeeded(processingStrategy);

    chain = newChain(ofNullable(processingStrategy), processors);
    initialiseIfNeeded(chain, true, muleContext);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(processingStrategy);
    startIfNeeded(chain);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(chain);
    stopIfNeeded(processingStrategy);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(chain, LOGGER);
    disposeIfNeeded(processingStrategy, LOGGER);
  }

  @Override
  public OperationModel getModel() {
    return operationModel;
  }
}
