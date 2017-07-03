/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.cluster;

import org.mule.runtime.api.cluster.ClusterService;

/**
 * Implementation of {@link ClusterService} to be used when the runtime is not in cluster mode.
 * 
 * @since 4.0
 */
public class DefaultClusterService implements ClusterService {

  @Override
  public boolean isPrimaryPollingInstance() {
    return true;
  }
}
