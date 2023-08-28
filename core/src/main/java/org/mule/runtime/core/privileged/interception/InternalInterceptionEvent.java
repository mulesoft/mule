/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.interception;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.MuleSession;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

/**
 * Adds support for compatibility attribues of the {@link CoreEvent} to {@link InterceptionEvent}.
 *
 * @since 4.0
 */
@NoImplement
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
