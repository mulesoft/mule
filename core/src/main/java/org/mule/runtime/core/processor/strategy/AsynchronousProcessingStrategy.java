/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.ProcessingStrategy;
import org.mule.runtime.core.processor.AsyncInterceptingMessageProcessor;

import java.util.List;

import javax.resource.spi.work.WorkManager;

/**
 * This strategy uses a {@link WorkManager} to schedule the processing of the pipeline of message processors in a single worker
 * thread.
 */
public class AsynchronousProcessingStrategy extends AbstractThreadingProfileProcessingStrategy {

  protected ProcessingStrategy synchronousProcessingStrategy = new SynchronousProcessingStrategy();

  @Override
  public void configureProcessors(List<MessageProcessor> processors,
                                  org.mule.runtime.core.api.processor.StageNameSource nameSource,
                                  MessageProcessorChainBuilder chainBuilder, MuleContext muleContext) {
    if (processors.size() > 0) {
      chainBuilder.chain(createAsyncMessageProcessor(nameSource, muleContext));
      synchronousProcessingStrategy.configureProcessors(processors, nameSource, chainBuilder, muleContext);
    }
  }

  protected AsyncInterceptingMessageProcessor createAsyncMessageProcessor(org.mule.runtime.core.api.processor.StageNameSource nameSource,
                                                                          MuleContext muleContext) {
    return new AsyncInterceptingMessageProcessor(createThreadingProfile(muleContext),
                                                 getThreadPoolName(nameSource.getName(), muleContext),
                                                 muleContext.getConfiguration().getShutdownTimeout());
  }

}
