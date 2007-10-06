/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.assembly;

import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;

import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;

public class AbstractMapBeanAssembler extends DefaultBeanAssembler
{

    public AbstractMapBeanAssembler(PropertyConfiguration beanConfig, BeanDefinitionBuilder bean,
                                    PropertyConfiguration targetConfig, BeanDefinition target)
    {
        super(beanConfig, bean, targetConfig, target);
    }

    protected void insertDefinitionAsMap(String oldName)
    {
        // being called for the second time, so translate definition to map and insert
        String newName = bestGuessName(getTargetConfig(), oldName, getTarget().getBeanClassName());
        if (! getTargetConfig().isIgnored(oldName))
        {
            Map map;
            if (null != getTarget().getPropertyValues().getPropertyValue(newName))
            {
                map = (Map) getTarget().getPropertyValues().getPropertyValue(newName).getValue();
            }
            else
            {
                map = new ManagedMap();
            }
            Iterator pvs = getBean().getBeanDefinition().getPropertyValues().getPropertyValueList().iterator();
            while (pvs.hasNext())
            {
                PropertyValue pv = (PropertyValue) pvs.next();
                // TODO - this needs to be more intelligent, extending lists etc
                map.put(pv.getName(), pv.getValue());
            }
            getTarget().getPropertyValues().addPropertyValue(newName, map);
        }
    }

}