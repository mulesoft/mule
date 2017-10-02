/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.outbound;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApplyWithChildContext;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.privileged.event.Acceptor;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.event.CoreEvent.Builder;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.internal.routing.MessageSequence;
import org.mule.runtime.core.privileged.routing.RouterResultsHandler;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.routing.AbstractSplitter;
import org.mule.runtime.core.privileged.processor.AbstractInterceptingMessageProcessor;
import org.mule.runtime.core.privileged.routing.DefaultRouterResultsHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Base implementation of a {@link Message} splitter, that converts its payload in a {@link MessageSequence}, and process each
 * element of it. Implementations must implement {@link #splitMessageIntoSequence(CoreEvent)} and determine how the message is split.
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
  protected Acceptor filterOnErrorTypeAcceptor = new Acceptor() {

    @Override
    public boolean acceptsAll() {
      return false;
    }

    @Override
    public boolean accept(CoreEvent event) {
      return false;
    }
  };

  @Override
  public final CoreEvent process(CoreEvent event) throws MuleException {
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

  protected boolean isSplitRequired(CoreEvent event) {
    return true;
  }

  /**
   * Converts the event into a {@link MessageSequence} that will retrieve each of the event elements
   * 
   * @param event the event to split
   * @return a sequence of elements
   * @throws MuleException
   */
  protected abstract MessageSequence<?> splitMessageIntoSequence(CoreEvent event) throws MuleException;

  protected List<CoreEvent> processParts(MessageSequence<?> seq, CoreEvent originalEvent) throws MuleException {
    List<CoreEvent> resultEvents = new ArrayList<>();
    int correlationSequence = 0;
    MessageSequence<?> messageSequence = seq;
    if (batchSize > 1) {
      messageSequence = new PartitionedMessageSequence<>(seq, batchSize);
    }
    Integer count = messageSequence.size();
    CoreEvent lastResult = null;
    for (; messageSequence.hasNext();) {
      correlationSequence++;

      final Builder builder = builder(originalEvent);

      propagateFlowVars(lastResult, builder);
      if (counterVariableName != null) {
        builder.addVariable(counterVariableName, correlationSequence);
      }

      builder.groupCorrelation(Optional
          .of(count != null ? GroupCorrelation.of(correlationSequence, count) : GroupCorrelation.of(correlationSequence)));
      initEventBuilder(messageSequence.next(), originalEvent, builder, resolvePropagatedFlowVars(lastResult));

      try {
        // TODO MULE-13052 Migrate Splitter and Foreach implementation to non-blocking
        CoreEvent resultEvent = processToApplyWithChildContext(builder.build(), applyNext());
        if (resultEvent != null) {
          resultEvents.add(builder(originalEvent.getContext(), resultEvent).build());
          lastResult = resultEvent;
        }
      } catch (MessagingException e) {
        if (!filterOnErrorTypeAcceptor.accept(e.getEvent())) {
          throw e;
        }
      }
    }
    if (correlationSequence == 1) {
      logger.debug("Splitter only returned a single result. If this is not expected, please check your split expression");
    }
    return resultEvents;
  }

  protected Map<String, ?> resolvePropagatedFlowVars(CoreEvent lastResult) {
    return emptyMap();
  }

  protected void propagateFlowVars(CoreEvent previousResult, final Builder builder) {
    // Nothing to do
  }

  private void initEventBuilder(Object sequenceValue, CoreEvent originalEvent, Builder builder,
                                Map<String, ?> flowVarsFromLastResult) {
    if (sequenceValue instanceof EventBuilderConfigurer) {
      ((EventBuilderConfigurer) sequenceValue).configure(builder);
    } else if (sequenceValue instanceof CoreEvent) {
      final CoreEvent payloadAsEvent = (CoreEvent) sequenceValue;
      builder.message(payloadAsEvent.getMessage());
      for (String flowVarName : payloadAsEvent.getVariables().keySet()) {
        if (!flowVarsFromLastResult.containsKey(flowVarName)) {
          builder.addVariable(flowVarName, payloadAsEvent.getVariables().get(flowVarName).getValue(),
                              payloadAsEvent.getVariables().get(flowVarName).getDataType());
        }
      }
    } else if (sequenceValue instanceof Message) {
      final Message message = (Message) sequenceValue;
      builder.message(message);
    } else if (sequenceValue instanceof TypedValue) {
      builder.message(Message.builder().payload((TypedValue) sequenceValue).build());
    } else if (sequenceValue instanceof Collection) {
      builder.message(Message.builder(originalEvent.getMessage()).value(((Collection) sequenceValue).stream()
          .map(v -> v instanceof TypedValue ? ((TypedValue) v).getValue() : v).collect(toList())).build());
    } else {
      builder.message(Message.builder(originalEvent.getMessage()).value(sequenceValue).build());
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
