/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.destination;

import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.Topic;

/**
 * Type identifier for a {@link Destination}.
 *
 * @since 4.0
 */
public enum DestinationType {

  /**
   * {@link Destination} is a {@link Queue}
   */
  QUEUE(false),

  /**
   * {@link Destination} is a {@link Topic}
   */
  TOPIC(true);

  private final boolean isTopic;

  DestinationType(boolean isTopic) {
    this.isTopic = isTopic;
  }

  public boolean isTopic() {
    return isTopic;
  }
}
