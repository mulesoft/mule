/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.mule.runtime.core.message.DefaultEventBuilder.EventImplementation.setCurrentEvent;

import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ProcessorExecutor;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transport.LegacyOutboundEndpoint;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.execution.MessageProcessorExecutionTemplate;
import org.mule.runtime.core.routing.MessageFilter;

import java.util.List;

/**
 * This {@link ProcessorExecutor} implementation executes each {@link Processor} in succession in the same thread until or
 * processors have been invoked or one of the following is returned by a processor:
 * <li>{@link org.mule.runtime.core.VoidMuleEvent}</li>
 * <li>{@code null}</li>
 */
public class BlockingProcessorExecutor implements ProcessorExecutor {

  protected final MessageProcessorExecutionTemplate messageProcessorExecutionTemplate;
  protected final boolean copyOnVoidEvent;
  protected final List<Processor> processors;

  protected Event event;
  private int index;

  public BlockingProcessorExecutor(Event event, List<Processor> processors,
                                   MessageProcessorExecutionTemplate messageProcessorExecutionTemplate, boolean copyOnVoidEvent) {
    this.event = event;
    this.processors = processors;
    this.copyOnVoidEvent = copyOnVoidEvent;
    this.messageProcessorExecutionTemplate = messageProcessorExecutionTemplate;
  }

  @Override
  public final Event execute() throws MessagingException {
    Event result = event;
    while (hasNext() && isEventValid(event)) {
      result = executeNext();
      if (!isEventValid(result)) {
        break;
      }
      event = result;
    }
    return result;
  }

  private boolean isEventValid(Event result) {
    return result != null && !(result instanceof VoidMuleEvent);
  }

  protected boolean hasNext() {
    return index < processors.size();
  }

  protected Event executeNext() throws MessagingException {
    Processor processor = nextProcessor();

    preProcess(processor);

    if (copyOnVoidEvent
        && !(processor instanceof Transformer || processor instanceof MessageFilter || processor instanceof Component
            || (processor instanceof LegacyOutboundEndpoint && !((LegacyOutboundEndpoint) processor).mayReturnVoidEvent()))) {
      Event copy = Event.builder(event).build();
      Event result = messageProcessorExecutionTemplate.execute(processor, event);

      if (isUseEventCopy(result)) {
        setCurrentEvent(copy);
        result = copy;
      }
      return result;
    } else {
      return messageProcessorExecutionTemplate.execute(processor, event);
    }
  }

  protected boolean isUseEventCopy(Event result) {
    return VoidMuleEvent.getInstance().equals(result);
  }

  protected void preProcess(Processor processor) {}

  protected Processor nextProcessor() {
    return processors.get(index++);
  }

}
