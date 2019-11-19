/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.api.annotation.NoImplement;

import java.io.Serializable;
import java.util.List;

/**
 * Keeps context information about the message processors that were executed as part of the processing of an event.
 *
 * @since 3.8.0
 * 
 * @deprecated Use the message history functionality form the agent instead.
 */
@NoImplement
@Deprecated
public interface ProcessorsTrace extends Serializable {

  /**
   * @return an empty list.
   */
  List<String> getExecutedProcessors();

}
