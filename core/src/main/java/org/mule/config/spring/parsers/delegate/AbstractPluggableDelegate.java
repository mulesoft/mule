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

import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.PostProcessor;
import org.mule.config.spring.parsers.PreProcessor;

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

    public AbstractBeanDefinition parseDelegate(Element element, ParserContext parserContext)
    {
        return delegate.parseDelegate(element, parserContext);
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
        return delegate.addReference(propertyName);
    }

    public MuleDefinitionParser addMapping(String propertyName, Map mappings)
    {
        return delegate.addMapping(propertyName, mappings);
    }

    public MuleDefinitionParser addMapping(String propertyName, String mappings)
    {
        return delegate.addMapping(propertyName, mappings);
    }

    public MuleDefinitionParser addAlias(String alias, String propertyName)
    {
        return delegate.addAlias(alias, propertyName);
    }

    public MuleDefinitionParser addCollection(String propertyName)
    {
        return delegate.addCollection(propertyName);
    }

    public MuleDefinitionParser addIgnored(String propertyName)
    {
        return delegate.addIgnored(propertyName);
    }

    public MuleDefinitionParser removeIgnored(String propertyName)
    {
        return delegate.removeIgnored(propertyName);
    }

    public MuleDefinitionParser setIgnoredDefault(boolean ignoreAll)
    {
        return delegate.setIgnoredDefault(ignoreAll);
    }

}
