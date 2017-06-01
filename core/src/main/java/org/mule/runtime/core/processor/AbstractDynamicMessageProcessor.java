/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.ObjectUtils;

/**
 * Implementation of {@link Processor} that dynamically chooses and uses another {@link Processor}
 */
public abstract class AbstractDynamicMessageProcessor extends AbstractAnnotatedObject implements Processor {

  public Event process(Event event) throws MuleException {
    return resolveMessageProcessor(event).process(event);
  }

  /**
   * Determines which MessageProcessor should be used. Implementations may choose to use a message property, configure this
   */
  protected abstract Processor resolveMessageProcessor(Event event) throws MuleException;

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }
}
