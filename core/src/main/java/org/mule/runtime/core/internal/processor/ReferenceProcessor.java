/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used as wrapper for {@link Processor}s referenced using the <processor> element so it can hold the metadata added
 * through the {@link Component} interface since the referenced object may be a singleton an the metadata is related to the
 * <processor> element it self and not to the referenced object.
 */
public class ReferenceProcessor extends AbstractProcessor
    implements InterceptingMessageProcessor, MuleContextAware, Lifecycle {

  private static final Logger logger = LoggerFactory.getLogger(ReferenceProcessor.class);
  private final Processor referencedProcessor;
  private MuleContext muleContext;

  public ReferenceProcessor(Processor processor) {
    this.referencedProcessor = processor;
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return referencedProcessor.process(event);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return referencedProcessor.apply(publisher);
  }

  public Processor getReferencedProcessor() {
    return referencedProcessor;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(referencedProcessor);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(referencedProcessor, logger);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(referencedProcessor);
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(referencedProcessor, muleContext);
  }

  @Override
  public void setListener(Processor listener) {
    if (referencedProcessor instanceof InterceptingMessageProcessor) {
      ((InterceptingMessageProcessor) referencedProcessor).setListener(listener);
    } else {
      throw new IllegalStateException("setListener call not expected since the referenced message processor is not intercepting");
    }
  }
}
