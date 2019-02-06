/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
