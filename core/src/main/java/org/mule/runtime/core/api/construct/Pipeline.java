/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.construct;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.InternalEventContext;
import org.mule.runtime.core.api.processor.ProcessingDescriptor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;

import java.util.List;
import java.util.Map;

/**
 * A pipeline has an ordered list of {@link Processor}'s that are invoked in order to processor new messages received from it's
 * {@link MessageSource}
 */
public interface Pipeline extends FlowConstruct, ProcessingDescriptor {

  /**
   * @return source of messages to use.
   */
  MessageSource getSource();

  /**
   * @return processors to execute on a {@link Message}.
   */
  List<Processor> getProcessors();

  /**
   * @return the maximum concurrency to be used by the {@link Pipeline}.
   */
  int getMaxConcurrency();

  /**
   * Map of current {@link InternalEventContext} instances for {@link InternalEvent}'s that have been serialized. Entries will be removed on
   * deserialization or in the last resort purged through garbage collection when there are no longer any hard references to the
   * {@link InternalEventContext} left. {@link InternalEventContext}'s for {@link InternalEvent}'s that are not serialized will never be added to this
   * cache.
   *
   * @return map of event context keyed by their id as obtained from {@link InternalEventContext#getId()}
   */
  Map<String, InternalEventContext> getSerializationEventContextCache();

}
