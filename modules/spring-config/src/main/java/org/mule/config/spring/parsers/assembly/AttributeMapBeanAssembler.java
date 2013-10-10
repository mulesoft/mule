/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.assembly;

import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * This can be used inside a {@link org.mule.config.spring.parsers.generic.ChildDefinitionParser} - it
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
