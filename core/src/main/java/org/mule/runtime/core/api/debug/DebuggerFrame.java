/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.debug;

import java.util.List;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.core.util.Preconditions.checkArgument;

/**
 * Represents an execution frame with the current debugging state.
 *
 * @since 3.8.0
 */
public class DebuggerFrame {

  private List<FieldDebugInfo<?>> variables;
  private String name;

  /**
   * A new debugger frame with the list of variables and a given name for this frame.
   *
   * @param variables The list of visible variables in this frame. Non null
   * @param name A name of the frame. Non empty
   */
  public DebuggerFrame(List<FieldDebugInfo<?>> variables, String name) {
    checkArgument(variables != null, "Variables cannot be null.");
    checkArgument(!isEmpty(name), "Name cannot be empty.");
    this.variables = variables;
    this.name = name;
  }

  /**
   * Returns the list of variables active in this frame
   *
   * @return The list of variables
   */
  public List<FieldDebugInfo<?>> getVariables() {
    return variables;
  }

  /**
   * The name of the frame
   *
   * @return The name
   */
  public String getName() {
    return name;
  }
}
