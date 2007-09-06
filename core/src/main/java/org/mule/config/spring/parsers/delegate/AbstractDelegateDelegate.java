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

import java.util.Map;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Support for extending a
 * {@link org.mule.config.spring.parsers.delegate.DelegateDefinitionParser}
 */
public abstract class AbstractDelegateDelegate implements DelegateDefinitionParser
{

    private DelegateDefinitionParser delegate;

    public AbstractDelegateDelegate(DelegateDefinitionParser delegate)
    {
        this.delegate = delegate;
    }

    protected DelegateDefinitionParser getDelegate()
    {
        return delegate;
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

    public DelegateDefinitionParser addReference(String propertyName)
    {
        return delegate.addReference(propertyName);
    }

    public DelegateDefinitionParser addMapping(String propertyName, Map mappings)
    {
        return delegate.addMapping(propertyName, mappings);
    }

    public DelegateDefinitionParser addMapping(String propertyName, String mappings)
    {
        return delegate.addMapping(propertyName, mappings);
    }

    public DelegateDefinitionParser addAlias(String alias, String propertyName)
    {
        return delegate.addAlias(alias, propertyName);
    }

    public DelegateDefinitionParser addCollection(String propertyName)
    {
        return delegate.addCollection(propertyName);
    }

    public DelegateDefinitionParser addIgnored(String propertyName)
    {
        return delegate.addIgnored(propertyName);
    }

    public DelegateDefinitionParser removeIgnored(String propertyName)
    {
        return delegate.removeIgnored(propertyName);
    }

    public DelegateDefinitionParser setIgnoredDefault(boolean ignoreAll)
    {
        return delegate.setIgnoredDefault(ignoreAll);
    }

}
