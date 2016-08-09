/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.debug;

/**
 * Manages the breakpoint in the debugging session.
 *
 * @since 3.8.0
 */
public interface BreakpointManager {

  /**
   * Adds a breakpoint to the current set of breakpoints.
   *
   * @param breakpoint The breakpoint to be added. Non null
   * @return True if the breakpoint was add successfully
   */
  boolean addBreakpoint(Breakpoint breakpoint);

  /**
   * Removes a breakpoint from the current set of breakpoints.
   *
   * @param breakpoint The breakpoint to be removed. Non null
   * @return True if the breakpoint was removed successfully
   */
  boolean removeBreakpoint(Breakpoint breakpoint);

}
