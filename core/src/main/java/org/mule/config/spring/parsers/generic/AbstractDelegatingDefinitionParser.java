/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.generic;

import org.mule.util.ArrayUtils;

import java.util.Map;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This allows a definition parsers to be dynamically represented by different
 * definition parsers, depending on the context.  For example, a single model may
 * be defined across file - the first use defines the model and subsequent uses
 * extend it (for this particular case, see
 * {@link org.mule.config.spring.parsers.generic.InheritDefinitionParser}).
 *
 * <p>Note that the sub-parsers must be consistent.  That includes matching the
 * same schema, for example.
 */
public abstract class AbstractDelegatingDefinitionParser extends AbstractBeanDefinitionParser
{

    private DelegateDefinitionParser[] delegates;

    protected AbstractDelegatingDefinitionParser()
    {
        this(new DelegateDefinitionParser[0]);
    }

    protected AbstractDelegatingDefinitionParser(DelegateDefinitionParser[] delegates)
    {
        this.delegates = delegates;
    }

    protected void addDelegate(DelegateDefinitionParser delegate)
    {
        delegates = (DelegateDefinitionParser[]) ArrayUtils.add(delegates, delegate);
    }

    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        return getDelegate(element, parserContext).parseDelegate(element, parserContext);
    }

    public AbstractDelegatingDefinitionParser addReference(String propertyName)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].addReference(propertyName);
        }
        return this;
    }

    public AbstractDelegatingDefinitionParser addMapping(String propertyName, Map mappings)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].addMapping(propertyName, mappings);
        }
        return this;
    }

    public AbstractDelegatingDefinitionParser addMapping(String propertyName, String mappings)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].addMapping(propertyName, mappings);
        }
        return this;
    }

    public AbstractDelegatingDefinitionParser addAlias(String alias, String propertyName)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].addAlias(alias, propertyName);
        }
        return this;
    }

    public AbstractDelegatingDefinitionParser addCollection(String propertyName)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].addCollection(propertyName);
        }
        return this;
    }

    public AbstractDelegatingDefinitionParser addIgnored(String propertyName)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].addIgnored(propertyName);
        }
        return this;
    }

    protected abstract DelegateDefinitionParser getDelegate(Element element, ParserContext parserContext);

}
