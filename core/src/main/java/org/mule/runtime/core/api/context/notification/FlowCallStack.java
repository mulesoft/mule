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
 * Keeps context information about the executing flows and its callers in order to provide augmented troubleshooting information
 * for an application developer.
 *
 * @since 3.8.0
 */
@NoImplement
public interface FlowCallStack extends Serializable, Cloneable {

  /**
   * @return the current elements of this stack as a list, ordered from top to bottom.
   */
  List<FlowStackElement> getElements();

  /**
   * @return a deep copy of this object.
   */
  FlowCallStack clone();
}
