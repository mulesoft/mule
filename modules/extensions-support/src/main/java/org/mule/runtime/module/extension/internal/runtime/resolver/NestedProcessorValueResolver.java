/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.NestedProcessor;
import org.mule.runtime.core.api.processor.Processor;

import javax.inject.Inject;

/**
 * An {@link AbstractNestedProcessorValueResolver} which wraps the given {@link InternalEvent} in a {@link NestedProcessor}. This resolver
 * returns new instances per every invocation
 *
 * @since 3.7.0
 */
public final class NestedProcessorValueResolver extends AbstractNestedProcessorValueResolver<NestedProcessor> {

  private final Processor messageProcessor;

  // TODO MULE-10332: Review MuleContextAware vs @Inject usage
  @Inject
  private MuleContext muleContext;

  public NestedProcessorValueResolver(Processor messageProcessor) {
    this.messageProcessor = messageProcessor;
  }

  /**
   * Returns a {@link NestedProcessor} that wraps the {@code event}
   *
   * @param context a {@link ValueResolvingContext}
   * @return a {@link NestedProcessor}
   * @throws MuleException
   */
  @Override
  public NestedProcessor resolve(ValueResolvingContext context) throws MuleException {
    return toNestedProcessor(messageProcessor, context.getEvent(), muleContext);
  }

  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }
}
