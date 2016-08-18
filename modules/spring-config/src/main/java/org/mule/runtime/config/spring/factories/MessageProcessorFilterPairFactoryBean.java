/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.routing.MessageProcessorFilterPair;
import org.mule.runtime.core.routing.filters.AcceptAllFilter;
import org.mule.runtime.core.routing.filters.ExpressionFilter;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

public class MessageProcessorFilterPairFactoryBean implements FactoryBean<MessageProcessorFilterPair>, MuleContextAware {

  private List<MessageProcessor> messageProcessors;
  private Filter filter = new ExpressionFilter();
  private MuleContext muleContext;

  public void setFilter(Filter filter) {
    this.filter = filter;
  }

  public void setMessageProcessors(List<MessageProcessor> messageProcessors) {
    this.messageProcessors = messageProcessors;
  }

  public void setExpression(String expression) {
    ((ExpressionFilter) filter).setExpression(expression);
  }

  @Override
  public MessageProcessorFilterPair getObject() throws Exception {
    MessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder(muleContext);
    for (Object processor : messageProcessors) {
      if (processor instanceof MessageProcessor) {
        builder.chain((MessageProcessor) processor);
      } else if (processor instanceof MessageProcessorBuilder) {
        builder.chain((MessageProcessorBuilder) processor);
      } else {
        throw new IllegalArgumentException("MessageProcessorBuilder should only have MessageProcessors or MessageProcessorBuilders configured");
      }
    }

    return createFilterPair(builder);
  }

  private MessageProcessorFilterPair createFilterPair(MessageProcessorChainBuilder builder) throws Exception {
    if (filter == null) {
      return new MessageProcessorFilterPair(builder.build(), AcceptAllFilter.INSTANCE);
    } else {
      return new MessageProcessorFilterPair(builder.build(), filter);
    }
  }

  @Override
  public Class<MessageProcessorFilterPair> getObjectType() {
    return MessageProcessorFilterPair.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    if (filter != null && filter instanceof MuleContextAware) {
      ((MuleContextAware) filter).setMuleContext(context);
    }
    this.muleContext = context;
  }
}
