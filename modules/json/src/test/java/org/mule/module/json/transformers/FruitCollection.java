/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Orange;

/**
 * TODO
 */
public class FruitCollection
{
    private Apple apple;
    private Banana banana;
    private Orange orange;

    public FruitCollection()
    {
        super();
    }

    public FruitCollection(Apple apple, Banana banana, Orange orange)
    {
        this.apple = apple;
        this.banana = banana;
        this.orange = orange;
    }

    public Apple getApple()
    {
        return apple;
    }

    public Banana getBanana()
    {
        return banana;
    }

    public Orange getOrange()
    {
        return orange;
    }

    public void setApple(Apple apple)
    {
        this.apple = apple;
    }

    public void setBanana(Banana banana)
    {
        this.banana = banana;
    }

    public void setOrange(Orange orange)
    {
        this.orange = orange;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        FruitCollection that = (FruitCollection) o;

        if (apple != null ? !apple.equals(that.apple) : that.apple != null)
        {
            return false;
        }
        if (banana != null ? !banana.equals(that.banana) : that.banana != null)
        {
            return false;
        }
        if (orange != null ? !orange.equals(that.orange) : that.orange != null)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = apple != null ? apple.hashCode() : 0;
        result = 31 * result + (banana != null ? banana.hashCode() : 0);
        result = 31 * result + (orange != null ? orange.hashCode() : 0);
        return result;
    }
}
