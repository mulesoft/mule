/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.config;

import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.vegan.extension.HarvestPeachesSource;

import org.junit.Test;

public class SourceWithoutConnectedOperationTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "vegan-config.xml";
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    HarvestPeachesSource.isConnected = false;
    super.doSetUpBeforeMuleContextCreation();
  }

  @Test
  public void testSourceIsConnected() throws Exception {
    new PollingProber(1000, 1000).check(new JUnitLambdaProbe(() -> HarvestPeachesSource.isConnected));
  }

}
