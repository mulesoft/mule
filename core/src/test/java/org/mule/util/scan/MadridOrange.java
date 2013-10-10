/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.scan;

import org.mule.tck.testmodels.fruit.Orange;

/**
 * Test class used for classpath searching
 */
public class MadridOrange extends Orange
{
    public MadridOrange()
    {
        super(new Integer(12), new Double(4.3), "Madrid");
    }
}
