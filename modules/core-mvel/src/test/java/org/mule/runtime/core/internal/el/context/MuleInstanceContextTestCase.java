/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.context;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;

import org.mule.runtime.core.api.config.MuleManifest;

import java.net.UnknownHostException;

import org.junit.Test;

import junit.framework.Assert;

public class MuleInstanceContextTestCase extends AbstractELTestCase {

  public MuleInstanceContextTestCase(String mvelOptimizer) {
    super(mvelOptimizer);
  }

  @Test
  public void version() throws UnknownHostException {
    Assert.assertEquals(MuleManifest.getProductVersion(), evaluate("mule.version"));
  }

  public void assignValueToMuleVersion() {
    assertFinalProperty("mule.version='1'");
  }

  @Test
  public void home() throws UnknownHostException {
    Assert.assertEquals(muleContext.getConfiguration().getMuleHomeDirectory(), evaluate(MULE_HOME_DIRECTORY_PROPERTY));
  }

  public void assignValueToHomeDir() {
    assertFinalProperty(MULE_HOME_DIRECTORY_PROPERTY + "='1'");
  }

  @Test
  public void clusterId() throws UnknownHostException {
    Assert.assertEquals(muleContext.getClusterId(), evaluate("mule.clusterId"));
  }

  public void assignValueToClusterId() {
    assertFinalProperty("mule.clusterId='1'");
  }

  @Test
  public void nodeId() throws UnknownHostException {
    Assert.assertEquals(muleContext.getClusterNodeId(), evaluate("mule.nodeId"));
  }

  public void assignValueToNodeId() {
    assertFinalProperty("mule.nodeId='1'");
  }

}
