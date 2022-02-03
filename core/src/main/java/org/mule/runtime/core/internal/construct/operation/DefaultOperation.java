/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct.operation;

import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Operation;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.construct.AbstractPipeline;

import java.util.List;
import java.util.Optional;

public class DefaultOperation extends AbstractPipeline implements Operation {

  public DefaultOperation(String name,
                          MuleContext muleContext,
                          MessageSource source,
                          List<Processor> processors,
                          Optional<FlowExceptionHandler> exceptionListener,
                          Optional<ProcessingStrategyFactory> processingStrategyFactory,
                          String initialState,
                          Integer maxConcurrency,
                          FlowConstructStatistics flowConstructStatistics,
                          ComponentInitialStateManager componentInitialStateManager) {
    super(name, muleContext, source, processors, exceptionListener, processingStrategyFactory, initialState, maxConcurrency, flowConstructStatistics, componentInitialStateManager);
  }

  @Override
  public OperationModel getModel() {
    return null;
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return null;
  }
}
