/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.Processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a simple route with the {@link Processor} it leads to.
 *
 * @Since 4.3.0
 */
public class ProcessorRoute extends AbstractComponent implements MuleContextAware, Lifecycle {

  private final static Logger LOGGER = LoggerFactory.getLogger(ProcessorRoute.class);

  private final Processor processor;
  private MuleContext muleContext;

  public ProcessorRoute(Processor processor) {
    requireNonNull(processor, "processor can't be null");
    this.processor = processor;
  }

  public Processor getProcessor() {
    return processor;
  }

  @Override
  public String toString() {
    return reflectionToString(this, SHORT_PREFIX_STYLE);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
    if (processor instanceof MuleContextAware) {
      ((MuleContextAware) processor).setMuleContext(context);
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(processor, muleContext);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(processor);
  }

  /**
   * @return an {@link ExecutableRoute} for this one.
   */
  ExecutableRoute toExecutableRoute() {
    return new ExecutableRoute(this);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(processor);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(processor, LOGGER);
  }
}
