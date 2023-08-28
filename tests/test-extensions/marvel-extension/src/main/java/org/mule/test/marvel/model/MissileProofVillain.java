/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.marvel.model;

public class MissileProofVillain extends Villain {

  public static final String MISSILE_PROOF = "Missile proof";

  @Override
  public String takeHit(Missile missile) {
    throw new UnsupportedOperationException(MISSILE_PROOF);
  }
}
