/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.scan;

import org.mule.tck.testmodels.fruit.Fruit;

/**
 * TODO
 */
public class Grape implements Fruit
{
    private boolean bitten;

    public void bite()
    {
        bitten = true;
    }

    public boolean isBitten()
    {
        return bitten;
    }

    public int getSeeds()
    {
        return 4;
    }
}
