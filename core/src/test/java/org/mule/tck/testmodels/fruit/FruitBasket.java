/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
