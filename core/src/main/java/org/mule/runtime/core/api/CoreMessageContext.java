/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.core.management.stats.ProcessingTime;

/**
 * Extension of {@link MessageContext} with core specific methods.
 *
 * @since 4.0
 */
public interface CoreMessageContext extends MessageContext {

  /**
   * @returns information about the times spent processing the events for this context (so far).
   */
  ProcessingTime getProcessingTime();

}
