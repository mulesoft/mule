/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.policy;

import static org.mule.runtime.api.notification.PolicyNotification.PROCESS_END;
import static org.mule.runtime.api.notification.PolicyNotification.PROCESS_START;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.policy.PolicyNotificationHelper;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import org.reactivestreams.Publisher;

import java.util.List;

import javax.inject.Inject;

import reactor.core.publisher.Mono;

/**
 * Policy chain for handling the message processor associated to a policy.
 *
 * @since 4.0
 */
public class PolicyChain extends AbstractComponent
    implements Initialisable, Startable, Stoppable, Disposable, Processor {

  @Inject
  private MuleContext muleContext;

  private ProcessingStrategy processingStrategy;

  private List<Processor> processors;
  private MessageProcessorChain processorChain;

  private ReactiveProcessor chainWithPs;
  private PolicyNotificationHelper notificationHelper;

  public void setProcessors(List<Processor> processors) {
    this.processors = processors;
  }

  @Override
  public final void initialise() throws InitialisationException {
    DefaultMessageProcessorChainBuilder chainBuilder = new DefaultMessageProcessorChainBuilder().chain(processors);

    chainBuilder.setProcessingStrategy(processingStrategy);
    processorChain = chainBuilder.build();
    chainWithPs = processingStrategy.onPipeline(processorChain);
    initialiseIfNeeded(processorChain, muleContext);

    notificationHelper =
        new PolicyNotificationHelper(muleContext.getNotificationManager(), muleContext.getConfiguration().getId(), this);
  }

  public void setProcessingStrategy(ProcessingStrategy processingStrategy) {
    this.processingStrategy = processingStrategy;
  }

  @Override
  public void start() throws MuleException {
    if (processorChain != null) {
      processorChain.start();
    }
  }

  @Override
  public void dispose() {
    if (processorChain != null) {
      processorChain.dispose();
    }
  }

  @Override
  public void stop() throws MuleException {
    if (processorChain != null) {
      processorChain.stop();
    }
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, chainWithPs);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return Mono.from(publisher)
        .doOnNext(notificationHelper.notification(PROCESS_START))
        .transform(chainWithPs)
        .doOnSuccess(notificationHelper.notification(PROCESS_END))
        .doOnError(MessagingException.class, notificationHelper.errorNotification(PROCESS_END));
  }

}
