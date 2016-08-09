/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process;

import org.mule.tck.probe.Probe;

/**
 * Checks if a Mule application is successfully deployed.
 *
 * @deprecated use {@link org.mule.test.infrastructure.process.rules.MuleDeployment#checkAppIsDeployed(String)} instead}
 */
@Deprecated
public class AppDeploymentProbe implements Probe {

  private boolean check;
  private MuleProcessController mule;
  private String appName;

  public static AppDeploymentProbe isDeployed(MuleProcessController mule, String appName) {
    return new AppDeploymentProbe(mule, appName, true);
  }

  public static AppDeploymentProbe notDeployed(MuleProcessController mule, String appName) {
    return new AppDeploymentProbe(mule, appName, false);
  }

  protected AppDeploymentProbe(MuleProcessController mule, String appName, Boolean check) {
    this.mule = mule;
    this.appName = appName;
    this.check = check;
  }

  public boolean isSatisfied() {
    return check == mule.isDeployed(appName);
  }

  public String describeFailure() {
    return "Application [" + appName + "] is " + (check ? "not" : "") + " deployed.";
  }
}
