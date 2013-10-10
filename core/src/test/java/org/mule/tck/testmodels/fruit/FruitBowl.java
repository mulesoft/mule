/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.fruit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FruitBowl
{
    /**
     * logger used by this class
     */
    private static final Log logger = LogFactory.getLog(FruitBowl.class);

    private final Map<Class<?>, Fruit> bowl = Collections.synchronizedMap(new HashMap<Class<?>, Fruit>());

    public FruitBowl()
    {
        super();
    }

    public FruitBowl(Fruit fruit[])
    {
        for (int i = 0; i < fruit.length; i++)
        {
            bowl.put(fruit[i].getClass(), fruit[i]);
        }
    }

    public FruitBowl(Apple apple, Banana banana)
    {
        bowl.put(Apple.class, apple);
        bowl.put(Banana.class, banana);
    }

    public boolean hasApple()
    {
        return bowl.get(Apple.class) != null;
    }

    public boolean hasBanana()
    {
        return bowl.get(Banana.class) != null;
    }

    public void addFruit(Fruit fruit)
    {
        bowl.put(fruit.getClass(), fruit);
    }

    public Fruit[] addAppleAndBanana(Apple apple, Banana banana)
    {
        bowl.put(Apple.class, apple);
        bowl.put(Banana.class, banana);
        return new Fruit[]{apple, banana};
    }

    public Fruit[] addBananaAndApple(Banana banana, Apple apple)
    {
        bowl.put(Apple.class, apple);
        bowl.put(Banana.class, banana);
        return new Fruit[]{banana, apple};

    }

    public List<Fruit> getFruit()
    {
        return new ArrayList<Fruit>(bowl.values());
    }

    public Object consumeFruit(FruitLover fruitlover)
    {
        logger.debug("Got a fruit lover who says: " + fruitlover.speak());
        for (Fruit fruit : bowl.values())
        {
            fruit.bite();
        }
        return fruitlover;
    }

    public void setFruit(Fruit[] fruit)
    {
        for (int i = 0; i < fruit.length; i++)
        {
            bowl.put(fruit[i].getClass(), fruit[i]);
        }
    }

    public void setFruit(List<Fruit> fruit)
    {
        this.setFruit(fruit.toArray(new Fruit[fruit.size()]));
    }

    public Apple getApple()
    {
        return (Apple) bowl.get(Apple.class);
    }

    public void setApple(Apple apple)
    {
        bowl.put(Apple.class, apple);
    }

    public Banana getBanana()
    {
        return (Banana) bowl.get(Banana.class);
    }

    public void setBanana(Banana banana)
    {
        bowl.put(Banana.class, banana);
    }

}
