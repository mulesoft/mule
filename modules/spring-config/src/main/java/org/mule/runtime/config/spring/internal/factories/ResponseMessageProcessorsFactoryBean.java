/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.internal.factories;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.processor.ResponseMessageProcessorAdapter;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.dsl.api.component.AbstractAnnotatedObjectFactory;

import java.util.List;

import javax.inject.Inject;

public class ResponseMessageProcessorsFactoryBean extends AbstractAnnotatedObjectFactory<ResponseMessageProcessorAdapter> {

  @Inject
  private MuleContext muleContext;

  @Inject
  private Registry registry;

  protected List messageProcessors;

  public void setMessageProcessors(List messageProcessors) {
    this.messageProcessors = messageProcessors;
  }

  @Override
  public ResponseMessageProcessorAdapter doGetObject() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    //builder.setProcessingStrategy(flowConstruct.getProcessingStrategy());
    builder.setName("'response' child processor chain");
    for (Object processor : messageProcessors) {
      if (processor instanceof Processor) {
        builder.chain((Processor) processor);
      } else if (processor instanceof MessageProcessorBuilder) {
        builder.chain((MessageProcessorBuilder) processor);
      } else {
        throw new IllegalArgumentException("MessageProcessorBuilder should only have MessageProcessor's or MessageProcessorBuilder's configured");
      }
    }
    ResponseMessageProcessorAdapter responseAdapter = new ResponseMessageProcessorAdapter();
    responseAdapter.setProcessor(new Processor() {

      private Processor processor;

      @Override
      public InternalEvent process(InternalEvent event) throws MuleException {
        if (processor == null) {
          FlowConstruct flowConstruct = (FlowConstruct) registry.lookupByName(getRootContainerName()).get();
          builder.setProcessingStrategy(flowConstruct.getProcessingStrategy());
          processor = builder.build();
          if (processor instanceof MuleContextAware) {
            ((MuleContextAware) processor).setMuleContext(muleContext);
          }
          if (processor instanceof Initialisable) {
            ((Initialisable) processor).initialise();
          }
          if (processor instanceof Startable) {
            ((Startable) processor).start();
          }
        }
        return processor.process(event);
      }
    });
    responseAdapter.setMuleContext(muleContext);
    responseAdapter.setAnnotations(getAnnotations());
    return responseAdapter;
  }

}
