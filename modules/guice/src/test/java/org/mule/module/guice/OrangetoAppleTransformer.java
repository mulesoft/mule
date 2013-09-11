/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
