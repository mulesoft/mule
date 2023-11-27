/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.environment.singleapp.impl;

import org.mule.runtime.environment.api.RuntimeEnvironment;
import org.mule.runtime.environment.singleapp.api.SingleAppStarter;

import javax.inject.Inject;

/**
 * A {@link RuntimeEnvironment} with only one application. When started, the app is started.
 *
 * @since 4.7.0
 */
public class SingleAppEnvironment implements RuntimeEnvironment {

  private SingleAppStarter singleAppStarter;

  @Override
  public String getName() {
    return "Single App Environment";
  }

  @Override
  public String getDescription() {
    return "An Environment which works with a single app";
  }

  @Override
  public void start() {
    singleAppStarter.startApp();
  }

  @Override
  public boolean isSingleApp() {
    return false;
  }

  @Inject
  public void setSingleAppStarter(SingleAppStarter singleAppStarter) {
    this.singleAppStarter = singleAppStarter;
  }
}
