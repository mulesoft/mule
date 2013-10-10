/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
