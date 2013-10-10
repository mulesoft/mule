/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
