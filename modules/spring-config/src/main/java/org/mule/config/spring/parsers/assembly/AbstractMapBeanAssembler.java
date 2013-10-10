/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
