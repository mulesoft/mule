/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
