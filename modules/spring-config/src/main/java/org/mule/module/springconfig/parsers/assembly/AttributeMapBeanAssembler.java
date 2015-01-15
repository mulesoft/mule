/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.assembly;

import org.mule.module.springconfig.parsers.assembly.configuration.PropertyConfiguration;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * This can be used inside a {@link org.mule.module.springconfig.parsers.generic.ChildDefinitionParser} - it
 * takes named properties and, instead of inserting them individually on the target, it packages them as
 * a Map and inserts that.
 */
public class AttributeMapBeanAssembler extends AbstractMapBeanAssembler
{

    public AttributeMapBeanAssembler(PropertyConfiguration beanConfig, BeanDefinitionBuilder bean,
                                     PropertyConfiguration targetConfig, BeanDefinition target)
    {
        super(beanConfig, bean, targetConfig, target);
    }

    /**
     * We override this method to insert a map instead of the definition.
     */
    public void insertBeanInTarget(String oldName)
    {
        assertTargetPresent();
        insertDefinitionAsMap(oldName);
    }

}
