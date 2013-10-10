/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.fruit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FruitLover
{
    private final List eatList = Collections.synchronizedList(new ArrayList());
    private final String catchphrase;

    public FruitLover(String catchphrase)
    {
        this.catchphrase = catchphrase;
    }

    public void eatFruit(Fruit fruit)
    {
        fruit.bite();
        eatList.add(fruit.getClass());
    }

    public List getEatList()
    {
        return eatList;
    }

    public String speak()
    {
        return catchphrase;
    }
}
