/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories;

import org.mule.runtime.api.meta.NameableObject;
import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.ProcessingStrategy;
import org.mule.runtime.core.processor.AsyncDelegateMessageProcessor;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

public class AsyncMessageProcessorsFactoryBean extends AbstractAnnotatedObject
    implements FactoryBean, MuleContextAware, NameableObject {

  protected MuleContext muleContext;

  protected List messageProcessors;
  protected ProcessingStrategy processingStrategy;
  protected String name;

  @Override
  public Class getObjectType() {
    return MessageProcessor.class;
  }

  public void setMessageProcessors(List messageProcessors) {
    this.messageProcessors = messageProcessors;
  }

  @Override
  public Object getObject() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder(muleContext);
    builder.setName("'async' child chain");

    for (Object processor : messageProcessors) {
      if (processor instanceof MessageProcessor) {
        builder.chain((MessageProcessor) processor);
      } else if (processor instanceof MessageProcessorBuilder) {
        builder.chain((MessageProcessorBuilder) processor);
      } else {
        throw new IllegalArgumentException("MessageProcessorBuilder should only have MessageProcessor's or MessageProcessorBuilder's configured");
      }
    }
    AsyncDelegateMessageProcessor delegate = new AsyncDelegateMessageProcessor(builder.build(), processingStrategy, name);
    delegate.setAnnotations(getAnnotations());
    return delegate;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  public void setProcessingStrategy(ProcessingStrategy processingStrategy) {
    this.processingStrategy = processingStrategy;
  }
}
