/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.Extensible;

import java.util.Objects;

@Extensible
public interface Weapon {

  class WeaponAttributes {

    public String brand;

    public String getBrand() {
      return brand;
    }

    public void setBrand(String brand) {
      this.brand = brand;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      WeaponAttributes that = (WeaponAttributes) o;
      return Objects.equals(brand, that.brand);
    }

    @Override
    public int hashCode() {
      return Objects.hash(brand);
    }
  }

  String kill();
}
