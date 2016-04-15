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

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This encapsulates several definition parsers, selected depending on the parent
 * element in the DOM.
 */
public class ParentContextDefinitionParser extends AbstractParallelDelegatingDefinitionParser
{

    private Map parsers = new HashMap();
    private MuleDefinitionParser otherwise = null;

    public ParentContextDefinitionParser(String context, MuleDefinitionParser parser)
    {
        and(context, parser);
    }

    public ParentContextDefinitionParser and(String context, MuleDefinitionParser parser)
    {
        StringTokenizer names = new StringTokenizer(context);
        while (names.hasMoreTokens())
        {
            parsers.put(names.nextToken(), parser);
        }
        addDelegate(parser);
        return this;
    }

    public ParentContextDefinitionParser otherwise(MuleDefinitionParser otherwise)
    {
        this.otherwise = otherwise;
        return this;
    }

    protected MuleDefinitionParser getDelegate(Element element, ParserContext parserContext)
    {
        String context = element.getParentNode().getLocalName();
        if (parsers.containsKey(context))
        {
            return (MuleDefinitionParser) parsers.get(context);
        }
        else if (null != otherwise)
        {
            return otherwise;
        }
        else
        {
            throw new IllegalStateException("No parser defined for " + element.getLocalName() + " in the context "
                                            + context);
        }
    }

    protected MuleDefinitionParser getOtherwise()
    {
        return otherwise;
    }

    @Override
    public MuleDefinitionParserConfiguration addAlias(String alias, String propertyName)
    {
        super.addAlias(alias, propertyName);
        if (otherwise != null)
        {
            otherwise.addAlias(alias, propertyName);
        }
        return this;
    }

    @Override
    public MuleDefinitionParserConfiguration addBeanFlag(String flag)
    {
        super.addBeanFlag(flag);
        if (otherwise != null)
        {
            otherwise.addBeanFlag(flag);
        }
        return this;
    }

    @Override
    public MuleDefinitionParserConfiguration addCollection(String propertyName)
    {
        super.addCollection(propertyName);
        if (otherwise != null)
        {
            otherwise.addCollection(propertyName);
        }
        return this;
    }

    @Override
    public MuleDefinitionParserConfiguration addIgnored(String propertyName)
    {
        super.addIgnored(propertyName);
        if (otherwise != null)
        {
            otherwise.addIgnored(propertyName);
        }
        return this;
    }

    @Override
    public MuleDefinitionParserConfiguration addMapping(String propertyName, Map mappings)
    {
        super.addMapping(propertyName, mappings);
        if (otherwise != null)
        {
            otherwise.addMapping(propertyName, mappings);
        }
        return this;
    }

    @Override
    public MuleDefinitionParserConfiguration addMapping(String propertyName, String mappings)
    {
        super.addMapping(propertyName, mappings);
        if (otherwise != null)
        {
            otherwise.addMapping(propertyName, mappings);
        }
        return this;
    }

    @Override
    public MuleDefinitionParserConfiguration addMapping(String propertyName, ValueMap mappings)
    {
        super.addMapping(propertyName, mappings);
        if (otherwise != null)
        {
            otherwise.addMapping(propertyName, mappings);
        }
        return this;
    }

    @Override
    public MuleDefinitionParserConfiguration addReference(String propertyName)
    {
        super.addReference(propertyName);
        if (otherwise != null)
        {
            otherwise.addReference(propertyName);
        }
        return this;
    }

    @Override
    public MuleDefinitionParserConfiguration registerPostProcessor(PostProcessor postProcessor)
    {
        super.registerPostProcessor(postProcessor);
        if (otherwise != null)
        {
            otherwise.registerPostProcessor(postProcessor);
        }
        return this;
    }

    @Override
    public MuleDefinitionParserConfiguration registerPreProcessor(PreProcessor preProcessor)
    {
        super.registerPreProcessor(preProcessor);
        if (otherwise != null)
        {
            otherwise.registerPreProcessor(preProcessor);
        }
        return this;
    }

    @Override
    public MuleDefinitionParserConfiguration removeIgnored(String propertyName)
    {
        super.removeIgnored(propertyName);
        if (otherwise != null)
        {
            otherwise.removeIgnored(propertyName);
        }
        return this;
    }

    @Override
    public MuleDefinitionParserConfiguration setIgnoredDefault(boolean ignoreAll)
    {
        super.setIgnoredDefault(ignoreAll);
        if (otherwise != null)
        {
            otherwise.setIgnoredDefault(ignoreAll);
        }
        return this;
    }

}
