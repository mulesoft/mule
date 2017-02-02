/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring.dsl.processor;


import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.enricher.MessageEnricher;
import org.mule.runtime.dsl.api.component.AbstractAnnotatedObjectFactory;
import org.mule.runtime.dsl.api.component.ObjectFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ObjectFactory} to create a
 * {@link org.mule.runtime.core.enricher.MessageEnricher}.
 */
public class MessageEnricherObjectFactory extends AbstractAnnotatedObjectFactory<MessageEnricher>
    implements MuleContextAware, FlowConstructAware {

  private Processor messageProcessor;
  private String source;
  private String target;
  private List<MessageEnricher.EnrichExpressionPair> enrichExpressionPairs = new ArrayList<>();
  private FlowConstruct flowConstruct;
  private MuleContext muleContext;

  public void setMessageProcessor(Processor messageProcessor) {
    this.messageProcessor = messageProcessor;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public void setEnrichExpressionPairs(List<MessageEnricher.EnrichExpressionPair> enrichExpressionPairs) {
    this.enrichExpressionPairs = enrichExpressionPairs;
  }

  @Override
  public MessageEnricher doGetObject() {
    MessageEnricher messageEnricher = new MessageEnricher();
    if (target != null || source != null) {
      messageEnricher.addEnrichExpressionPair(new MessageEnricher.EnrichExpressionPair(source, target));
    }
    for (MessageEnricher.EnrichExpressionPair enrichExpressionPair : enrichExpressionPairs) {
      messageEnricher.addEnrichExpressionPair(enrichExpressionPair);
    }
    messageEnricher.setFlowConstruct(flowConstruct);
    messageEnricher.setMuleContext(muleContext);
    messageEnricher.setMessageProcessor(messageProcessor);
    return messageEnricher;
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

}
