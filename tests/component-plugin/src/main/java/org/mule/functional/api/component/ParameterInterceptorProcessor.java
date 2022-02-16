/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import javax.inject.Inject;

public class ParameterInterceptorProcessor extends AbstractComponent implements Processor {

  @Inject
  private ParameterInterceptorConfig config;

  private String name;

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    config.intercept(name, event.getParameters());
    return event;
  }

  public void setName(String name) {
    this.name = name;
  }
}
