/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.marvel.model;

import java.util.Objects;

public class Villain {

  public static final String KABOOM = "KABOOM!";

  private boolean alive = true;

  public String takeHit(Missile missile) {
    alive = false;
    return KABOOM;
  }

  public boolean isAlive() {
    return alive;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Villain villain = (Villain) o;
    return alive == villain.alive;
  }

  @Override
  public int hashCode() {
    return Objects.hash(alive);
  }
}
