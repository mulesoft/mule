/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import static java.lang.System.currentTimeMillis;

import java.io.Serializable;

/**
 * Keeps context information about the processors that a flow executed.
 *
 * @since 3.8.0
 */
public final class FlowStackElement implements Serializable {

  private static final long serialVersionUID = -2372094725681872367L;

  private final String flowName;
  private final String processorPath;
  private final long creationTime;

  public FlowStackElement(String flowName, String processorPath) {
    this.flowName = flowName;
    this.processorPath = processorPath;
    this.creationTime = currentTimeMillis();
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
  public long getCreationTimeLong() {
    return creationTime;
  }

  /**
   * @return the milliseconds elapsed between its creation and now.
   */
  public long getElapsedTimeLong() {
    return currentTimeMillis() - creationTime;
  }

  /**
   * @return the time when the flow stack was created.
   *
   * @deprecated Use {@link #getCreationTimeLong()} instead.
   */
  @Deprecated
  public Long getCreationTime() {
    return creationTime;
  }

  /**
   * @return the milliseconds elapsed between its creation and now.
   *
   * @deprecated Use {@link #getElapsedTimeLong()} instead.
   */
  @Deprecated
  public Long getElapsedTime() {
    return currentTimeMillis() - creationTime;
  }

  @Override
  public String toString() {
    if (processorPath == null) {
      return flowName;
    } else {
      return flowName.concat("(").concat(processorPath).concat(")");
    }
  }

  public String toStringWithElapsedTime() {
    return toString().concat(" ").concat(Long.toString(getElapsedTimeLong())).concat(" ms");
  }
}
