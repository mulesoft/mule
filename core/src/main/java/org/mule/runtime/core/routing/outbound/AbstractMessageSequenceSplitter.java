/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.outbound;

import static java.util.Collections.emptySet;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.routing.RouterResultsHandler;
import org.mule.runtime.core.message.GroupCorrelation;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;
import org.mule.runtime.core.routing.AbstractSplitter;
import org.mule.runtime.core.routing.DefaultRouterResultsHandler;
import org.mule.runtime.core.routing.MessageSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Base implementation of a {@link Message} splitter, that converts its payload in a {@link MessageSequence}, and process each
 * element of it. Implementations must implement {@link #splitMessageIntoSequence(Event)} and determine how the message is split.
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/Sequencer.html">http://www .eaipatterns.com/Sequencer.html</a>
 * 
 * @author flbulgarelli
 * @see AbstractSplitter
 */
public abstract class AbstractMessageSequenceSplitter extends AbstractInterceptingMessageProcessor
    implements MuleContextAware {

  protected RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();
  protected int batchSize;
  protected String counterVariableName;

  @Override
  public final Event process(Event event) throws MuleException {
    if (isSplitRequired(event)) {
      MessageSequence<?> seq = splitMessageIntoSequence(event);
      if (!seq.isEmpty()) {
        return resultsHandler.aggregateResults(processParts(seq, event), event);
      } else {
        logger.warn("Splitter returned no results. If this is not expected, please check your split expression");
        return event;
      }
    } else {
      return processNext(event);
    }
  }

  protected boolean isSplitRequired(Event event) {
    return true;
  }

  /**
   * Converts the event into a {@link MessageSequence} that will retrieve each of the event elements
   * 
   * @param event the event to split
   * @return a sequence of elements
   * @throws MuleException
   */
  protected abstract MessageSequence<?> splitMessageIntoSequence(Event event) throws MuleException;

  protected List<Event> processParts(MessageSequence<?> seq, Event originalEvent) throws MuleException {
    List<Event> resultEvents = new ArrayList<>();
    int correlationSequence = 0;
    MessageSequence<?> messageSequence = seq;
    if (batchSize > 1) {
      messageSequence = new PartitionedMessageSequence(seq, batchSize);
    }
    Integer count = messageSequence.size();
    Event lastResult = null;
    for (; messageSequence.hasNext();) {
      correlationSequence++;

      final Builder builder = Event.builder(originalEvent);

      propagateFlowVars(lastResult, builder);
      if (counterVariableName != null) {
        builder.addVariable(counterVariableName, correlationSequence);
      }

      builder.groupCorrelation(new GroupCorrelation(count, correlationSequence));
      initEventBuilder(messageSequence.next(), originalEvent, builder, resolvePropagatedFlowVars(lastResult));
      final Event event = builder.build();
      Event resultEvent = processNext(event);
      if (resultEvent != null) {
        resultEvents.add(resultEvent);
        lastResult = resultEvent;
      }
    }
    if (correlationSequence == 1) {
      logger.debug("Splitter only returned a single result. If this is not expected, please check your split expression");
    }
    return resultEvents;
  }

  protected Set<String> resolvePropagatedFlowVars(Event lastResult) {
    return emptySet();
  }

  protected void propagateFlowVars(Event previousResult, final Builder builder) {
    // Nothing to do
  }

  private void initEventBuilder(Object payload, Event originalEvent, Builder builder, Set<String> flowVarsFromLastResult) {
    if (payload instanceof EventBuilderConfigurer) {
      ((EventBuilderConfigurer) payload).configure(builder);
    } else if (payload instanceof Event) {
      final Event payloadAsEvent = (Event) payload;
      builder.message(payloadAsEvent.getMessage());
      for (String flowVarName : payloadAsEvent.getVariableNames()) {
        if (!flowVarsFromLastResult.contains(flowVarName)) {
          builder.addVariable(flowVarName, payloadAsEvent.getVariable(flowVarName).getValue(),
                              payloadAsEvent.getVariable(flowVarName).getDataType());
        }
      }
    } else if (payload instanceof InternalMessage) {
      builder.message((InternalMessage) payload);
    } else {
      builder.message(InternalMessage.builder(originalEvent.getMessage()).payload(payload).build());
    }
  }

  /**
   * Split the elements in groups of the specified size
   */
  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public void setCounterVariableName(String counterVariableName) {
    this.counterVariableName = counterVariableName;
  }
}
