/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.deployment;

import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Prober;
import org.mule.tck.probe.file.FileExists;

import java.io.File;

public class DeploymentTestUtils {

  public static void assertFileExists(File pluginsDir, String filePath) {
    Prober prober = new PollingProber(5000, 100);
    File marker = new File(pluginsDir, filePath);
    prober.check(new FileExists(marker));
  }
}
