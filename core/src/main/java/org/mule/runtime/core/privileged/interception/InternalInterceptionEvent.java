/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.interception;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

/**
 * Adds support for compatibility attribues of the {@link CoreEvent} to {@link InterceptionEvent}.
 *
 * @since 4.0
 */
@NoImplement
public interface InternalInterceptionEvent extends InterceptionEvent {

  /**
   * Updates the state of this object if needed.
   *
   * @return {@link PrivilegedEvent} with the result.
   */
  PrivilegedEvent resolve();

}
