/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.model;

public class MissileProofVillain extends Villain {

  public static final String MISSILE_PROOF = "Missile proof";

  @Override
  public String takeHit(Missile missile) {
    throw new UnsupportedOperationException(MISSILE_PROOF);
  }
}
