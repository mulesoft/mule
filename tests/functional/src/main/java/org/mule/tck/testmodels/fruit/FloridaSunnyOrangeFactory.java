/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
