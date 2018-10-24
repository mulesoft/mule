/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import static java.lang.String.format;

import java.io.Serializable;

/**
 * Keeps context information about the processors that a flow executed.
 *
 * @since 3.8.0
 */
public final class FlowStackElement implements Serializable {

  private static final long serialVersionUID = 192333659386101806L;

  private String flowName;
  private String processorPath;
  private Long creationTime;

  public FlowStackElement(String flowName, String processorPath) {
    this.flowName = flowName;
    this.processorPath = processorPath;
    this.creationTime = System.currentTimeMillis();
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

  /**
   * @return the time when the flow stack was created.
   */
  public Long getCreationTime() {
    return creationTime;
  }

  /**
   * @return the milliseconds elapsed between its creation and now.
   */
  public Long getElapsedTime() {
    return System.currentTimeMillis() - creationTime;
  }

  @Override
  public String toString() {
    if (processorPath == null) {
      return format("%s %d ms", flowName, getElapsedTime());
    } else {
      return format("%s(%s) %d ms", flowName, processorPath, getElapsedTime());
    }
  }
}
