/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.debug;

/**
 * Represents a debugging session.
 *
 * @see Debuggeable#enableDebug(DebuggerCallback)
 * @since 3.8.0
 */
public interface DebuggingSession {

  /**
   * Returns the breakpoint manager for this session
   *
   * @return The breakpoint manager
   */
  BreakpointManager getBreakpointManager();

  /**
   * Returns the execution handler
   *
   * @return The Execution Manager
   */
  ExecutionManager getExecutionManager();

}
