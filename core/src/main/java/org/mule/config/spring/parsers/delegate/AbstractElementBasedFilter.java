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

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public abstract class AbstractElementBasedFilter extends AbstractPluggableDelegate
{

    public AbstractElementBasedFilter(MuleDefinitionParser delegate)
    {
        super(delegate);
    }

    public AbstractBeanDefinition parseDelegate(Element element, ParserContext parserContext)
    {
        if (accept(element))
        {
            return super.parseDelegate(element, parserContext);
        }
        else
        {
            return null;
        }
    }

    public abstract boolean accept(Element element);

}
