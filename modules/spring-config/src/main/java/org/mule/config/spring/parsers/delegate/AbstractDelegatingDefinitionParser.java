/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.delegate;

import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParserConfiguration;
import org.mule.config.spring.parsers.PostProcessor;
import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.assembly.configuration.ValueMap;
import org.mule.config.spring.parsers.generic.AutoIdUtils;
import org.mule.util.ArrayUtils;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    protected Log logger = LogFactory.getLog(getClass());
    
    private MuleDefinitionParser[] delegates;

    protected AbstractDelegatingDefinitionParser()
    {
        this(new MuleDefinitionParser[0]);
    }

    protected AbstractDelegatingDefinitionParser(MuleDefinitionParser[] delegates)
    {
        this.delegates = delegates;
        addBeanFlag(MuleHierarchicalBeanDefinitionParserDelegate.MULE_FORCE_RECURSE);
    }

    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        return muleParse(element, parserContext);
    }

    protected MuleDefinitionParserConfiguration addDelegate(MuleDefinitionParser delegate)
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

    public MuleDefinitionParserConfiguration registerPreProcessor(PreProcessor preProcessor)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].registerPreProcessor(preProcessor);
        }
        return this;
    }

    public MuleDefinitionParserConfiguration registerPostProcessor(PostProcessor postProcessor)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].registerPostProcessor(postProcessor);
        }
        return this;
    }

    public MuleDefinitionParserConfiguration addReference(String propertyName)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].addReference(propertyName);
        }
        return this;
    }

    public MuleDefinitionParserConfiguration addMapping(String propertyName, Map mappings)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].addMapping(propertyName, mappings);
        }
        return this;
    }

    public MuleDefinitionParserConfiguration addMapping(String propertyName, String mappings)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].addMapping(propertyName, mappings);
        }
        return this;
    }

    public MuleDefinitionParserConfiguration addMapping(String propertyName, ValueMap mappings)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].addMapping(propertyName, mappings);
        }
        return this;
    }

    public MuleDefinitionParserConfiguration addAlias(String alias, String propertyName)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].addAlias(alias, propertyName);
        }
        return this;
    }

    public MuleDefinitionParserConfiguration addCollection(String propertyName)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].addCollection(propertyName);
        }
        return this;
    }

    public MuleDefinitionParserConfiguration addIgnored(String propertyName)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].addIgnored(propertyName);
        }
        return this;
    }

    public MuleDefinitionParserConfiguration removeIgnored(String propertyName)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].removeIgnored(propertyName);
        }
        return this;
    }

    public MuleDefinitionParserConfiguration setIgnoredDefault(boolean ignoreAll)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].setIgnoredDefault(ignoreAll);
        }
        return this;
    }

    public String getBeanName(Element element)
    {
        return AutoIdUtils.getUniqueName(element, "delegate");
    }

    public MuleDefinitionParserConfiguration addBeanFlag(String flag)
    {
        for (int i = 0; i < delegates.length; ++i)
        {
            delegates[i].addBeanFlag(flag);
        }
        return this;
    }
}
