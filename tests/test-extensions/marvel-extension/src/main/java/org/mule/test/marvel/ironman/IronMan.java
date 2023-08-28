/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.marvel.ironman;

import static org.mule.test.marvel.ironman.IronMan.CONFIG_NAME;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.ClassValue;
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

  @ClassValue(extendsOrImplements = "com.starkindustries.AIEngine")
  @Parameter
  @Optional(defaultValue = "com.starkindustries.Jarvis")
  private String aiType;

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
