/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.Router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.reactivestreams.Publisher;

/**
 * FirstSuccessful routes an event to the first target route that can accept it without throwing or returning an exception. If no
 * such route can be found, an exception is thrown. Note that this works more reliable with synchronous targets, but no such
 * restriction is imposed.
 */
public class FirstSuccessful extends AbstractComponent implements Router, Lifecycle, MuleContextAware {

  private final List<ProcessorRoute> routes = new ArrayList<>();
  private MuleContext muleContext;


  @Override
  public void initialise() throws InitialisationException {
    for (ProcessorRoute route : routes) {
      route.setMessagingExceptionHandler(null);
      initialiseIfNeeded(route, muleContext);
    }
  }

  @Override
  public void start() throws MuleException {
    for (ProcessorRoute route : routes) {
      route.start();
    }
  }

  @Override
  public void stop() throws MuleException {
    for (ProcessorRoute route : routes) {
      route.stop();
    }
  }

  @Override
  public void dispose() {
    for (ProcessorRoute route : routes) {
      route.dispose();
    }
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  public void addRoute(final Processor processor) {
    routes.add(new ProcessorRoute(processor));
  }

  public void setRoutes(Collection<Processor> routes) {
    routes.forEach(this::addRoute);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return new FirstSuccessfulRouter(this, publisher, routes).getDownstreamPublisher();
  }


}
