/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.enricher;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.rx.Exceptions.checkedFunction;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.setCurrentEvent;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.core.privileged.event.DefaultMuleSession;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.processor.Scope;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
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
public class MessageEnricher extends AbstractMessageProcessorOwner implements Scope {

  private List<EnrichExpressionPair> enrichExpressionPairs = new ArrayList<>();

  private Processor enrichmentProcessor;

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  protected PrivilegedEvent enrich(PrivilegedEvent currentEvent,
                                   PrivilegedEvent enrichmentEvent,
                                   String sourceExpressionArg,
                                   String targetExpressionArg,
                                   ExtendedExpressionManager expressionManager) {
    if (StringUtils.isEmpty(sourceExpressionArg)) {
      sourceExpressionArg = "#[mel:payload:]";
    }

    TypedValue typedValue = expressionManager.evaluate(sourceExpressionArg, enrichmentEvent, getLocation());

    if (typedValue.getValue() instanceof Message) {
      Message muleMessage = (Message) typedValue.getValue();
      typedValue = new TypedValue(muleMessage.getPayload().getValue(), muleMessage.getPayload().getDataType());
    }

    if (!StringUtils.isEmpty(targetExpressionArg)) {
      PrivilegedEvent.Builder eventBuilder = PrivilegedEvent.builder(currentEvent);
      expressionManager.enrich(targetExpressionArg, currentEvent, eventBuilder, getLocation(), typedValue);
      return eventBuilder.build();
    } else {
      return PrivilegedEvent.builder(currentEvent).message(Message.builder(currentEvent.getMessage())
          .value(typedValue.getValue()).mediaType(typedValue.getDataType().getMediaType()).build()).build();
    }
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return from(publisher)
        .cast(PrivilegedEvent.class)
        // Use flatMap and child context in order to handle null response and do nothing rather than complete response as empty
        // if enrichment processor drops event due to a filter for example.
        .flatMap(event -> from(processWithChildContext(PrivilegedEvent.builder(event)
            .session(new DefaultMuleSession(event.getSession()))
            .build(), enrichmentProcessor, ofNullable(getLocation())))
                .cast(PrivilegedEvent.class)
                .map(checkedFunction(response -> enrich(response, event)))
                .defaultIfEmpty(event));
  }

  protected PrivilegedEvent enrich(final PrivilegedEvent event, PrivilegedEvent eventToEnrich) throws MuleException {
    final ExtendedExpressionManager expressionManager = muleContext.getExpressionManager();

    if (event != null) {
      for (EnrichExpressionPair pair : enrichExpressionPairs) {
        eventToEnrich = enrich(eventToEnrich, event, pair.getSource(), pair.getTarget(), expressionManager);
      }
    }
    setCurrentEvent(eventToEnrich);
    return eventToEnrich;
  }

  @Override
  public void initialise() throws InitialisationException {
    enrichmentProcessor = newChain(getProcessingStrategy(locator, getRootContainerLocation()),
                                   enrichmentProcessor);
    initialiseIfNeeded(this.enrichmentProcessor, muleContext);
  }

  public void setEnrichmentMessageProcessor(Processor enrichmentProcessor) {
    this.enrichmentProcessor = enrichmentProcessor;
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

  public static class EnrichExpressionPair extends AbstractComponent {

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

}
