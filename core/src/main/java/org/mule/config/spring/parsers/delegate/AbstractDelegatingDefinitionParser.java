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

import org.mule.util.ArrayUtils;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.PostProcessor;

import java.util.Map;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This allows a definition parsers to be dynamically represented by different
 * definition parsers, depending on the context.  For example, a single model may
 * be defined across file - the first use defines the model and subsequent uses
 * extend it (for this particular case, see {@link InheritDefinitionParser}).
 *
 * <p>Note that the sub-parsers must be consistent.  That includes matching the
 * same schema, for example.</p>
 */
public abstract class AbstractDelegatingDefinitionParser extends AbstractBeanDefinitionParser
    implements MuleDefinitionParser
{

    private MuleDefinitionParser[] delegates;

    protected AbstractDelegatingDefinitionParser()
    {
        this(new MuleDefinitionParser[0]);
    }

    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        return parseDelegate(element, parserContext);
    }

    protected AbstractDelegatingDefinitionParser(MuleDefinitionParser[] delegates)
    {
        this.delegates = delegates;
    }

    protected MuleDefinitionParser addDelegate(MuleDefinitionParser delegate)
    {
        delegates = (MuleDefinitionParser[]) ArrayUtils.add(delegates, delegate);
        return delegate;
    }

    protected int size()
    {
        return delegates.length;
    }

    protected MuleDefinitionParser getDelegate(int index)
    {
        return delegates[index];
    }

    public void registerPreProcessor(PreProcessor preProcessor)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].registerPreProcessor(preProcessor);
        }
    }

    public void registerPostProcessor(PostProcessor postProcessor)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].registerPostProcessor(postProcessor);
        }
    }

    public MuleDefinitionParser addReference(String propertyName)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].addReference(propertyName);
        }
        return this;
    }

    public MuleDefinitionParser addMapping(String propertyName, Map mappings)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].addMapping(propertyName, mappings);
        }
        return this;
    }

    public MuleDefinitionParser addMapping(String propertyName, String mappings)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].addMapping(propertyName, mappings);
        }
        return this;
    }

    public MuleDefinitionParser addAlias(String alias, String propertyName)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].addAlias(alias, propertyName);
        }
        return this;
    }

    public MuleDefinitionParser addCollection(String propertyName)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].addCollection(propertyName);
        }
        return this;
    }

    public MuleDefinitionParser addIgnored(String propertyName)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].addIgnored(propertyName);
        }
        return this;
    }

    public MuleDefinitionParser removeIgnored(String propertyName)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].removeIgnored(propertyName);
        }
        return this;
    }

    public MuleDefinitionParser setIgnoredDefault(boolean ignoreAll)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].setIgnoredDefault(ignoreAll);
        }
        return this;
    }

}