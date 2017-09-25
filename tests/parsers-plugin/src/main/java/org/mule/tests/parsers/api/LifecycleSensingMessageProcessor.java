/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.parsers.api;

import static org.mule.tests.parsers.api.LifecycleAction.DISPOSE;
import static org.mule.tests.parsers.api.LifecycleAction.INITIALISE;
import static org.mule.tests.parsers.api.LifecycleAction.START;
import static org.mule.tests.parsers.api.LifecycleAction.STOP;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.LinkedList;
import java.util.List;

public class LifecycleSensingMessageProcessor extends AbstractComponent implements Lifecycle, Processor {

  private List<LifecycleAction> lifecycleActions = new LinkedList<>();
  private LifecycleSensingObjectFactory objectFactory;

  public void setObjectFactory(LifecycleSensingObjectFactory objectFactory) {
    this.objectFactory = objectFactory;
  }

  @Override
  public void dispose() {
    lifecycleActions.add(DISPOSE);
  }

  @Override
  public void initialise() throws InitialisationException {
    lifecycleActions.add(INITIALISE);
  }

  @Override
  public void start() throws MuleException {
    lifecycleActions.add(START);
  }

  @Override
  public void stop() throws MuleException {
    lifecycleActions.add(STOP);
  }

  public List<LifecycleAction> getLifecycleActions() {
    return lifecycleActions;
  }

  public LifecycleSensingObjectFactory getObjectFactory() {
    return objectFactory;
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return event;
  }
}
