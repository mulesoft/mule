/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.core.api.util.collection.Collectors.toImmutableList;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.NestedProcessor;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;

import javax.inject.Inject;

/**
 * An {@link AbstractNestedProcessorValueResolver} which wraps the given {@link InternalEvent} in a {@link List} of
 * {@link NestedProcessor}. This resolver returns new instances per every invocation
 *
 * @since 3.7.0
 */
public class NestedProcessorListValueResolver extends AbstractNestedProcessorValueResolver<List<NestedProcessor>> {

  private List<Processor> messageProcessors;

  // TODO MULE-10332: Review MuleContextAware vs @Inject usage
  @Inject
  private MuleContext muleContext;

  public NestedProcessorListValueResolver(List<Processor> messageProcessors) {
    this.messageProcessors = messageProcessors;
  }

  @Override
  public List<NestedProcessor> resolve(ValueResolvingContext context) throws MuleException {
    return messageProcessors.stream().map(mp -> toNestedProcessor(mp, context.getEvent(), muleContext))
        .collect(toImmutableList());
  }

  @Override
  public boolean isDynamic() {
    return false;
  }
}
