/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.context;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleManifest;

/**
 * Expose information about the Mule instance:
 * 
 * <li><b>clusterid</b> <i>Cluster ID</i>
 * <li><b>home</b> <i>Home directory</i>
 * <li><b>nodeid</b> <i>Cluster Node ID</i>
 * <li><b>version</b> <i>Mule Version</i>
 */
public class MuleInstanceContext {

  private MuleContext muleContext;

  public MuleInstanceContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  public String getVersion() {
    return MuleManifest.getProductVersion();
  }

  public String getClusterId() {
    return muleContext.getClusterId();
  }

  public int getNodeId() {
    return muleContext.getClusterNodeId();
  }

  public String getHome() {
    return muleContext.getConfiguration().getMuleHomeDirectory();
  }

}
