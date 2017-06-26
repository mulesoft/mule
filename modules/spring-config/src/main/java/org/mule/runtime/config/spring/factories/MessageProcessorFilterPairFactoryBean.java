/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories;

import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.routing.MessageProcessorExpressionPair;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

public class MessageProcessorFilterPairFactoryBean extends AbstractAnnotatedObject
    implements FactoryBean<MessageProcessorExpressionPair> {

  private String expression = "true";
  private List<Processor> messageProcessors;

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public void setMessageProcessors(List<Processor> messageProcessors) {
    this.messageProcessors = messageProcessors;
  }

  @Override
  public MessageProcessorExpressionPair getObject() throws Exception {
    MessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    for (Object processor : messageProcessors) {
      if (processor instanceof Processor) {
        builder.chain((Processor) processor);
      } else if (processor instanceof MessageProcessorBuilder) {
        builder.chain((MessageProcessorBuilder) processor);
      } else {
        throw new IllegalArgumentException("MessageProcessorBuilder should only have MessageProcessors or MessageProcessorBuilders configured");
      }
    }

    MessageProcessorExpressionPair filterPair = new MessageProcessorExpressionPair(expression, builder.build());
    filterPair.setAnnotations(getAnnotations());
    return filterPair;
  }

  @Override
  public Class<MessageProcessorExpressionPair> getObjectType() {
    return MessageProcessorExpressionPair.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
