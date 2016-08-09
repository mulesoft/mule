/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.vegan.extension.HarvestPeachesSource;
import org.mule.test.vegan.extension.VeganExtension;

import org.junit.Test;

public class SourceWithoutConnectedOperationTestCase extends ExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "vegan-config.xml";
  }

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {VeganExtension.class};
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
