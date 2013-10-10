/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
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
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.create(Map.class));
    }

    @Override
    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        Map result = BeanUtils.describeBean(src);
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
