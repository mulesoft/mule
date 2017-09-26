/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.interception;

import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.MuleSession;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

/**
 * Adds support for compatibility attribues of the {@link CoreEvent} to {@link InterceptionEvent}.
 *
 * @since 4.0
 */
public interface InternalInterceptionEvent extends InterceptionEvent {

  /**
   * Returns the service session for this event
   * 
   * @return the service session for the current event
   */
  MuleSession getSession();

  /**
   * Set the {@link MuleSession} to construct the target Event with.
   *
   * @param session the session instance.
   * @return the builder instance
   */
  InternalInterceptionEvent session(MuleSession session);

  /**
   * Updates the state of this object if needed.
   *
   * @return {@link PrivilegedEvent} with the result.
   */
  PrivilegedEvent resolve();

}
