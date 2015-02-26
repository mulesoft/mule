/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.config;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.config.spring.parsers.generic.AutoIdUtils;
import org.mule.config.spring.util.SpringXMLUtils;
import org.mule.util.TemplateParser;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

public abstract class AbstractDevkitBasedDefinitionParser implements BeanDefinitionParser
{

    /**
     * Mule Pattern Info
     */
    private TemplateParser.PatternInfo patternInfo;

    public AbstractDevkitBasedDefinitionParser()
    {
        patternInfo = TemplateParser.createMuleStyleParser().getStyle();
    }

    protected boolean hasAttribute(Element element, String attributeName)
    {
        String value = element.getAttribute(attributeName);
        if ((value != null) && (!StringUtils.isBlank(value)))
        {
            return true;
        }
        return false;
    }

    protected void setRef(BeanDefinitionBuilder builder, String propertyName, String ref)
    {
        if (!isMuleExpression(ref))
        {
            builder.addPropertyValue(propertyName, new RuntimeBeanReference(ref));
        }
        else
        {
            builder.addPropertyValue(propertyName, ref);
        }
    }

    protected boolean isMuleExpression(String value)
    {
        if ((!value.startsWith(patternInfo.getPrefix())) && (!value.endsWith(patternInfo.getSuffix())))
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    protected ManagedList parseList(Element element,
                                    String childElementName,
                                    AbstractDevkitBasedDefinitionParser.ParseDelegate parserDelegate)
    {
        ManagedList managedList = new ManagedList();
        List<Element> childDomElements = DomUtils.getChildElementsByTagName(element, childElementName);
        for (Element childDomElement : childDomElements)
        {
            if (hasAttribute(childDomElement, "value-ref"))
            {
                if (!isMuleExpression(childDomElement.getAttribute("value-ref")))
                {
                    managedList.add(new RuntimeBeanReference(childDomElement.getAttribute("value-ref")));
                }
                else
                {
                    managedList.add(childDomElement.getAttribute("value-ref"));
                }
            }
            else
            {
                managedList.add(parserDelegate.parse(childDomElement));
            }
        }
        return managedList;
    }

    protected void parseListAndSetProperty(Element element,
                                           BeanDefinitionBuilder builder,
                                           String fieldName,
                                           String parentElementName,
                                           String childElementName,
                                           AbstractDevkitBasedDefinitionParser.ParseDelegate parserDelegate)
    {
        Element domElement = DomUtils.getChildElementByTagName(element, parentElementName);
        if (domElement != null)
        {
            if (hasAttribute(domElement, "ref"))
            {
                setRef(builder, fieldName, domElement.getAttribute("ref"));
            }
            else
            {
                ManagedList managedList = parseList(domElement, childElementName, parserDelegate);
                builder.addPropertyValue(fieldName, managedList);
            }
        }
    }

    protected ManagedSet parseSet(Element element,
                                  String childElementName,
                                  AbstractDevkitBasedDefinitionParser.ParseDelegate parserDelegate)
    {
        ManagedSet managedSet = new ManagedSet();
        List<Element> childDomElements = DomUtils.getChildElementsByTagName(element, childElementName);
        for (Element childDomElement : childDomElements)
        {
            if (hasAttribute(childDomElement, "value-ref"))
            {
                if (!isMuleExpression(childDomElement.getAttribute("value-ref")))
                {
                    managedSet.add(new RuntimeBeanReference(childDomElement.getAttribute("value-ref")));
                }
                else
                {
                    managedSet.add(childDomElement.getAttribute("value-ref"));
                }
            }
            else
            {
                managedSet.add(parserDelegate.parse(childDomElement));
            }
        }
        return managedSet;
    }

    protected void parseSetAndSetProperty(Element element,
                                          BeanDefinitionBuilder builder,
                                          String fieldName,
                                          String parentElementName,
                                          String childElementName,
                                          AbstractDevkitBasedDefinitionParser.ParseDelegate parserDelegate)
    {
        Element domElement = DomUtils.getChildElementByTagName(element, parentElementName);
        if (domElement != null)
        {
            if (hasAttribute(domElement, "ref"))
            {
                setRef(builder, fieldName, domElement.getAttribute("ref"));
            }
            else
            {
                ManagedSet managedSet = parseSet(domElement, childElementName, parserDelegate);
                builder.addPropertyValue(fieldName, managedSet);
            }
        }
    }

    protected void parseSetWithDefaultAndSetProperty(Element element,
                                                     BeanDefinitionBuilder builder,
                                                     String fieldName,
                                                     String parentElementName,
                                                     String childElementName,
                                                     String defaultValue,
                                                     AbstractDevkitBasedDefinitionParser.ParseDelegate parserDelegate)
    {
        Element domElement = DomUtils.getChildElementByTagName(element, parentElementName);
        if (domElement != null)
        {
            if (hasAttribute(domElement, "ref"))
            {
                setRef(builder, fieldName, domElement.getAttribute("ref"));
            }
            else
            {
                ManagedSet managedSet = parseSet(domElement, childElementName, parserDelegate);
                builder.addPropertyValue(fieldName, managedSet);
            }
        }
        else
        {
            builder.addPropertyValue(fieldName, defaultValue);
        }
    }

    protected ManagedMap parseMap(Element element,
                                  String childElementName,
                                  AbstractDevkitBasedDefinitionParser.ParseDelegate parserDelegate)
    {
        ManagedMap managedMap = new ManagedMap();
        List<Element> childDomElements = DomUtils.getChildElementsByTagName(element, childElementName);
        if (childDomElements.size() == 0)
        {
            childDomElements = DomUtils.getChildElements(element);
        }
        for (Element childDomElement : childDomElements)
        {
            Object key = null;
            if (hasAttribute(childDomElement, "key-ref"))
            {
                key = new RuntimeBeanReference(childDomElement.getAttribute("key-ref"));
            }
            else
            {
                if (hasAttribute(childDomElement, "key"))
                {
                    key = childDomElement.getAttribute("key");
                }
                else
                {
                    key = childDomElement.getTagName();
                }
            }
            if (hasAttribute(childDomElement, "value-ref"))
            {
                if (!isMuleExpression(childDomElement.getAttribute("value-ref")))
                {
                    managedMap.put(key, new RuntimeBeanReference(childDomElement.getAttribute("value-ref")));
                }
                else
                {
                    managedMap.put(key, childDomElement.getAttribute("value-ref"));
                }
            }
            else
            {
                managedMap.put(key, parserDelegate.parse(childDomElement));
            }
        }
        return managedMap;
    }

    protected void parseMapAndSetProperty(Element element,
                                          BeanDefinitionBuilder builder,
                                          String fieldName,
                                          String parentElementName,
                                          String childElementName,
                                          AbstractDevkitBasedDefinitionParser.ParseDelegate parserDelegate)
    {
        Element domElement = DomUtils.getChildElementByTagName(element, parentElementName);
        if (domElement != null)
        {
            if (hasAttribute(domElement, "ref"))
            {
                setRef(builder, fieldName, domElement.getAttribute("ref"));
            }
            else
            {
                ManagedMap managedMap = parseMap(domElement, childElementName, parserDelegate);
                builder.addPropertyValue(fieldName, managedMap);
            }
        }
    }

    protected void parseMapWithDefaultAndSetProperty(Element element,
                                                     BeanDefinitionBuilder builder,
                                                     String fieldName,
                                                     String parentElementName,
                                                     String childElementName,
                                                     String defaultValue,
                                                     AbstractDevkitBasedDefinitionParser.ParseDelegate parserDelegate)
    {
        Element domElement = DomUtils.getChildElementByTagName(element, parentElementName);
        if (domElement != null)
        {
            if (hasAttribute(domElement, "ref"))
            {
                setRef(builder, fieldName, domElement.getAttribute("ref"));
            }
            else
            {
                ManagedMap managedMap = parseMap(domElement, childElementName, parserDelegate);
                builder.addPropertyValue(fieldName, managedMap);
            }
        }
        else
        {
            builder.addPropertyValue(fieldName, defaultValue);
        }
    }

    protected void parseListWithDefaultAndSetProperty(Element element,
                                                      BeanDefinitionBuilder builder,
                                                      String fieldName,
                                                      String parentElementName,
                                                      String childElementName,
                                                      String defaultValue,
                                                      AbstractDevkitBasedDefinitionParser.ParseDelegate parserDelegate)
    {
        Element domElement = DomUtils.getChildElementByTagName(element, parentElementName);
        if (domElement != null)
        {
            if (hasAttribute(domElement, "ref"))
            {
                setRef(builder, fieldName, domElement.getAttribute("ref"));
            }
            else
            {
                ManagedList managedList = parseList(domElement, childElementName, parserDelegate);
                builder.addPropertyValue(fieldName, managedList);
            }
        }
        else
        {
            builder.addPropertyValue(fieldName, defaultValue);
        }
    }

    protected void parseConfigRef(Element element, BeanDefinitionBuilder builder)
    {
        if (hasAttribute(element, "config-ref"))
        {
            String configRef = element.getAttribute("config-ref");
            if (configRef.startsWith("#["))
            {
                builder.addPropertyValue("moduleObject", configRef);
            }
            else
            {
                builder.addPropertyValue("moduleObject", new RuntimeBeanReference(configRef));
            }
        }
    }

    protected void attachProcessorDefinition(ParserContext parserContext, BeanDefinition definition)
    {
        MutablePropertyValues propertyValues = parserContext.getContainingBeanDefinition()
            .getPropertyValues();
        if (parserContext.getContainingBeanDefinition()
            .getBeanClassName()
            .equals("org.mule.config.spring.factories.PollingMessageSourceFactoryBean"))
        {
            propertyValues.addPropertyValue("messageProcessor", definition);
        }
        else
        {
            if (parserContext.getContainingBeanDefinition()
                .getBeanClassName()
                .equals("org.mule.enricher.MessageEnricher"))
            {
                propertyValues.addPropertyValue("enrichmentMessageProcessor", definition);
            }
            else
            {
                PropertyValue messageProcessors = propertyValues.getPropertyValue("messageProcessors");
                if ((messageProcessors == null) || (messageProcessors.getValue() == null))
                {
                    propertyValues.addPropertyValue("messageProcessors", new ManagedList());
                }
                List listMessageProcessors = ((List) propertyValues.getPropertyValue("messageProcessors")
                    .getValue());
                listMessageProcessors.add(definition);
            }
        }
    }

    protected void attachSourceDefinition(ParserContext parserContext, BeanDefinition definition)
    {
        MutablePropertyValues propertyValues = parserContext.getContainingBeanDefinition()
            .getPropertyValues();
        propertyValues.addPropertyValue("messageSource", definition);
    }

    protected String getAttributeValue(Element element, String attributeName)
    {
        if (!StringUtils.isEmpty(element.getAttribute(attributeName)))
        {
            return element.getAttribute(attributeName);
        }
        return null;
    }

    protected void parseConfigName(Element element)
    {
        if (hasAttribute(element, "name"))
        {
            element.setAttribute("name", AutoIdUtils.getUniqueName(element, "mule-bean"));
        }
    }

    protected void setInitMethodIfNeeded(BeanDefinitionBuilder builder, Class clazz)
    {
        if (Initialisable.class.isAssignableFrom(clazz))
        {
            builder.setInitMethodName(Initialisable.PHASE_NAME);
        }
    }

    protected void setDestroyMethodIfNeeded(BeanDefinitionBuilder builder, Class clazz)
    {
        if (Disposable.class.isAssignableFrom(clazz))
        {
            builder.setDestroyMethodName(Disposable.PHASE_NAME);
        }
    }

    protected void parseProperty(BeanDefinitionBuilder builder, Element element, String propertyName)
    {
        parseProperty(builder, element, propertyName, propertyName);
    }

    protected void parseProperty(BeanDefinitionBuilder builder,
                                 Element element,
                                 String attributeName,
                                 String propertyName)
    {
        if (hasAttribute(element, attributeName))
        {
            builder.addPropertyValue(propertyName, element.getAttribute(attributeName));
        }
    }

    protected void setNoRecurseOnDefinition(BeanDefinition definition)
    {
        definition.setAttribute(MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_RECURSE, Boolean.TRUE);
    }

    protected String generateChildBeanName(Element element)
    {
        String id = SpringXMLUtils.getNameOrId(element);
        if (StringUtils.isBlank(id))
        {
            String parentId = SpringXMLUtils.getNameOrId(((Element) element.getParentNode()));
            return ((("." + parentId) + ":") + element.getLocalName());
        }
        else
        {
            return id;
        }
    }

    protected BeanDefinition parseNestedProcessor(Element element, ParserContext parserContext, Class factory)
    {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(factory);
        BeanDefinition beanDefinition = builder.getBeanDefinition();
        parserContext.getRegistry().registerBeanDefinition(generateChildBeanName(element), beanDefinition);
        element.setAttribute("name", generateChildBeanName(element));
        builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
        builder.setScope(BeanDefinition.SCOPE_SINGLETON);
        List list = parserContext.getDelegate().parseListElement(element, builder.getBeanDefinition());
        parserContext.getRegistry().removeBeanDefinition(generateChildBeanName(element));
        return beanDefinition;
    }

    protected List parseNestedProcessorAsList(Element element, ParserContext parserContext, Class factory)
    {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(factory);
        BeanDefinition beanDefinition = builder.getBeanDefinition();
        parserContext.getRegistry().registerBeanDefinition(generateChildBeanName(element), beanDefinition);
        element.setAttribute("name", generateChildBeanName(element));
        builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
        builder.setScope(BeanDefinition.SCOPE_SINGLETON);
        List list = parserContext.getDelegate().parseListElement(element, builder.getBeanDefinition());
        parserContext.getRegistry().removeBeanDefinition(generateChildBeanName(element));
        return list;
    }

    protected void parseNestedProcessorAsListAndSetProperty(Element element,
                                                            ParserContext parserContext,
                                                            Class factory,
                                                            BeanDefinitionBuilder builder,
                                                            String propertyName)
    {
        builder.addPropertyValue(propertyName, parseNestedProcessorAsList(element, parserContext, factory));
    }

    protected void parseNestedProcessorAndSetProperty(Element element,
                                                      ParserContext parserContext,
                                                      Class factory,
                                                      BeanDefinitionBuilder builder,
                                                      String propertyName)
    {
        builder.addPropertyValue(propertyName, parseNestedProcessor(element, parserContext, factory));
    }

    protected void parseNestedProcessorAsListAndSetProperty(Element element,
                                                            String childElementName,
                                                            ParserContext parserContext,
                                                            Class factory,
                                                            BeanDefinitionBuilder builder,
                                                            String propertyName)
    {
        Element childDomElement = DomUtils.getChildElementByTagName(element, childElementName);
        if (childDomElement != null)
        {
            builder.addPropertyValue(propertyName,
                parseNestedProcessorAsList(childDomElement, parserContext, factory));
        }
    }

    protected void parseNestedProcessorAndSetProperty(Element element,
                                                      String childElementName,
                                                      ParserContext parserContext,
                                                      Class factory,
                                                      BeanDefinitionBuilder builder,
                                                      String propertyName)
    {
        Element childDomElement = DomUtils.getChildElementByTagName(element, childElementName);
        if (childDomElement != null)
        {
            builder.addPropertyValue(propertyName,
                parseNestedProcessor(childDomElement, parserContext, factory));
        }
    }

    protected void parseRetryPolicyTemplate(String elementName,
                                            Element element,
                                            ParserContext parserContext,
                                            BeanDefinitionBuilder builder,
                                            BeanDefinition definition)
    {
        Element reconnectElement = DomUtils.getChildElementByTagName(element, elementName);
        if (reconnectElement != null)
        {
            parserContext.getRegistry()
                .registerBeanDefinition(getAttributeValue(element, "name"), definition);
            BeanDefinition retryPolicyBeanDefinition = parserContext.getDelegate().parseCustomElement(
                reconnectElement);
            parserContext.getRegistry().removeBeanDefinition(getAttributeValue(element, "name"));
            builder.addPropertyValue("retryPolicyTemplate", retryPolicyBeanDefinition);
        }
    }

    protected boolean parseObjectRef(Element element,
                                     BeanDefinitionBuilder builder,
                                     String elementName,
                                     String propertyName)
    {
        Element childElement = DomUtils.getChildElementByTagName(element, elementName);
        if (childElement != null)
        {
            if (hasAttribute(childElement, "ref"))
            {
                if (childElement.getAttribute("ref").startsWith("#"))
                {
                    builder.addPropertyValue(propertyName, childElement.getAttribute("ref"));
                }
                else
                {
                    builder.addPropertyValue(propertyName,
                        (("#[registry:" + childElement.getAttribute("ref")) + "]"));
                }
                return true;
            }
        }
        return false;
    }

    protected boolean parseObjectRefWithDefault(Element element,
                                                BeanDefinitionBuilder builder,
                                                String elementName,
                                                String propertyName,
                                                String defaultValue)
    {
        Element childElement = DomUtils.getChildElementByTagName(element, elementName);
        if (childElement != null)
        {
            if (hasAttribute(childElement, "ref"))
            {
                if (childElement.getAttribute("ref").startsWith("#"))
                {
                    builder.addPropertyValue(propertyName, childElement.getAttribute("ref"));
                }
                else
                {
                    builder.addPropertyValue(propertyName,
                        (("#[registry:" + childElement.getAttribute("ref")) + "]"));
                }
                return true;
            }
        }
        else
        {
            builder.addPropertyValue(propertyName, defaultValue);
        }
        return false;
    }

    protected boolean parseNoExprObjectRef(Element element,
                                           BeanDefinitionBuilder builder,
                                           String elementName,
                                           String propertyName)
    {
        Element childElement = DomUtils.getChildElementByTagName(element, elementName);
        if (childElement != null)
        {
            if (hasAttribute(childElement, "ref"))
            {
                if (childElement.getAttribute("ref").startsWith("#"))
                {
                    builder.addPropertyValue(propertyName, childElement.getAttribute("ref"));
                }
                else
                {
                    builder.addPropertyValue(propertyName,
                        new RuntimeBeanReference(childElement.getAttribute("ref")));
                }
                return true;
            }
        }
        return false;
    }

    protected boolean parseNoExprObjectRefWithDefault(Element element,
                                                      BeanDefinitionBuilder builder,
                                                      String elementName,
                                                      String propertyName,
                                                      String defaultValue)
    {
        Element childElement = DomUtils.getChildElementByTagName(element, elementName);
        if (childElement != null)
        {
            if (hasAttribute(childElement, "ref"))
            {
                if (childElement.getAttribute("ref").startsWith("#"))
                {
                    builder.addPropertyValue(propertyName, childElement.getAttribute("ref"));
                }
                else
                {
                    builder.addPropertyValue(propertyName,
                        new RuntimeBeanReference(childElement.getAttribute("ref")));
                }
                return true;
            }
        }
        else
        {
            builder.addPropertyValue(propertyName, defaultValue);
        }
        return false;
    }

    protected void parsePropertyRef(BeanDefinitionBuilder builder,
                                    Element element,
                                    String attributeName,
                                    String propertyName)
    {
        if (hasAttribute(element, attributeName))
        {
            builder.addPropertyValue(propertyName,
                new RuntimeBeanReference(element.getAttribute(attributeName)));
        }
    }

    protected void parsePropertyRef(BeanDefinitionBuilder builder, Element element, String propertyName)
    {
        parsePropertyRef(builder, element, propertyName, propertyName);
    }

    protected void parseTextProperty(BeanDefinitionBuilder builder,
                                     Element element,
                                     String elementName,
                                     String propertyName)
    {
        Element childElement = DomUtils.getChildElementByTagName(element, elementName);
        if (childElement != null)
        {
            builder.addPropertyValue(propertyName, childElement.getTextContent());
        }
    }

    public interface ParseDelegate<T>
    {

        public T parse(Element element);

    }

}
