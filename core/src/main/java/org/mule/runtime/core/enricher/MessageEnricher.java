/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.enricher;

import static java.util.Collections.singletonList;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;
import static org.mule.runtime.core.api.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.util.rx.Exceptions.checkedFunction;
import static reactor.core.publisher.Flux.from;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.metadata.DefaultTypedValue;
import org.mule.runtime.core.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.runtime.core.util.NotificationUtils;
import org.mule.runtime.core.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

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
public class MessageEnricher extends AbstractMessageProcessorOwner implements Processor {

  private List<EnrichExpressionPair> enrichExpressionPairs = new ArrayList<>();

  private Processor enrichmentProcessor;

  @Override
  public Event process(Event event) throws MuleException {
    return enrich(enrichmentProcessor.process(Event.builder(event).session(new DefaultMuleSession(event.getSession())).build()),
                  event);
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

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    return from(publisher).flatMap(event -> Mono.just(event)
        .map(event1 -> Event.builder(event).session(new DefaultMuleSession(event.getSession())).build())
        .transform(enrichmentProcessor).map(checkedFunction(response -> enrich(response, event)))
        .otherwiseIfEmpty(Mono.just(event)));
  }

  protected Event enrich(final Event event, Event eventToEnrich) throws MuleException {
    final ExpressionLanguage expressionLanguage = muleContext.getExpressionLanguage();

    if (event != null) {
      for (EnrichExpressionPair pair : enrichExpressionPairs) {
        eventToEnrich = enrich(eventToEnrich, event, pair.getSource(), pair.getTarget(), expressionLanguage);
      }
    }
    setCurrentEvent(eventToEnrich);
    return eventToEnrich;
  }

  public void setEnrichmentMessageProcessor(Processor enrichmentProcessor) {
    this.enrichmentProcessor = newChain(enrichmentProcessor);
    setMuleContextIfNeeded(this.enrichmentProcessor, muleContext);
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
    return singletonList(enrichmentProcessor);
  }

  @Override
  public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
    NotificationUtils.addMessageProcessorPathElements(enrichmentProcessor, pathElement.addChild(this));
  }

}
