/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.fruit;

/**
 * <code>InvalidSatsuma</code> has no discoverable methods
 */
public class InvalidSatsuma implements Fruit
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6328691504772842584L;

    private boolean bitten = false;

    public void bite()
    {
        bitten = true;

    }

    public boolean isBitten()
    {
        return bitten;
    }
}
