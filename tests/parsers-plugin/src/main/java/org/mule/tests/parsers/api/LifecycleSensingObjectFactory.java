/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.parsers.api;

import static org.mule.tests.parsers.api.LifecycleAction.GET_OBJECT;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

import java.util.LinkedList;
import java.util.List;

public class LifecycleSensingObjectFactory extends AbstractComponentFactory<Processor> {

  private List<LifecycleAction> lifecycleActions = new LinkedList<>();

  @Override
  public Processor doGetObject() throws Exception {
    lifecycleActions.add(GET_OBJECT);
    LifecycleSensingMessageProcessor lifecycleSensingMessageProcessor = new LifecycleSensingMessageProcessor();
    lifecycleSensingMessageProcessor.setObjectFactory(this);
    return lifecycleSensingMessageProcessor;
  }

  public List<LifecycleAction> getLifecycleActions() {
    return lifecycleActions;
  }
}
