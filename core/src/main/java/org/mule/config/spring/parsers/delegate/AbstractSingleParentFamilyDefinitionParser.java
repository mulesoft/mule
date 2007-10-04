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

import org.mule.config.spring.parsers.MuleChildDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParser;

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
        extends AbstractSerialDelegatingDefinitionParser
{

    private AbstractBeanDefinition parent;

    protected void addChildDelegate(MuleChildDefinitionParser delegate)
    {
        super.addDelegate(delegate);
    }

    protected void addDelegate(MuleDefinitionParser delegate)
    {
        if (size() > 0)
        {
            throw new IllegalStateException("Children must implement child interface");
        }
        super.addDelegate(delegate);
    }

    protected AbstractBeanDefinition doSingleBean(int index, MuleDefinitionParser parser,
                                                  Element element, ParserContext parserContext)
    {
        if (0 != index)
        {
            ((MuleChildDefinitionParser) parser).forceParent(parent);
            // we need this because we often block "everything but" which would mean
            // being unable to set ourselves on the parent
            ((MuleChildDefinitionParser) parser).getTargetPropertyConfiguration().setIgnoredDefault(false);
        }
        try
        {
            AbstractBeanDefinition result = super.doSingleBean(index, parser, element, parserContext);
            if (0 == index)
            {
                parent = result;
            }
            if (size() == index + 1)
            {
                return parent;
            }
            else
            {
                return null;
            }
        }
        catch (RuntimeException e)
        {
            parent = null;
            throw e;
        }
    }

}
