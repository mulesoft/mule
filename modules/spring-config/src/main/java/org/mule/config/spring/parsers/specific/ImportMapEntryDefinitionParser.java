/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.assembly.BeanAssemblerFactory;
import org.mule.config.spring.parsers.assembly.DefaultBeanAssembler;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;

public class ImportMapEntryDefinitionParser extends ChildMapEntryDefinitionParser
{

    public ImportMapEntryDefinitionParser(String mapName)
    {
        super(mapName);
        addAlias("name", KEY);
        addAlias("class", VALUE);
        setBeanAssemblerFactory(new ImportMapEntryBeanAssemblerFactory());
    }

    private static class ImportMapEntryBeanAssemblerFactory implements BeanAssemblerFactory
    {
        @Override
        public BeanAssembler newBeanAssembler(PropertyConfiguration beanConfig,
                                              BeanDefinitionBuilder bean,
                                              PropertyConfiguration targetConfig,
                                              BeanDefinition target)
        {
            return new ImportMapEntryBeanAssembler(beanConfig, bean, targetConfig, target);
        }
    }

    private static class ImportMapEntryBeanAssembler extends DefaultBeanAssembler
    {
        public ImportMapEntryBeanAssembler(PropertyConfiguration beanConfig,
                                           BeanDefinitionBuilder bean,
                                           PropertyConfiguration targetConfig,
                                           BeanDefinition target)
        {
            super(beanConfig, bean, targetConfig, target);
        }

        @Override
        public void insertBeanInTarget(String oldName)
        {
            assertTargetPresent();
            PropertyValues sourceProperties = bean.getRawBeanDefinition().getPropertyValues();
            String newName = bestGuessName(targetConfig, oldName, target.getBeanClassName());
            MutablePropertyValues targetProperties = target.getPropertyValues();
            PropertyValue pv = targetProperties.getPropertyValue(newName);
            @SuppressWarnings("unchecked")
            ManagedMap<String, String> oldValue = (ManagedMap<String, String>) (null == pv
                                                                                          ? null
                                                                                          : pv.getValue());

            if (null == oldValue)
            {
                oldValue = new ManagedMap<String, String>();
                pv = new PropertyValue(newName, oldValue);
                targetProperties.addPropertyValue(pv);
            }

            String importName = null;
            String importClassName = (String) sourceProperties.getPropertyValue(
                ChildMapEntryDefinitionParser.VALUE).getValue();

            PropertyValue namePropertyValue = sourceProperties.getPropertyValue(ChildMapEntryDefinitionParser.KEY);
            if (namePropertyValue != null)
            {
                importName = (String) namePropertyValue.getValue();
            }
            else
            {
                importName = importClassName.substring(importClassName.lastIndexOf(".") + 1,
                    importClassName.length());
            }

            oldValue.put(importName, importClassName);
        }
    }

}
