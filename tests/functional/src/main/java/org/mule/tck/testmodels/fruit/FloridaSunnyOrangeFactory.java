/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.fruit;


/**
 * A simple object factory for unit testing.
 */
public class FloridaSunnyOrangeFactory 
{
    public static Orange giveMeAnOrange() 
    {
        Orange o = new Orange();
        o.setBrand("Florida Sunny");
        return o;
    }
}
