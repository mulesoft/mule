/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.privileged.dsl.processor;

import static java.lang.String.format;
import static org.mule.runtime.core.api.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder.newLazyProcessorChainBuilder;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

import java.util.List;

public class MessageProcessorChainFactoryBean extends AbstractComponentFactory<MessageProcessorChain>
    implements MuleContextAware {

  protected List processors;
  protected String name;
  protected MuleContext muleContext;

  public void setMessageProcessors(List processors) {
    this.processors = processors;
  }

  @Override
  public MessageProcessorChain doGetObject() throws Exception {
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
    return newLazyProcessorChainBuilder((DefaultMessageProcessorChainBuilder) builder,
                                        muleContext,
                                        () -> getProcessingStrategy(muleContext, getRootContainerName()).orElse(null));
  }

  protected MessageProcessorChainBuilder getBuilderInstance() {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
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
