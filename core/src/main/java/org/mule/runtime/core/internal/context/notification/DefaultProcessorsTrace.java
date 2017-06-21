/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.notification;

import org.mule.runtime.core.api.context.notification.ProcessorsTrace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keeps context information about the message processors that were executed as part of the processing of an event.
 */
public class DefaultProcessorsTrace implements ProcessorsTrace {

  private static final long serialVersionUID = 5327053121687733907L;

  private List<String> executedProcessors = Collections.synchronizedList(new ArrayList<String>());

  /**
   * Adds a message processor path to the list of processors that were executed as part of the processing of this event.
   * 
   * @param processorPath the path to mask as executed.
   */
  public void addExecutedProcessors(String processorPath) {
    executedProcessors.add(processorPath);
  }

  @Override
  public List<String> getExecutedProcessors() {
    return new ArrayList<>(executedProcessors);
  }

}
