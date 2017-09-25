/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static reactor.core.publisher.Flux.from;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.AbstractInterceptingMessageProcessor;

import org.reactivestreams.Publisher;

public class ResponseMessageProcessorAdapter extends AbstractInterceptingMessageProcessor
    implements Lifecycle {

  protected Processor responseProcessor;

  public ResponseMessageProcessorAdapter() {
    super();
  }

  public ResponseMessageProcessorAdapter(Processor responseProcessor) {
    super();
    this.responseProcessor = responseProcessor;
  }

  public void setProcessor(Processor processor) {
    this.responseProcessor = processor;
  }


  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    if (responseProcessor == null) {
      return publisher;
    } else {
      return from(publisher)
          .transform(applyNext())
          // Use flatMap and child context in order to handle null response and continue with current event
          .flatMap(event -> from(processWithChildContext(event, responseProcessor, ofNullable(getLocation())))
              .defaultIfEmpty(event));
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    if (responseProcessor instanceof MuleContextAware) {
      ((MuleContextAware) responseProcessor).setMuleContext(muleContext);
    }
    if (responseProcessor instanceof Initialisable) {
      ((Initialisable) responseProcessor).initialise();
    }
  }

  @Override
  public void start() throws MuleException {
    if (responseProcessor instanceof Startable) {
      ((Startable) responseProcessor).start();
    }
  }

  @Override
  public void stop() throws MuleException {
    if (responseProcessor instanceof Stoppable) {
      ((Stoppable) responseProcessor).stop();
    }
  }

  @Override
  public void dispose() {
    if (responseProcessor instanceof Disposable) {
      ((Disposable) responseProcessor).dispose();
    }
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    super.setMuleContext(muleContext);
    setMuleContextIfNeeded(responseProcessor, muleContext);
  }
}
