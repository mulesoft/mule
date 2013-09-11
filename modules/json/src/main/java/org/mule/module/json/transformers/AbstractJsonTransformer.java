/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.transformer.AbstractMessageTransformer;

import org.codehaus.jackson.map.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO
 */
public abstract class AbstractJsonTransformer extends AbstractMessageTransformer implements DiscoverableTransformer
{
    protected int weighting = DiscoverableTransformer.MAX_PRIORITY_WEIGHTING;

    private ObjectMapper mapper;

    private Map<Class<?>, Class<?>> sharedMixins = new HashMap<Class<?>, Class<?>>();

    @Override
    public void initialise() throws InitialisationException
    {
        if (mapper == null)
        {
            mapper = new ObjectMapper();
        }
    }

    public ObjectMapper getMapper()
    {
        return mapper;
    }

    public void setMapper(ObjectMapper mapper)
    {
        this.mapper = mapper;
    }

    public int getPriorityWeighting()
    {
        return weighting;
    }

    public void setPriorityWeighting(int weighting)
    {
        this.weighting = weighting;
    }

    public Map<Class<?>, Class<?>> getMixins()
    {
        return sharedMixins;
    }

    public void setMixins(Map<Class<?>, Class<?>> mixins)
    {
        this.sharedMixins = mixins;
    }
}
