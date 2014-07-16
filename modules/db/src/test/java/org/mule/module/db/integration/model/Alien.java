/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.model;

public class Alien
{

    public static final Alien ET = new Alien("Mars", "ET", "Male", true);
    public static final Alien MONGUITO = new Alien("Saturn", "Monguito", "Male", true);

    private final String planet;
    private final String name;
    private final String gender;
    private final boolean friendly;

    public Alien(String planet, String name, String gender, boolean friendly)
    {
        this.planet = planet;
        this.name = name;
        this.gender = gender;
        this.friendly = friendly;
    }

    public String getName()
    {
        return name;
    }

    public String getGender()
    {
        return gender;
    }

    public boolean isFriendly()
    {
        return friendly;
    }

    public String getPlanet()
    {
        return planet;
    }

    public String getXml()
    {
        return "<Alien>" +
               "<Planet>" + getPlanet() + "</Planet>"
               +"<Gender>" + getGender() + "</Gender>" +
               "<Friendly>" + isFriendly() + "</Friendly>" +
               "</Alien>";
    }
}
