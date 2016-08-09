/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.NestedProcessor;
import org.mule.runtime.core.api.processor.MessageProcessor;

/**
 * An {@link AbstractNestedProcessorValueResolver} which wraps the given {@link MuleEvent} in a {@link NestedProcessor}. This
 * resolver returns new instances per every invocation
 *
 * @since 3.7.0
 */
public final class NestedProcessorValueResolver extends AbstractNestedProcessorValueResolver<NestedProcessor> {

  private final MessageProcessor messageProcessor;

  public NestedProcessorValueResolver(MessageProcessor messageProcessor) {
    this.messageProcessor = messageProcessor;
  }

  /**
   * Returns a {@link NestedProcessor} that wraps the {@code event}
   *
   * @param event a {@link MuleEvent}
   * @return a {@link NestedProcessor}
   * @throws MuleException
   */
  @Override
  public NestedProcessor resolve(MuleEvent event) throws MuleException {
    return toNestedProcessor(messageProcessor, event);
  }
}
