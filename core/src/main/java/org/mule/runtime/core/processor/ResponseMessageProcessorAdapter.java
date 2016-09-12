/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static java.util.Collections.singletonList;
import static org.mule.runtime.core.execution.MessageProcessorExecutionTemplate.createExecutionTemplate;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.execution.MessageProcessorExecutionTemplate;

import java.util.List;

public class ResponseMessageProcessorAdapter extends AbstractRequestResponseMessageProcessor
    implements Lifecycle, FlowConstructAware {

  protected MessageProcessorExecutionTemplate messageProcessorExecutionTemplate = createExecutionTemplate();

  protected Processor responseProcessor;

  public ResponseMessageProcessorAdapter() {
    super();
  }

  public ResponseMessageProcessorAdapter(Processor responseProcessor) {
    super();
    this.responseProcessor = responseProcessor;
  }

  public void setProcessor(Processor processor) {
    this.responseProcessor = processor;
  }

  @Override
  protected Event processResponse(Event response, final Event request) throws MuleException {
    if (responseProcessor == null || !isEventValid(response)) {
      return response;
    } else {
      return new CopyOnNullNonBlockingProcessorExecutor(response, singletonList(responseProcessor),
                                                        messageProcessorExecutionTemplate, true).execute();
    }
  }

  class CopyOnNullNonBlockingProcessorExecutor extends NonBlockingProcessorExecutor {

    public CopyOnNullNonBlockingProcessorExecutor(Event event, List<Processor> processors,
                                                  MessageProcessorExecutionTemplate executionTemplate, boolean copyOnVoidEvent) {
      super(event, processors, executionTemplate, copyOnVoidEvent, flowConstruct);
    }

    @Override
    protected boolean isUseEventCopy(Event result) {
      return super.isUseEventCopy(result) || result == null;
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    if (responseProcessor instanceof MuleContextAware) {
      ((MuleContextAware) responseProcessor).setMuleContext(muleContext);
    }
    if (responseProcessor instanceof FlowConstructAware) {
      ((FlowConstructAware) responseProcessor).setFlowConstruct(flowConstruct);
    }
    if (responseProcessor instanceof Initialisable) {
      ((Initialisable) responseProcessor).initialise();
    }
  }

  @Override
  public void start() throws MuleException {
    if (responseProcessor instanceof Startable) {
      ((Startable) responseProcessor).start();
    }
  }

  @Override
  public void stop() throws MuleException {
    if (responseProcessor instanceof Stoppable) {
      ((Stoppable) responseProcessor).stop();
    }
  }

  @Override
  public void dispose() {
    if (responseProcessor instanceof Disposable) {
      ((Disposable) responseProcessor).dispose();
    }
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    super.setFlowConstruct(flowConstruct);
    messageProcessorExecutionTemplate.setFlowConstruct(flowConstruct);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    super.setMuleContext(context);
    messageProcessorExecutionTemplate.setMuleContext(context);
  }
}
