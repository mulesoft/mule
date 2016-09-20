/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static java.util.Collections.singletonList;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setFlowConstructIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;
import static org.mule.runtime.core.execution.MessageProcessorExecutionTemplate.createExecutionTemplate;

import org.mule.runtime.core.VoidMuleEvent;
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
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
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
  protected Event processResponse(final Event response, final Event request) throws MuleException {
    if (responseProcessor == null || !isEventValid(response)) {
      return response;
    } else {
      Event result = responseProcessor.process(response);
      if (result == null || VoidMuleEvent.getInstance().equals(result)) {
        // If <response> returns null then it acts as an implicit branch like in flows, the different
        // here is that what's next, it's not another message processor that follows this one in the
        // configuration file but rather the response phase of the inbound endpoint, or optionally
        // other response processing on the way back to the inbound endpoint.
        return response;
      } else {
        return result;
      }
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
    setFlowConstructIfNeeded(responseProcessor, flowConstruct);
    messageProcessorExecutionTemplate.setFlowConstruct(flowConstruct);
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    super.setMuleContext(muleContext);
    setMuleContextIfNeeded(responseProcessor, muleContext);
    messageProcessorExecutionTemplate.setMuleContext(muleContext);
  }
}
