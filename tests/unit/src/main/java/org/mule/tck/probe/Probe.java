/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.probe;

/**
 * A probe indicates whether the state of the system satisfies a given criteria
 */
public interface Probe {

  /**
   * Indicates whether or not the specified criteria was met or not.
   *
   * @return true if the criteria is satisfied.
   */
  boolean isSatisfied();

  /**
   * Describes the cause of the criteria failure for further analysis.
   *
   * @return the error message.
   */
  String describeFailure();
}
