/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.source;

import org.mule.runtime.api.lifecycle.Lifecycle;

/**
 * Defines a message source that runs in only one node of a cluster.
 */
public interface ClusterizableMessageSource extends MessageSource, Lifecycle {

  /**
   * @return true is the source is started in the current cluster node, false otherwise.
   */
  boolean isStarted();
}
