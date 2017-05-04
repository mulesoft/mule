/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.destination;

import javax.jms.Topic;

/**
 * Implementations of this interface provide a way to configure custom properties for a consumer
 * based on the destination kind from which consumption will occurr
 *
 * @since 4.0
 */
public interface ConsumerType {

  /**
   * @return {@code true} if this a consumer for {@link Topic}s
   */
  boolean topic();

}
