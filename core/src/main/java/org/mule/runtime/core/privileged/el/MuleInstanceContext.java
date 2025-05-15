/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.el;

import static org.mule.runtime.manifest.api.MuleManifest.getMuleManifest;

import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;

/**
 * Expose information about the Mule instance:
 *
 * <li><b>clusterid</b> <i>Cluster ID</i>
 * <li><b>home</b> <i>Home directory</i>
 * <li><b>nodeid</b> <i>Cluster Node ID</i>
 * <li><b>version</b> <i>Mule Version</i>
 */
@NoInstantiate
public class MuleInstanceContext {

  private MuleContext muleContext;
  private MuleConfiguration configuration;

  public MuleInstanceContext(MuleContext muleContext, MuleConfiguration configuration) {
    this.muleContext = muleContext;
    this.configuration = configuration;
  }

  public String getVersion() {
    return getMuleManifest().getProductVersion();
  }

  public String getClusterId() {
    return muleContext.getClusterId();
  }

  public int getNodeId() {
    return muleContext.getClusterNodeId();
  }

  public String getHome() {
    return configuration.getMuleHomeDirectory();
  }

}
