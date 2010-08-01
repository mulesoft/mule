/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.api.routing.filter.Filter;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.assembly.BeanAssemblerFactory;
import org.mule.config.spring.parsers.assembly.DefaultBeanAssembler;
import org.mule.config.spring.parsers.assembly.DefaultBeanAssemblerFactory;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.routing.MessageFilter;

import java.util.List;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.w3c.dom.Element;

/**
 * This allows a filter to be defined globally, or embedded within an endpoint. IF required the filter is
 * wrapped in MessageFilter instance before being injected into the parent.
 */
public class FilterDefinitionParser extends ParentContextDefinitionParser
{

    public static final String FILTER = "filter";
    public static final String ATTRIBUTE_NAME = AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME;

    public FilterDefinitionParser(Class filter)
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, new OrphanDefinitionParser(filter, false));
        otherwise(new FilterChildDefinitionParser("messageProcessor", filter, Filter.class, false));
        addIgnored(ATTRIBUTE_NAME);
    }

    public FilterDefinitionParser()
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, new OrphanDefinitionParser(false));
        otherwise(new FilterChildDefinitionParser("messageProcessor", null, Filter.class, true));
        addIgnored(ATTRIBUTE_NAME);
    }

    private static class FilterChildDefinitionParser extends ChildDefinitionParser
    {

        public FilterChildDefinitionParser(String setterMethod,
                                           Class clazz,
                                           Class constraint,
                                           boolean allowClassAttribute)
        {
            super(setterMethod, clazz, constraint, allowClassAttribute);
        }

        @Override
        public String getPropertyName(Element e)
        {
            if (!isWrapWithMessageFilter(e))
            {
                return FILTER;
            }
            else
            {
                return super.getPropertyName(e);
            }
        }

        @Override
        protected void preProcess(Element element)
        {
            super.preProcess(element);
            if (isWrapWithMessageFilter(element))
            {
                setBeanAssemblerFactory(new MessageProcessorWrappingBeanAssemblerFactory());
            }
            else
            {
                setBeanAssemblerFactory(new DefaultBeanAssemblerFactory());
            }
        }

        private boolean isWrapWithMessageFilter(Element e)
        {
            String parentName = e.getParentNode().getLocalName().toLowerCase();
            String grandParentName = e.getParentNode().getParentNode().getLocalName().toLowerCase();

            return !("message-filter".equals(parentName) || "and-filter".equals(parentName)
                     || "or-filter".equals(parentName) || "not-filter".equals(parentName)
                     || "outbound".equals(grandParentName) || "selective-consumer-router".equals(parentName) || "error-filter".equals(parentName));
        }

    }

    private static class MessageProcessorWrappingBeanAssemblerFactory implements BeanAssemblerFactory
    {

        public BeanAssembler newBeanAssembler(PropertyConfiguration beanConfig,
                                              BeanDefinitionBuilder bean,
                                              PropertyConfiguration targetConfig,
                                              BeanDefinition target)
        {
            return new MessageProcessorWrappingBeanAssembler(beanConfig, bean, targetConfig, target);
        }
    }

    private static class MessageProcessorWrappingBeanAssembler extends DefaultBeanAssembler
    {

        public MessageProcessorWrappingBeanAssembler(PropertyConfiguration beanConfig,
                                                     BeanDefinitionBuilder bean,
                                                     PropertyConfiguration targetConfig,
                                                     BeanDefinition target)
        {
            super(beanConfig, bean, targetConfig, target);
        }

        public void insertBeanInTarget(String oldName)
        {
            assertTargetPresent();
            String newName = bestGuessName(targetConfig, oldName, target.getBeanClassName());
            MutablePropertyValues targetProperties = target.getPropertyValues();
            PropertyValue pv = targetProperties.getPropertyValue(newName);
            Object oldValue = null == pv ? null : pv.getValue();

            BeanDefinitionBuilder messageFilter = BeanDefinitionBuilder.genericBeanDefinition(MessageFilter.class);
            messageFilter.addPropertyValue(FILTER, bean.getBeanDefinition());

            if (oldValue == null)
            {
                oldValue = new ManagedList();
                pv = new PropertyValue(newName, oldValue);
                targetProperties.addPropertyValue(pv);
            }
            if (targetConfig.isCollection(oldName))
            {
                List list = retrieveList(oldValue);
                list.add(messageFilter.getBeanDefinition());
            }
            else
            {
                targetProperties.addPropertyValue(newName, messageFilter.getBeanDefinition());
            }
        }

    }

}
