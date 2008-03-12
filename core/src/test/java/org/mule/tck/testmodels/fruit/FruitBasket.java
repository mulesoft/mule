/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.fruit;

import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * TODO
 */
public class FruitBasket
{
    private final Map basket = Collections.synchronizedMap(new HashMap());

    public boolean hasApple()
    {
        return basket.get(Apple.class) != null;
    }

    public boolean hasBanana()
    {
        return basket.get(Banana.class) != null;
    }

    public void setFruit(Fruit[] fruit)
    {
        for (int i = 0; i < fruit.length; i++)
        {
            basket.put(fruit[i].getClass(), fruit[i]);
        }
    }

    public void setFruit(List fruit)
    {
        this.setFruit((Fruit[]) fruit.toArray(new Fruit[fruit.size()]));
    }

    public List getFruit()
    {
        return new ArrayList(basket.values());
    }

    public Apple getApple()
    {
        return (Apple) basket.get(Apple.class);
    }

    public Banana getBanana()
    {
        return (Banana) basket.get(Banana.class);
    }
}
