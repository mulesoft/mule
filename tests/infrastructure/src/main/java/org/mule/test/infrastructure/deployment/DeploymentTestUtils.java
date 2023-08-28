/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
