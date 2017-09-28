/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder.newLazyProcessorChainBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.privileged.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.processor.ResponseMessageProcessorAdapter;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

import java.util.List;

import javax.inject.Inject;

public class ResponseMessageProcessorsFactoryBean extends AbstractComponentFactory<ResponseMessageProcessorAdapter> {

  @Inject
  private MuleContext muleContext;

  protected List messageProcessors;

  public void setMessageProcessors(List messageProcessors) {
    this.messageProcessors = messageProcessors;
  }

  @Override
  public ResponseMessageProcessorAdapter doGetObject() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
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
    responseAdapter.setProcessor(newLazyProcessorChainBuilder(builder, muleContext,
                                                              () -> getProcessingStrategy(muleContext, getRootContainerName())
                                                                  .orElse(null)));
    responseAdapter.setMuleContext(muleContext);
    return responseAdapter;
  }

}
