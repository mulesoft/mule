/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;

import javax.inject.Inject;

/**
 * Returns and clears all the parameters captured in the {@link ParameterInterceptorConfig}
 *
 * @since 4.5.0
 */
public class DumpInterceptedParametersProcessor extends AbstractComponent implements Processor {

  @Inject
  private ParameterInterceptorConfig config;

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return CoreEvent.builder(event)
        .message(Message.builder()
            .mapValue(config.dump(), String.class, List.class)
            .build())
        .build();
  }
}
