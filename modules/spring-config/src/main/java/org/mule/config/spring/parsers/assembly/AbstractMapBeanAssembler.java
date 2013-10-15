/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.assembly;

import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.util.MapCombiner;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;

public abstract class AbstractMapBeanAssembler extends DefaultBeanAssembler
{

    public AbstractMapBeanAssembler(PropertyConfiguration beanConfig, BeanDefinitionBuilder bean,
                                    PropertyConfiguration targetConfig, BeanDefinition target)
    {
        super(beanConfig, bean, targetConfig, target);
    }

    protected void insertDefinitionAsMap(String oldName)
    {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(MapCombiner.class);
        Map map = new ManagedMap();
        for (Iterator pvs = getBean().getBeanDefinition().getPropertyValues().getPropertyValueList().iterator();
             pvs.hasNext();)
        {
            PropertyValue pv = (PropertyValue) pvs.next();
            map.put(pv.getName(), pv.getValue());
        }
        List list = new ManagedList();
        list.add(map);
        builder.addPropertyValue(MapCombiner.LIST, list);
        setBean(builder);
        super.insertBeanInTarget(oldName);
    }

}
