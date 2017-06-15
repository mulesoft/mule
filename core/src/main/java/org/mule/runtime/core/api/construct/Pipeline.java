/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.construct;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.processor.ProcessingDescriptor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.source.MessageSource;

import java.util.List;
import java.util.Map;

/**
 * A pipeline has an ordered list of {@link Processor}'s that are invoked in order to processor new messages received from it's
 * {@link MessageSource}
 */
public interface Pipeline extends FlowConstruct, ProcessingDescriptor {

  MessageSource getMessageSource();

  List<Processor> getMessageProcessors();

  /**
   * @return the {@link ProcessingStrategy} used on the pipeline.
   */
  ProcessingStrategy getProcessingStrategy();

  /**
   * Map of current {@link EventContext} instances for {@link Event}'s that have been serialized. Entries will be removed on
   * deserialization or in the last resort purged through garbage collection when there are no longer any hard references to the
   * {@link EventContext} left. {@link EventContext}'s for {@link Event}'s that are not serialized will never be added to this
   * cache.
   *
   * @return map of event context keyed by their id as obtained from {@link EventContext#getId()}
   */
  Map<String, EventContext> getSerializationEventContextCache();
}
