/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
