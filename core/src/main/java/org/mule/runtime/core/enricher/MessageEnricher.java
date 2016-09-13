/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.enricher;

import static org.mule.runtime.core.message.DefaultEventBuilder.EventImplementation.setCurrentEvent;

import org.mule.runtime.core.NonBlockingVoidMuleEvent;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.runtime.core.api.processor.InternalMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorContainer;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.api.processor.MessageProcessors;
import org.mule.runtime.core.metadata.DefaultTypedValue;
import org.mule.runtime.core.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.processor.AbstractRequestResponseMessageProcessor;
import org.mule.runtime.core.processor.NonBlockingMessageProcessor;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChain;
import org.mule.runtime.core.processor.chain.InterceptingChainLifecycleWrapper;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.runtime.core.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The <code>Message Enricher</code> allows the current message to be augmented using data from a seperate resource.
 * <p/>
 * The Mule implementation the <i>Enrichment Resource</i> can be any Message Processor. This allows you to not only use a JDBC
 * endpoint directly but also call out to a remote service via HTTP or even reference another flow or sub-flow.
 * <p/>
 * The Message Processor that implements the <i>Enrichment Resource</i> is invoked with a copy of the current message along with
 * any flow or session variables that are present. Invocation of the this message processor is done in a separate context to the
 * main flow such that any modification to the message (and it's properties and attachments) or flow or session variables will not
 * be reflected in the flow where the enricher is configured.
 * <p/>
 * The <i>Enrichment Resource</i> should always return a result. If it doesn't then the Enricher will simply leave the message
 * untouched.
 * <p/>
 * The way in which the message is enriched (or modified) is by explicitly configuring mappings (source -> target) between the
 * result from the Enrichment Resource and the message using of Mule Expressions. Mule Expressions are used to both select the
 * value to be extracted from result that comes back from the enrichment resource (source) and to define where this value to be
 * inserted into the message (target). The default 'source' if it's not configured is the payload of the result from the
 * enrichment resource..
 * <p/>
 * <b>EIP Reference:</b> <a href="http://eaipatterns.com/DataEnricher.html">http://eaipatterns.com/DataEnricher.html<a/>
 */
public class MessageEnricher extends AbstractMessageProcessorOwner implements NonBlockingMessageProcessor {

  private List<EnrichExpressionPair> enrichExpressionPairs = new ArrayList<>();

  private Processor enrichmentProcessor;

  @Override
  public Event process(Event event) throws MuleException {
    return new EnricherProcessor(enrichmentProcessor, muleContext).process(event);
  }

  protected Event enrich(Event currentEvent,
                         Event enrichmentEvent,
                         String sourceExpressionArg,
                         String targetExpressionArg,
                         ExpressionLanguage expressionLanguage) {
    if (StringUtils.isEmpty(sourceExpressionArg)) {
      sourceExpressionArg = "#[payload:]";
    }

    DefaultTypedValue typedValue = expressionLanguage.evaluateTyped(sourceExpressionArg, enrichmentEvent, flowConstruct);

    if (typedValue.getValue() instanceof InternalMessage) {
      InternalMessage muleMessage = (InternalMessage) typedValue.getValue();
      typedValue = new DefaultTypedValue(muleMessage.getPayload().getValue(), muleMessage.getPayload().getDataType());
    }

    if (!StringUtils.isEmpty(targetExpressionArg)) {
      Event.Builder eventBuilder = Event.builder(currentEvent);
      expressionLanguage.enrich(targetExpressionArg, currentEvent, eventBuilder, flowConstruct, typedValue);
      return eventBuilder.build();
    } else {
      return Event.builder(currentEvent).message(InternalMessage.builder(currentEvent.getMessage())
          .payload(typedValue.getValue()).mediaType(typedValue.getDataType().getMediaType()).build()).build();
    }
  }

  public void setEnrichmentMessageProcessor(Processor enrichmentProcessor) {
    if (!(enrichmentProcessor instanceof MessageProcessorChain)) {
      this.enrichmentProcessor = MessageProcessors.singletonChain(muleContext, enrichmentProcessor);
    } else {
      this.enrichmentProcessor = enrichmentProcessor;
    }

    if (this.enrichmentProcessor instanceof DefaultMessageProcessorChain) {
      ((DefaultMessageProcessorChain) this.enrichmentProcessor).setTemplateMuleContext(muleContext);
    }
  }

  /**
   * For spring
   */
  public void setMessageProcessor(Processor enrichmentProcessor) {
    setEnrichmentMessageProcessor(enrichmentProcessor);
  }

  public void setEnrichExpressionPairs(List<EnrichExpressionPair> enrichExpressionPairs) {
    this.enrichExpressionPairs = enrichExpressionPairs;
  }

  public void addEnrichExpressionPair(EnrichExpressionPair pair) {
    this.enrichExpressionPairs.add(pair);
  }

  public static class EnrichExpressionPair {

    private String source;
    private String target;

    public EnrichExpressionPair() {
      // for spring
    }

    public EnrichExpressionPair(String target) {
      this.target = target;
    }

    public EnrichExpressionPair(String source, String target) {
      this.source = source;
      this.target = target;
    }

    public String getSource() {
      return source;
    }

    public void setSource(String source) {
      this.source = source;
    }

    public String getTarget() {
      return target;
    }

    public void setTarget(String target) {
      this.target = target;
    }
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return Collections.singletonList(enrichmentProcessor);
  }

  @Override
  public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
    if (enrichmentProcessor instanceof InterceptingChainLifecycleWrapper) {
      super.addMessageProcessorPathElements(pathElement);
    } else {
      ((MessageProcessorContainer) enrichmentProcessor).addMessageProcessorPathElements(pathElement);
    }
  }

  /**
   * Enriches the current event using the result of processing the next message processor (the enrichment processor) and the
   * configured enrichment pairs.
   */
  private class EnricherProcessor extends AbstractRequestResponseMessageProcessor implements InternalMessageProcessor {

    private Event eventToEnrich;

    protected EnricherProcessor(Processor enrichmentProcessor, MuleContext muleContext) {
      this.next = enrichmentProcessor;
      this.muleContext = muleContext;
    }

    @Override
    protected Event processBlocking(Event event) throws MuleException {
      this.eventToEnrich = event;
      return super.processBlocking(copyEventForEnrichment(event));
    }

    @Override
    protected Event processNonBlocking(Event event) throws MuleException {
      this.eventToEnrich = event;
      Event result =
          processNext(copyEventForEnrichment(Event.builder(event).replyToHandler(createReplyToHandler(event)).build()));
      if (!(result instanceof NonBlockingVoidMuleEvent)) {
        result = processResponse(result, event);
      }
      return result;
    }

    private Event copyEventForEnrichment(Event event) {
      Event copy = Event.builder(event).session(new DefaultMuleSession(event.getSession())).build();
      setCurrentEvent(copy);
      return copy;
    }

    @Override
    protected Event processResponse(Event response, final Event request) throws MuleException {
      final ExpressionLanguage expressionLanguage = muleContext.getExpressionLanguage();

      if (response != null && !VoidMuleEvent.getInstance().equals(eventToEnrich)) {
        for (EnrichExpressionPair pair : enrichExpressionPairs) {
          eventToEnrich = enrich(eventToEnrich, response, pair.getSource(), pair.getTarget(), expressionLanguage);
        }
      }
      setCurrentEvent(eventToEnrich);
      return eventToEnrich;
    }

  }

  @Override
  public void setMuleContext(MuleContext context) {
    super.setMuleContext(context);
    if (this.enrichmentProcessor instanceof DefaultMessageProcessorChain) {
      ((DefaultMessageProcessorChain) this.enrichmentProcessor).setTemplateMuleContext(muleContext);
    }
  }
}
