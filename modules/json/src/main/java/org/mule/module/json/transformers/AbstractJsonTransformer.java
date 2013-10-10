/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
