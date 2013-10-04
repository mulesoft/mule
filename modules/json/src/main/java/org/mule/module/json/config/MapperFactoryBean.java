/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.config;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import java.util.Map;

/**
 * TODO
 */
public class MapperFactoryBean extends AbstractFactoryBean<ObjectMapper>
{
    private String name;
    private Map<Class<?>, Class<?>> mixins;

    @Override
    public Class<?> getObjectType()
    {
        return ObjectMapper.class;
    }

    @Override
    protected ObjectMapper createInstance() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        if(mixins!=null)
        {
            for (Map.Entry<Class<?>, Class<?>> entry : mixins.entrySet())
            {
                mapper.getSerializationConfig().addMixInAnnotations(entry.getKey(), entry.getValue());
                mapper.getDeserializationConfig().addMixInAnnotations(entry.getKey(), entry.getValue());
            }
        }
        return mapper;
    }

    public Map<Class<?>, Class<?>> getMixins()
    {
        return mixins;
    }

    public void setMixins(Map<Class<?>, Class<?>> mixins)
    {
        this.mixins = mixins;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
