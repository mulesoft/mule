/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.debug;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.core.util.Preconditions.checkArgument;

/**
 * Identifies an execution breakpoint.
 *
 * @since 3.8.0
 */
public class Breakpoint {

  private String location;
  private String condition;

  /**
   * It contains the location of where the execution needs to stop and the condition if any.
   *
   * @param location The location. Must not be empty {@link String}
   * @param condition The condition script
   */
  public Breakpoint(String location, String condition) {
    checkArgument(!isEmpty(location), "Location cannot be empty");
    this.location = location;
    this.condition = condition;
  }

  /**
   * Returns the location of this breakpoint
   *
   * @return The location string
   */
  public String getLocation() {
    return location;
  }

  /**
   * Return the condition expression for this breakpoint if any.
   *
   * @return Returns the script or null
   */
  public String getCondition() {
    return condition;
  }
}
