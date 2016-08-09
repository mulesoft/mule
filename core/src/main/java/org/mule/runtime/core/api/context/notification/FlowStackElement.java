/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import java.io.Serializable;

/**
 * Keeps context information about the processors that a flow executed.
 * 
 * @since 3.8.0
 */
public class FlowStackElement implements Serializable {

  private static final long serialVersionUID = -851491195125245390L;

  private String flowName;
  private String processorPath;

  public FlowStackElement(String flowName, String processorPath) {
    this.flowName = flowName;
    this.processorPath = processorPath;
  }

  /**
   * @return the path of the currently executing processor in the flow represented by this element.
   */
  public String getProcessorPath() {
    return processorPath;
  }

  /**
   * @return the name of the flow which execution is represented by this element.
   */
  public String getFlowName() {
    return flowName;
  }

  @Override
  public String toString() {
    if (processorPath == null) {
      return String.format("%s", flowName);
    } else {
      return String.format("%s(%s)", flowName, processorPath);
    }
  }
}
