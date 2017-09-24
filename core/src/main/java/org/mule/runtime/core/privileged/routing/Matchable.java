/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.routing;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;

/**
 * Enables an artifact to be matched for routing before actually routing to it
 */
public interface Matchable {

  /**
   * Determines if the event should be processed
   *
   * @param event the current event to evaluate
   * @param builder an event builder in case the filter needs to make changes to the event.
   * @return true if the event should be processed by this router
   * @throws MuleException if the event cannot be evaluated
   */
  boolean isMatch(CoreEvent event, CoreEvent.Builder builder) throws MuleException;
}
