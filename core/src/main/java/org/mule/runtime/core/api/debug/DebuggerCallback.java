/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.debug;

import org.mule.runtime.core.api.MuleEvent;

import java.util.List;

/**
 * Callback to the mule debugger framework.
 *
 * @since 3.8.0
 */
public interface DebuggerCallback {

  /**
   * Notifies when the execution of the event has begun.
   *
   * @param event The event that is being debugged. Non null
   */
  void onExecutionBegun(MuleEvent event);

  /**
   * Notifies when the execution is paused for debugging the current execution state.
   *
   * @param event The event that is being debugged. Non null
   * @param frameStack The frame stack. Non null
   * @param location The location where it was stopped. Non null
   */
  void onExecutionPaused(MuleEvent event, List<DebuggerFrame> frameStack, String location);

  /**
   * Notifies when the execution of the event has ended.
   *
   * @param event The event being debugged. Non null
   */
  void onExecutionEnded(MuleEvent event);
}
