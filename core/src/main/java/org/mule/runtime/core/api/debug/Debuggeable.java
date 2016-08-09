/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.debug;

/**
 * Specifies that a message processor can debug its internal execution.
 *
 * @since 3.8.0
 */
public interface Debuggeable {

  /**
   * Enables the debug mode for this message processor and returns a new debugging session.
   *
   * @param callback The callback to be used to interact with the mule debugger. Non null
   * @return The debugging session
   */
  DebuggingSession enableDebug(DebuggerCallback callback);

  /**
   * If it is already enabled or not.
   *
   * @return True if it is enabled, false otherwise
   */
  boolean isDebugEnabled();

  /**
   * Disables the debugging mode. And finish the debugging session. It should resume any paused execution.
   */
  void disableDebug();

}
