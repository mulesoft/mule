/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.fruit;

import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

/**
 * Converts a FruitBowl to a FruitBasket (for testing obviously :)
 */
public class FruitBowlToFruitBasket extends AbstractTransformer implements DiscoverableTransformer
{
    private int weighting = 1;

    public FruitBowlToFruitBasket()
    {
        registerSourceType(DataTypeFactory.create(FruitBowl.class));
        setReturnDataType(DataTypeFactory.create(FruitBasket.class));
    }

    @Override
    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        FruitBowl bowl = (FruitBowl)src;
        FruitBasket basket = new FruitBasket();
        basket.setFruit(bowl.getFruit());
        return basket;
    }

    /**
     * If 2 or more discoverable transformers are equal, this value can be used to select the correct one
     *
     * @return the priority weighting for this transformer. This is a value between
     *         {@link #MIN_PRIORITY_WEIGHTING} and {@link #MAX_PRIORITY_WEIGHTING}.
     */
    public int getPriorityWeighting()
    {
        return weighting;
    }

    /**
     * If 2 or more discoverable transformers are equal, this value can be used to select the correct one
     *
     * @param weighting the priority weighting for this transformer. This is a value between
     *                  {@link #MIN_PRIORITY_WEIGHTING} and {@link #MAX_PRIORITY_WEIGHTING}.
     */
    public void setPriorityWeighting(int weighting)
    {
        this.weighting = weighting;
    }
}
