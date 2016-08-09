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
import org.mule.runtime.core.util.collection.ImmutableListCollector;

import java.util.List;

/**
 * An {@link AbstractNestedProcessorValueResolver} which wraps the given {@link MuleEvent} in a {@link List} of
 * {@link NestedProcessor}. This resolver returns new instances per every invocation
 *
 * @since 3.7.0
 */
public class NestedProcessorListValueResolver extends AbstractNestedProcessorValueResolver<List<NestedProcessor>> {

  private List<MessageProcessor> messageProcessors;

  public NestedProcessorListValueResolver(List<MessageProcessor> messageProcessors) {
    this.messageProcessors = messageProcessors;
  }

  @Override
  public List<NestedProcessor> resolve(MuleEvent event) throws MuleException {
    return messageProcessors.stream().map(mp -> toNestedProcessor(mp, event)).collect(new ImmutableListCollector<>());
  }

  @Override
  public boolean isDynamic() {
    return false;
  }
}
