/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
