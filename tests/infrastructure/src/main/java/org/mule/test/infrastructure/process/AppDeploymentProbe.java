/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
