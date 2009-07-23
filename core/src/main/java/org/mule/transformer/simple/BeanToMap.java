/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.util.BeanUtils;

import java.util.Map;

/**
 * Conversts a simple bean object to a Map. every property on the bean will become an entry in the
 * result {@link java.util.Map}. Note that only exposed bean properties with getter and setter methods will be
 * added to the map.
 */
public class BeanToMap extends AbstractTransformer implements DiscoverableTransformer
{

    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING;

    public BeanToMap()
    {
        registerSourceType(Object.class);
        setReturnClass(Map.class);
    }

    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        Map result = BeanUtils.describe(src);
        return result;
    }

    public int getPriorityWeighting()
    {
        return priorityWeighting;
    }

    public void setPriorityWeighting(int weighting)
    {
        priorityWeighting = weighting;
    }

    
}
