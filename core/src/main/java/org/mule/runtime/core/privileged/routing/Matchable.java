/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
   * @param event   the current event to evaluate
   * @param builder an event builder in case the filter needs to make changes to the event.
   * @return true if the event should be processed by this router
   * @throws MuleException if the event cannot be evaluated
   */
  boolean isMatch(CoreEvent event, CoreEvent.Builder builder) throws MuleException;
}
