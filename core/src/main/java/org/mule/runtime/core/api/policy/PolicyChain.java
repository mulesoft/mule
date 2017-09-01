/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.policy;

import static reactor.core.publisher.Mono.from;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.AbstractComponent;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;

import org.reactivestreams.Publisher;

import java.util.List;

import javax.inject.Inject;

/**
 * Policy chain for handling the message processor associated to a policy.
 *
 * @since 4.0
 */
public class PolicyChain extends AbstractComponent
    implements Initialisable, Startable, Stoppable, Disposable, Processor {

  @Inject
  private MuleContext muleContext;

  private List<Processor> processors;
  private MessageProcessorChain processorChain;

  public void setProcessors(List<Processor> processors) {
    this.processors = processors;
  }

  @Override
  public final void initialise() throws InitialisationException {
    processorChain = new DefaultMessageProcessorChainBuilder().chain(this.processors).build();
    processorChain.setMuleContext(muleContext);
    processorChain.initialise();
  }

  @Override
  public void start() throws MuleException {
    processorChain.start();
  }

  @Override
  public void dispose() {
    processorChain.dispose();
  }

  @Override
  public void stop() throws MuleException {
    processorChain.stop();
  }

  @Override
  public InternalEvent process(InternalEvent event) throws MuleException {
    return processorChain.process(event);
  }

  @Override
  public Publisher<InternalEvent> apply(Publisher<InternalEvent> publisher) {
    return from(publisher).transform(processorChain);
  }

}
