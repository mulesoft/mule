/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.debug;

/**
 * Handles the execution of the underlying debugging engine.
 *
 * @since 3.8.0
 */
public interface ExecutionManager {

  /**
   * Resumes the execution and stop in the next instruction with same or lower frame level is reached.
   */
  void nextStep();

  /**
   * Resumes the execution and stop in the next instruction.
   */
  void stepIn();

  /**
   * Resumes the execution.
   */
  void resume();

  /**
   * Should resume the execution until the specified location is reached.
   *
   * @param location The location to stop
   */
  void runToLocation(String location);

}
