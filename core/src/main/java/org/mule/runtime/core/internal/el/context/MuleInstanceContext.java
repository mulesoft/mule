/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
