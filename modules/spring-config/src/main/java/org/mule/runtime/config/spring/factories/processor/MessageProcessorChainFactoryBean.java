/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories.processor;

import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.processor.chain.ExplicitMessageProcessorChainBuilder;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

public class MessageProcessorChainFactoryBean extends AbstractAnnotatedObject implements FactoryBean, MuleContextAware {

  protected List processors;
  protected String name;
  protected MuleContext muleContext;

  @Override
  public Class getObjectType() {
    return Processor.class;
  }

  public void setMessageProcessors(List processors) {
    this.processors = processors;
  }

  @Override
  public Object getObject() throws Exception {
    MessageProcessorChainBuilder builder = getBuilderInstance();
    for (Object processor : processors) {
      if (processor instanceof Processor) {
        builder.chain((Processor) processor);
      } else if (processor instanceof MessageProcessorBuilder) {
        builder.chain((MessageProcessorBuilder) processor);
      } else {
        throw new IllegalArgumentException("MessageProcessorBuilder should only have MessageProcessor's or MessageProcessorBuilder's configured");
      }
    }
    MessageProcessorChain messageProcessorChain = builder.build();
    messageProcessorChain.setMuleContext(muleContext);
    messageProcessorChain.setAnnotations(getAnnotations());
    return messageProcessorChain;
  }

  protected MessageProcessorChainBuilder getBuilderInstance() {
    ExplicitMessageProcessorChainBuilder builder = new ExplicitMessageProcessorChainBuilder();
    builder.setName("processor chain '" + name + "'");
    return builder;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

}
