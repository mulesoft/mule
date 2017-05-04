/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.destination;

import org.mule.runtime.extension.api.annotation.Alias;

import javax.jms.Destination;
import javax.jms.Queue;

/**
 * Implementation of {@link ConsumerType} that marks the consumed {@link Destination}
 * as a {@link Queue}.
 *
 * @since 4.0
 */
@Alias("queue-consumer")
public final class QueueConsumer implements ConsumerType {

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean topic() {
    return false;
  }
}
