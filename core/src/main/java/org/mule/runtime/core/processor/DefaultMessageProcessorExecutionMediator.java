/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.processor;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;

import org.reactivestreams.Publisher;

/**
 * Default execution mediator for {@link Processor} that delegates the transformation to the {@link Processor}.
 *
 * @since 4.0
 */
public class DefaultMessageProcessorExecutionMediator implements MessageProcessorExecutionMediator {

  /**
   * {@inheritDoc}
   */
  @Override
  public Publisher<Event> apply(Publisher<Event> publisher, Processor processor) {
    return processor.apply(publisher);
  }

}
