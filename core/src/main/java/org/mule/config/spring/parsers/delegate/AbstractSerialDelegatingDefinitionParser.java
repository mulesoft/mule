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

import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This allows a set of definition parsers to be used, one after another, to process
 * the same element.  This lets multipe beans be generated from a single element.
 *
 * <p>Typically, subclasses will add additional processing by wrapping delegate parsers
 * with {@link org.mule.config.spring.parsers.delegate.AbstractDelegateDelegate}.</p>
 */
public abstract class AbstractSerialDelegatingDefinitionParser extends AbstractDelegatingDefinitionParser
{

    private int index = 0;

    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        if (! repeat())
        {
            index = 0;
        }
        DelegateDefinitionParser parser = get(index++);
        AbstractBeanDefinition bean = parser.parseDelegate(element, parserContext);
        if (repeat())
        {
            bean.setAttribute(MuleHierarchicalBeanDefinitionParserDelegate.MULE_REPEAT_PARSE, Boolean.TRUE);
        }
        else
        {
            bean.removeAttribute(MuleHierarchicalBeanDefinitionParserDelegate.MULE_REPEAT_PARSE);
        }
        return bean;
    }

    private boolean repeat()
    {
        return index < size();
    }

}
