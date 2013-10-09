/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.annotations.param;

import org.mule.api.annotations.expressions.Lookup;
import org.mule.api.transformer.Transformer;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformer.simple.ObjectToByteArray;

import java.util.ArrayList;
import java.util.List;

/**
 * A component for testing invocations with more than one parameter
 */
public class LookupComponent
{
    @Lookup("transformer1")
    private Transformer transformer1;

    @Lookup(value = "transformer2", optional = true)
    private Transformer transformer2;

    @Lookup
    private ObjectToByteArray transformer3;

    public List<Fruit> listFruit(@Lookup Banana banana, 
                                           @Lookup("redApple") Apple apple,
                                           @Lookup(optional = true) Orange orange)
    {
        List<Fruit> f = new ArrayList<Fruit>();
        f.add(banana);
        f.add(apple);
        if(orange!=null)
        {
            f.add(orange);
        }
        return f;
    }

    public Transformer getTransformer1()
    {
        return transformer1;
    }

    public Transformer getTransformer2()
    {
        return transformer2;
    }

    public ObjectToByteArray getTransformer3()
    {
        return transformer3;
    }
}
