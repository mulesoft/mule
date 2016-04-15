/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.delegate;

import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParserConfiguration;
import org.mule.config.spring.parsers.PostProcessor;
import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.assembly.configuration.ValueMap;

import java.util.Map;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Support for extending a
 * {@link org.mule.config.spring.parsers.MuleDefinitionParser} without
 * needing to subclass.
 */
public abstract class AbstractPluggableDelegate implements MuleDefinitionParser
{

    private MuleDefinitionParser delegate;

    public AbstractPluggableDelegate(MuleDefinitionParser delegate)
    {
        this.delegate = delegate;
    }

    public AbstractBeanDefinition muleParse(Element element, ParserContext parserContext)
    {
        return delegate.muleParse(element, parserContext);
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
        delegate.addReference(propertyName);
        return this;
    }

    public MuleDefinitionParserConfiguration addMapping(String propertyName, Map mappings)
    {
        delegate.addMapping(propertyName, mappings);
        return this;
    }

    public MuleDefinitionParserConfiguration addMapping(String propertyName, String mappings)
    {
        delegate.addMapping(propertyName, mappings);
        return this;
    }

    public MuleDefinitionParserConfiguration addMapping(String propertyName, ValueMap mappings)
    {
        delegate.addMapping(propertyName, mappings);
        return this;
    }

    public MuleDefinitionParserConfiguration addAlias(String alias, String propertyName)
    {
        delegate.addAlias(alias, propertyName);
        return this;
    }

    public MuleDefinitionParserConfiguration addCollection(String propertyName)
    {
        delegate.addCollection(propertyName);
        return this;
    }

    public MuleDefinitionParserConfiguration addIgnored(String propertyName)
    {
        delegate.addIgnored(propertyName);
        return this;
    }

    public MuleDefinitionParserConfiguration removeIgnored(String propertyName)
    {
        delegate.removeIgnored(propertyName);
        return this;
    }

    public MuleDefinitionParserConfiguration setIgnoredDefault(boolean ignoreAll)
    {
        delegate.setIgnoredDefault(ignoreAll);
        return this;
    }
}
