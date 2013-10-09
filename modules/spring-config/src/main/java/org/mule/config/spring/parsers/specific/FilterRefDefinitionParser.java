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
import org.mule.config.spring.parsers.assembly.DefaultBeanAssemblerFactory;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.routing.MessageFilter;

import java.util.List;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.w3c.dom.Element;

/**
 * This allows a filter to be defined globally, or embedded within an endpoint. IF required the filter is
 * wrapped in MessageFilter instance before being injected into the parent.
 */
public class FilterRefDefinitionParser extends ParentDefinitionParser
{
    public static final String FILTER = "filter";

    @Override
    protected void preProcess(Element element)
    {
        super.preProcess(element);
        if (isWrapWithMessageFilter(element))
        {
            setBeanAssemblerFactory(new MessageProcessorWrappingBeanAssemblerFactory());
            addAlias(ATTRIBUTE_REF, "messageProcessor");
        }
        else
        {
            setBeanAssemblerFactory(new DefaultBeanAssemblerFactory());
            addAlias(ATTRIBUTE_REF, FILTER);
        }
    }

    private boolean isWrapWithMessageFilter(Element e)
    {
        String parentName = e.getParentNode().getLocalName().toLowerCase();
        String grandParentName = e.getParentNode().getParentNode().getLocalName().toLowerCase();

        return !("message-filter".equals(parentName) || "and-filter".equals(parentName)
                 || "or-filter".equals(parentName) || "not-filter".equals(parentName)
                 || "outbound".equals(grandParentName) || "selective-consumer-router".equals(parentName)
                 || "error-filter".equals(parentName) || "when".equals(parentName));
    }

    private static class MessageProcessorWrappingBeanAssemblerFactory implements BeanAssemblerFactory
    {
        @Override
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

        @Override
        public void copyBeanToTarget()
        {
            String oldName = "messageProcessor";
            assertTargetPresent();
            String newName = bestGuessName(targetConfig, oldName, target.getBeanClassName());
            MutablePropertyValues targetProperties = target.getPropertyValues();
            MutablePropertyValues beanProperties = bean.getBeanDefinition().getPropertyValues();
            Object value = beanProperties.getPropertyValue(newName).getValue();
            RuntimeBeanReference ref = (RuntimeBeanReference) ((ManagedList<?>) value).get(0);

            BeanDefinitionBuilder messageFilter = BeanDefinitionBuilder.genericBeanDefinition(MessageFilter.class);
            messageFilter.addPropertyValue(FILTER, ref);

            PropertyValue pv = targetProperties.getPropertyValue(newName);
            Object oldValue = null == pv ? null : pv.getValue();

            if (oldValue == null)
            {
                oldValue = new ManagedList<Object>();
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
