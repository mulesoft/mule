/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static org.mule.runtime.api.metadata.DataType.fromType;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.Map;

import javax.inject.Inject;

public class DumpInterceptedParametersProcessor extends AbstractComponent implements Processor {

  @Inject
  private ParameterInterceptorConfig config;

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return CoreEvent.builder(event)
        .message(Message.builder()
            .payload(new TypedValue<>(config.dump(), fromType(Map.class)))
            .build())
        .build();
  }
}
