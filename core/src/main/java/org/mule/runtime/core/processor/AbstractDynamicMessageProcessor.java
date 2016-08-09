/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.util.ObjectUtils;

/**
 * Implementation of {@link MessageProcessor} that dynamically chooses and uses another {@link MessageProcessor}
 */
public abstract class AbstractDynamicMessageProcessor implements MessageProcessor {

  public MuleEvent process(MuleEvent event) throws MuleException {
    return resolveMessageProcessor(event).process(event);
  }

  /**
   * Determines which MessageProcessor should be used. Implementations may choose to use a message property, configure this
   */
  protected abstract MessageProcessor resolveMessageProcessor(MuleEvent event) throws MuleException;

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }
}
