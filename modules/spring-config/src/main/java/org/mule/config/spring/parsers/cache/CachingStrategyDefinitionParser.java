/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.cache;

import org.mule.cache.ObjectStoreCachingStrategy;
import org.mule.keygenerator.ExpressionMuleEventKeyGenerator;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.assembly.BeanAssemblerFactory;
import org.mule.config.spring.parsers.assembly.DefaultBeanAssembler;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.parsers.assembly.configuration.SingleProperty;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

public class CachingStrategyDefinitionParser extends OrphanDefinitionParser
{

    public CachingStrategyDefinitionParser(Class<ObjectStoreCachingStrategy> objectStoreCachingStrategyClass, boolean singleton)
    {
        super(objectStoreCachingStrategyClass, singleton);
        setBeanAssemblerFactory(new LocalBeanAssemblerFactory());
        registerPreProcessor(new CheckExclusiveAttributes(new String[][] {
                new String[] {"keyGenerationExpression"}, new String[] {"keyGeneration-ref"}}));
    }

    private class LocalBeanAssembler extends DefaultBeanAssembler
    {

        public LocalBeanAssembler(PropertyConfiguration beanConfig, BeanDefinitionBuilder bean,
                                  PropertyConfiguration targetConfig, BeanDefinition target)
        {
            super(beanConfig, bean, targetConfig, target);
        }

        protected void addPropertyWithReference(MutablePropertyValues properties, SingleProperty config, String name, Object value)
        {
            if ("keyGenerationExpression".equals(name))
            {
                BeanDefinitionBuilder wrapper = BeanDefinitionBuilder.genericBeanDefinition(ExpressionMuleEventKeyGenerator.class);
                wrapper.addPropertyValue("expression", value);

                super.addPropertyWithReference(properties, config, "keyGenerator", wrapper.getBeanDefinition());
            }
            else
            {
                super.addPropertyWithReference(properties, config, name, value);
            }
        }
    }

    private class LocalBeanAssemblerFactory implements BeanAssemblerFactory
    {

        public BeanAssembler newBeanAssembler(PropertyConfiguration beanConfig, BeanDefinitionBuilder bean,
                                              PropertyConfiguration targetConfig, BeanDefinition target)
        {
            return new LocalBeanAssembler(beanConfig, bean, targetConfig, target);
        }

    }
}
