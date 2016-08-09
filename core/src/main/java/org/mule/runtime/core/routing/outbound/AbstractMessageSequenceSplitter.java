/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.outbound;

import static org.mule.runtime.core.routing.CorrelationMode.NEVER;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleMessage.Builder;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.routing.RouterResultsHandler;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;
import org.mule.runtime.core.routing.AbstractSplitter;
import org.mule.runtime.core.routing.CorrelationMode;
import org.mule.runtime.core.routing.DefaultRouterResultsHandler;
import org.mule.runtime.core.routing.MessageSequence;

import java.util.ArrayList;
import java.util.List;

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
public abstract class AbstractMessageSequenceSplitter extends AbstractInterceptingMessageProcessor implements MuleContextAware {

  protected MuleContext muleContext;
  protected RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();
  protected CorrelationMode enableCorrelation = CorrelationMode.IF_NOT_SET;
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
    String correlationId = originalEvent.getMessage().getCorrelation().getId().orElse(originalEvent.getMessage().getUniqueId());
    List<MuleEvent> resultEvents = new ArrayList<>();
    int correlationSequence = 0;
    MessageSequence<?> messageSequence = seq;
    if (batchSize > 1) {
      messageSequence = new PartitionedMessageSequence(seq, batchSize);
    }
    Integer count = messageSequence.size();
    for (; messageSequence.hasNext();) {
      MuleEvent event = createEvent(messageSequence.next(), originalEvent);

      correlationSequence++;
      if (counterVariableName != null) {
        originalEvent.setFlowVariable(counterVariableName, correlationSequence);
      }

      final Builder builder = MuleMessage.builder(event.getMessage());

      if (enableCorrelation.doCorrelation(event.getMessage().getCorrelation())) {
        builder.correlationId(correlationId);
      }
      if (enableCorrelation != NEVER) {
        // take correlation group size from the message properties, set by concrete message splitter
        // implementations
        builder.correlationGroupSize(count);
        builder.correlationSequence(correlationSequence);
      }
      builder.rootId(originalEvent.getMessage().getMessageRootId());

      event.setMessage(builder.build());

      MuleEvent resultEvent = processNext(RequestContext.setEvent(event));
      if (resultEvent != null && !VoidMuleEvent.getInstance().equals(resultEvent)) {
        resultEvents.add(resultEvent);
      }
    }
    if (correlationSequence == 1) {
      logger.debug("Splitter only returned a single result. If this is not expected, please check your split expression");
    }
    return resultEvents;
  }

  private MuleEvent createEvent(Object payload, MuleEvent originalEvent) {
    if (payload instanceof MuleEvent) {
      return new DefaultMuleEvent(((MuleEvent) payload).getMessage(), originalEvent);
    } else if (payload instanceof MuleMessage) {
      return new DefaultMuleEvent((MuleMessage) payload, originalEvent);
    } else {
      return new DefaultMuleEvent(MuleMessage.builder(originalEvent.getMessage()).payload(payload).build(), originalEvent);
    }
  }

  public void setEnableCorrelation(CorrelationMode enableCorrelation) {
    this.enableCorrelation = enableCorrelation;
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
