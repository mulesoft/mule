/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.privileged.dsl.processor;

import static java.lang.String.format;

import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.processor.chain.ExplicitMessageProcessorChainBuilder;
import org.mule.runtime.dsl.api.component.ObjectFactory;

import java.util.List;

public class MessageProcessorChainFactoryBean extends AbstractAnnotatedObject
    implements ObjectFactory<MessageProcessorChain>, MuleContextAware {

  protected List processors;
  protected String name;
  protected MuleContext muleContext;

  public void setMessageProcessors(List processors) {
    this.processors = processors;
  }

  @Override
  public MessageProcessorChain getObject() throws Exception {
    MessageProcessorChainBuilder builder = getBuilderInstance();
    for (Object processor : processors) {
      if (processor instanceof Processor) {
        builder.chain((Processor) processor);
      } else if (processor instanceof MessageProcessorBuilder) {
        builder.chain((MessageProcessorBuilder) processor);
      } else {
        throw new IllegalArgumentException(format("MessageProcessorBuilder should only have MessageProcessor's or MessageProcessorBuilder's configured. Found a %s",
                                                  processor.getClass().getName()));
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

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

}
