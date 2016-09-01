/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.outbound;

import static java.util.Collections.emptySet;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleEvent.Builder;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.routing.RouterResultsHandler;
import org.mule.runtime.core.message.Correlation;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;
import org.mule.runtime.core.routing.AbstractSplitter;
import org.mule.runtime.core.routing.DefaultRouterResultsHandler;
import org.mule.runtime.core.routing.MessageSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Base implementation of a {@link MuleMessage} splitter, that converts its payload in a {@link MessageSequence}, and process each
 * element of it. Implementations must implement {@link #splitMessageIntoSequence(MuleEvent)} and determine how the message is
 * split.
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/Sequencer.html">http://www .eaipatterns.com/Sequencer.html</a>
 * 
 * @author flbulgarelli
 * @see AbstractSplitter
 */
public abstract class AbstractMessageSequenceSplitter extends AbstractInterceptingMessageProcessor
    implements MuleContextAware {

  protected MuleContext muleContext;
  protected RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();
  protected int batchSize;
  protected String counterVariableName;

  @Override
  public final MuleEvent process(MuleEvent event) throws MuleException {
    if (isSplitRequired(event)) {
      MessageSequence<?> seq = splitMessageIntoSequence(event);
      if (!seq.isEmpty()) {
        MuleEvent aggregatedResults = resultsHandler.aggregateResults(processParts(seq, event), event);
        if (aggregatedResults instanceof VoidMuleEvent) {
          return null;
        } else {
          return aggregatedResults;
        }
      } else {
        logger.warn("Splitter returned no results. If this is not expected, please check your split expression");
        return VoidMuleEvent.getInstance();
      }
    } else {
      return processNext(event);
    }
  }

  protected boolean isSplitRequired(MuleEvent event) {
    return true;
  }

  /**
   * Converts the event into a {@link MessageSequence} that will retrieve each of the event elements
   * 
   * @param event the event to split
   * @return a sequence of elements
   * @throws MuleException
   */
  protected abstract MessageSequence<?> splitMessageIntoSequence(MuleEvent event) throws MuleException;

  protected List<MuleEvent> processParts(MessageSequence<?> seq, MuleEvent originalEvent) throws MuleException {
    List<MuleEvent> resultEvents = new ArrayList<>();
    int correlationSequence = 0;
    MessageSequence<?> messageSequence = seq;
    if (batchSize > 1) {
      messageSequence = new PartitionedMessageSequence(seq, batchSize);
    }
    Integer count = messageSequence.size();
    MuleEvent lastResult = null;
    for (; messageSequence.hasNext();) {
      correlationSequence++;

      final Builder builder = MuleEvent.builder(originalEvent);

      propagateFlowVars(lastResult, builder);
      if (counterVariableName != null) {
        builder.addFlowVariable(counterVariableName, correlationSequence);
      }

      builder.correlation(new Correlation(count, correlationSequence));
      initEventBuilder(messageSequence.next(), originalEvent, builder, resolvePropagatedFlowVars(lastResult));
      final MuleEvent event = builder.build();
      ((DefaultMuleEvent) event).setParent(originalEvent);
      MuleEvent resultEvent = processNext(event);
      if (resultEvent != null && !VoidMuleEvent.getInstance().equals(resultEvent)) {
        resultEvents.add(resultEvent);
        lastResult = resultEvent;
      }
    }
    if (correlationSequence == 1) {
      logger.debug("Splitter only returned a single result. If this is not expected, please check your split expression");
    }
    return resultEvents;
  }

  protected Set<String> resolvePropagatedFlowVars(MuleEvent lastResult) {
    return emptySet();
  }

  protected void propagateFlowVars(MuleEvent previousResult, final Builder builder) {
    // Nothing to do
  }

  private void initEventBuilder(Object payload, MuleEvent originalEvent, Builder builder, Set<String> flowVarsFromLastResult) {
    if (payload instanceof EventBuilderConfigurer) {
      ((EventBuilderConfigurer) payload).configure(builder);
    } else if (payload instanceof MuleEvent) {
      final MuleEvent payloadAsEvent = (MuleEvent) payload;
      builder.message(payloadAsEvent.getMessage());
      for (String flowVarName : payloadAsEvent.getFlowVariableNames()) {
        if (!flowVarsFromLastResult.contains(flowVarName)) {
          builder.addFlowVariable(flowVarName, payloadAsEvent.getFlowVariable(flowVarName),
                                  payloadAsEvent.getFlowVariableDataType(flowVarName));
        }
      }
    } else if (payload instanceof MuleMessage) {
      builder.message((MuleMessage) payload);
    } else {
      builder.message(MuleMessage.builder(originalEvent.getMessage()).payload(payload).build());
    }
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
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
