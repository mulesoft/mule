/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.delegate;

import org.mule.module.springconfig.parsers.MuleChildDefinitionParser;
import org.mule.module.springconfig.parsers.MuleDefinitionParser;
import org.mule.module.springconfig.parsers.MuleDefinitionParserConfiguration;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This allows several parsers to be used on a single element, creating a parent bean with
 * the first parser and then extending that with child parsers.
 *
 * <p>Note that this make a lot of assumptions about what type of parser is used.
 */
public abstract class AbstractSingleParentFamilyDefinitionParser
        extends AbstractFirstResultSerialDefinitionParser
{

    public AbstractSingleParentFamilyDefinitionParser()
    {
        super();
    }

    public AbstractSingleParentFamilyDefinitionParser(boolean doReset)
    {
        super(doReset);
    }

    protected MuleChildDefinitionParser addChildDelegate(MuleChildDefinitionParser delegate)
    {
        return (MuleChildDefinitionParser) super.addDelegate(delegate);
    }

    protected MuleDefinitionParserConfiguration addDelegate(MuleDefinitionParser delegate)
    {
        if (size() > 0)
        {
            return addDelegateAsChild(delegate);
        }
        else
        {
            return super.addDelegate(delegate);
        }
    }

    protected MuleDefinitionParserConfiguration addDelegateAsChild(MuleDefinitionParser delegate)
    {
        if (delegate instanceof MuleChildDefinitionParser)
        {
            return addChildDelegate((MuleChildDefinitionParser) delegate);
        }
        else
        {
            throw new IllegalStateException("Children must implement child interface");
        }
    }

    protected AbstractBeanDefinition doSingleBean(int index, MuleDefinitionParser parser,
                                                  Element element, ParserContext parserContext)
    {
        if (0 != index)
        {
            ((MuleChildDefinitionParser) parser).forceParent(firstDefinition);
            // we need this because we often block "everything but" which would mean
            // being unable to set ourselves on the parent
            ((MuleChildDefinitionParser) parser).getTargetPropertyConfiguration().setIgnoredDefault(false);
        }
        return super.doSingleBean(index, parser, element, parserContext);
    }

}
