/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice;

import org.mule.transformer.AbstractTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;

/**
 * TODO
 */
public class OrangetoAppleTransformer extends AbstractTransformer implements DiscoverableTransformer
{
    public OrangetoAppleTransformer()
    {
        setReturnClass(Apple.class);
        registerSourceType(Orange.class);
    }

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

    }
}
