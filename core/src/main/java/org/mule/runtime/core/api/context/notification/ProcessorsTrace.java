/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.core.api.config.DefaultMuleConfiguration;

import java.io.Serializable;
import java.util.List;

/**
 * Keeps context information about the message processors that were executed as part of the processing of an event.
 * 
 * @since 3.8.0
 */
public interface ProcessorsTrace extends Serializable {

  /**
   * @return the paths of the processors that were executed as part of flows that have already been completed, ordered by time of
   *         execution, if {@link DefaultMuleConfiguration#isFlowTrace()} is {@code true}. Empty list otherwise.
   */
  List<String> getExecutedProcessors();

}
