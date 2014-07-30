/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.delegate;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParserConfiguration;
import org.mule.config.spring.parsers.PostProcessor;
import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.parsers.assembly.configuration.SimplePropertyConfiguration;
import org.mule.config.spring.parsers.assembly.configuration.ValueMap;
import org.mule.config.spring.parsers.collection.DynamicAttributeDefinitionParser;
import org.mule.config.spring.parsers.generic.AutoIdUtils;
import org.mule.config.spring.util.SpringXMLUtils;

import java.util.Map;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * This is a very hurried demonstration.  It needs improving!
 */
public class AllAttributeChildDefinitionParser
        extends AbstractBeanDefinitionParser implements MuleDefinitionParser
{

    private DynamicAttributeDefinitionParser delegate;
    private PropertyConfiguration configuration = new SimplePropertyConfiguration();

    public AllAttributeChildDefinitionParser(DynamicAttributeDefinitionParser delegate)
    {
        addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_ID);
        addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
        this.delegate = delegate;
    }

    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); ++i)
        {
            String name = SpringXMLUtils.attributeName((Attr) attributes.item(i));
            if (!isIgnored(name))
            {
                delegate.setAttributeName(name);
                delegate.muleParse(element, parserContext);
            }
        }
        return null;
    }


    public AbstractBeanDefinition muleParse(Element element, ParserContext parserContext)
    {
        return parseInternal(element, parserContext);
    }

    public MuleDefinitionParserConfiguration registerPreProcessor(PreProcessor preProcessor)
    {
        delegate.registerPreProcessor(preProcessor);
        return this;
    }

    public MuleDefinitionParserConfiguration registerPostProcessor(PostProcessor postProcessor)
    {
        delegate.registerPostProcessor(postProcessor);
        return this;
    }

    public MuleDefinitionParserConfiguration addReference(String propertyName)
    {
        configuration.addReference(propertyName);
        return this;
    }

    public MuleDefinitionParserConfiguration addMapping(String propertyName, Map mappings)
    {
        configuration.addMapping(propertyName, mappings);
        return this;
    }

    public MuleDefinitionParserConfiguration addMapping(String propertyName, String mappings)
    {
        configuration.addMapping(propertyName, mappings);
        return this;
    }

    public MuleDefinitionParserConfiguration addMapping(String propertyName, ValueMap mappings)
    {
        configuration.addMapping(propertyName, mappings);
        return this;
    }

    public MuleDefinitionParserConfiguration addAlias(String alias, String propertyName)
    {
        configuration.addAlias(alias, propertyName);
        return this;
    }

    public MuleDefinitionParserConfiguration addCollection(String propertyName)
    {
        configuration.addCollection(propertyName);
        return this;
    }

    public MuleDefinitionParserConfiguration addIgnored(String propertyName)
    {
        configuration.addIgnored(propertyName);
        return this;
    }

    public MuleDefinitionParserConfiguration removeIgnored(String propertyName)
    {
        configuration.removeIgnored(propertyName);
        return this;
    }

    public MuleDefinitionParserConfiguration setIgnoredDefault(boolean ignoreAll)
    {
        configuration.setIgnoredDefault(ignoreAll);
        return this;
    }

    public String getAttributeMapping(String alias)
    {
        return configuration.getAttributeMapping(alias);
    }

    public boolean isCollection(String propertyName)
    {
        return configuration.isCollection(propertyName);
    }

    public boolean isIgnored(String propertyName)
    {
        return configuration.isIgnored(propertyName);
    }

    public boolean isBeanReference(String attributeName)
    {
        return configuration.isReference(attributeName);
    }

    public String translateName(String oldName)
    {
        return configuration.translateName(oldName);
    }

    public Object translateValue(String name, String value)
    {
        return configuration.translateValue(name, value);
    }

    public String getBeanName(Element element)
    {
        return AutoIdUtils.getUniqueName(element, "all-attribute");
    }

    public MuleDefinitionParserConfiguration addBeanFlag(String flag)
    {
        delegate.addBeanFlag(flag);
        return this;
    }
}
