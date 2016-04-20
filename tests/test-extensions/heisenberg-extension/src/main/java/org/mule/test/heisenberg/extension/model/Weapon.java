/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.model;

public interface Weapon
{

    public enum WeaponType
    {
        FIRE_WEAPON, MELEE_WEAPON
    }

    public class WeaponAttributes
    {

        public String getBrand()
        {
            return brand;
        }

        public void setBrand(String brand)
        {
            this.brand = brand;
        }

        public String brand;
    }

    String kill();
}
