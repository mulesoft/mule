/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.generic;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.assembly.BeanAssemblerFactory;
import org.mule.config.spring.parsers.assembly.DefaultBeanAssembler;
import org.mule.config.spring.parsers.assembly.DefaultBeanAssemblerFactory;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.w3c.dom.Element;

import java.util.List;

/**
 * A child definition parser that wraps the child object
 */
public class WrappingChildDefinitionParser extends ChildDefinitionParser
{
    private final Class wrapperClass;
    private final String propertyNameInWrapper;
    private final String unwrappedPropertyName;
    private final WrappingController wrappingController;

    public WrappingChildDefinitionParser(String setterMethod,
                                         Class clazz,
                                         Class constraint,
                                         boolean allowClassAttribute,
                                         Class wrapperClass,
                                         String propertyNameInWrapper,
                                         String unwrappedPropertyName,
                                         WrappingController wrappingController)
    {
        super(setterMethod, clazz, constraint, allowClassAttribute);
        this.wrapperClass = wrapperClass;
        this.propertyNameInWrapper = propertyNameInWrapper;
        this.unwrappedPropertyName = unwrappedPropertyName;
        this.wrappingController = wrappingController;
    }

    @Override
    public String getPropertyName(Element
        e)
    {
        if (!wrappingController.shouldWrap(e))
        {
            return unwrappedPropertyName;
        }
        else
        {
            return super.getPropertyName(e);
        }
    }

    @Override
    protected void preProcess(Element
        element)
    {
        super.preProcess(element);
        if (wrappingController.shouldWrap(element))
        {
            setBeanAssemblerFactory(new MessageProcessorWrappingBeanAssemblerFactory(wrapperClass, propertyNameInWrapper));
        }
        else
        {
            setBeanAssemblerFactory(new DefaultBeanAssemblerFactory());
        }
    }

    /**
     * Determines whether to wrap the child based on the where it appears in the DOM.
     */
    public interface WrappingController
    {
        boolean shouldWrap(Element elm);
    }

    private static class MessageProcessorWrappingBeanAssemblerFactory implements BeanAssemblerFactory
    {
        private final Class wrapperClass;
        private final String propertyNameInWrapper;

        public MessageProcessorWrappingBeanAssemblerFactory(Class wrapperClass, String propertyNameInWrapper)
        {
            this.wrapperClass = wrapperClass;
            this.propertyNameInWrapper = propertyNameInWrapper;
        }

        public BeanAssembler newBeanAssembler(PropertyConfiguration beanConfig,
                                              BeanDefinitionBuilder bean,
                                              PropertyConfiguration targetConfig,
                                              BeanDefinition target)
        {
            return new MessageProcessorWrappingBeanAssembler(beanConfig, bean, targetConfig, target, wrapperClass, propertyNameInWrapper);
        }
    }

    private static class MessageProcessorWrappingBeanAssembler extends DefaultBeanAssembler
    {
        private final Class wrapperClass;
        private final String propertyNameInWrapper;

        public MessageProcessorWrappingBeanAssembler(PropertyConfiguration beanConfig,
                                                     BeanDefinitionBuilder bean,
                                                     PropertyConfiguration targetConfig,
                                                     BeanDefinition target,
                                                     Class wrapperClass,
                                                     String propertyNameInWrapper)
        {
            super(beanConfig, bean, targetConfig, target);
            this.wrapperClass = wrapperClass;
            this.propertyNameInWrapper = propertyNameInWrapper;
        }

        public void insertBeanInTarget(String oldName)
        {
            assertTargetPresent();
            String newName = bestGuessName(targetConfig, oldName, target.getBeanClassName());
            MutablePropertyValues targetProperties = target.getPropertyValues();
            PropertyValue pv = targetProperties.getPropertyValue(newName);
            Object oldValue = null == pv ? null : pv.getValue();

            BeanDefinitionBuilder wrapper = BeanDefinitionBuilder.genericBeanDefinition(wrapperClass);
            wrapper.addPropertyValue(propertyNameInWrapper, bean.getBeanDefinition());

            if (oldValue == null)
            {
                oldValue = new ManagedList();
                pv = new PropertyValue(newName, oldValue);
                targetProperties.addPropertyValue(pv);
            }
            if (targetConfig.isCollection(oldName))
            {
                List list = retrieveList(oldValue);
                list.add(wrapper.getBeanDefinition());
            }
            else
            {
                targetProperties.addPropertyValue(newName, wrapper.getBeanDefinition());
            }
        }
    }
}
