/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.delegate;

import org.mule.config.spring.parsers.collection.DynamicAttributeDefinitionParser;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.PostProcessor;
import org.mule.config.spring.parsers.assembly.PropertyConfiguration;
import org.mule.config.spring.parsers.assembly.SimplePropertyConfiguration;
import org.mule.util.CoreXMLUtils;

import java.util.Map;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
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
            String name = CoreXMLUtils.attributeName((Attr) attributes.item(i));
            if (!isIgnored(name))
            {
                delegate.setAttributeName(name);
                delegate.parseDelegate(element, parserContext);
            }
        }
        return null;
    }


    public AbstractBeanDefinition parseDelegate(Element element, ParserContext parserContext)
    {
        return parseInternal(element, parserContext);
    }

    public void registerPreProcessor(PreProcessor preProcessor)
    {
        delegate.registerPreProcessor(preProcessor);
    }

    public void registerPostProcessor(PostProcessor postProcessor)
    {
        delegate.registerPostProcessor(postProcessor);
    }

    public MuleDefinitionParser addReference(String propertyName)
    {
        configuration.addReference(propertyName);
        return this;
    }

    public MuleDefinitionParser addMapping(String propertyName, Map mappings)
    {
        configuration.addMapping(propertyName, mappings);
        return this;
    }

    public MuleDefinitionParser addMapping(String propertyName, String mappings)
    {
        configuration.addMapping(propertyName, mappings);
        return this;
    }

    public MuleDefinitionParser addAlias(String alias, String propertyName)
    {
        configuration.addAlias(alias, propertyName);
        return this;
    }

    public MuleDefinitionParser addCollection(String propertyName)
    {
        configuration.addCollection(propertyName);
        return this;
    }

    public MuleDefinitionParser addIgnored(String propertyName)
    {
        configuration.addIgnored(propertyName);
        return this;
    }

    public MuleDefinitionParser removeIgnored(String propertyName)
    {
        configuration.removeIgnored(propertyName);
        return this;
    }

    public MuleDefinitionParser setIgnoredDefault(boolean ignoreAll)
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
        return configuration.isBeanReference(attributeName);
    }

    public String translateName(String oldName)
    {
        return configuration.translateName(oldName);
    }

    public String translateValue(String name, String value)
    {
        return configuration.translateValue(name, value);
    }

}
