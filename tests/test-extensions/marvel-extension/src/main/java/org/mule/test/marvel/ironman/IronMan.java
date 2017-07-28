/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.ironman;

import static org.mule.test.marvel.ironman.IronMan.CONFIG_NAME;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Path;
import org.mule.test.marvel.MissileProvider;
import org.mule.test.marvel.OddMissileProvider;
import org.mule.test.marvel.model.Missile;

@Configuration(name = CONFIG_NAME)
@Operations(IronManOperations.class)
@ConnectionProviders({MissileProvider.class, OddMissileProvider.class})
public class IronMan {

  public static final String CONFIG_NAME = "ironMan";

  @Path(acceptsUrls = true)
  @Parameter
  @Optional
  private String ironManConfigFile;

  private String flightPlan = null;
  private int missilesFired = 0;

  public void track(Missile missile) {
    missilesFired++;
  }

  public int getMissilesFired() {
    return missilesFired;
  }

  public String getFlightPlan() {
    return flightPlan;
  }

  public void setFlightPlan(String flightPlan) {
    this.flightPlan = flightPlan;
  }
}
