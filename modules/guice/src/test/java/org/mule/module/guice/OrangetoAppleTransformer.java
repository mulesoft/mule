/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.guice;

import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

/**
 * TODO
 */
public class OrangetoAppleTransformer extends AbstractTransformer implements DiscoverableTransformer
{
    public OrangetoAppleTransformer()
    {
        setReturnDataType(DataTypeFactory.create(Apple.class));
        registerSourceType(DataTypeFactory.create(Orange.class));
    }

    @Override
    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        return new Apple();
    }

    public int getPriorityWeighting()
    {
        return 0;
    }

    public void setPriorityWeighting(int weighting)
    {
        // ignore
    }
}
